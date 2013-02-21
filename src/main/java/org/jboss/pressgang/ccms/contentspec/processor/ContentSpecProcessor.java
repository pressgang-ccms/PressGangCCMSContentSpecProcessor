package org.jboss.pressgang.ccms.contentspec.processor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.jboss.pressgang.ccms.contentspec.Comment;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.KeyValueNode;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.Node;
import org.jboss.pressgang.ccms.contentspec.SpecNode;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.constants.CSConstants;
import org.jboss.pressgang.ccms.contentspec.entities.Relationship;
import org.jboss.pressgang.ccms.contentspec.interfaces.ShutdownAbleApp;
import org.jboss.pressgang.ccms.contentspec.processor.constants.ProcessorConstants;
import org.jboss.pressgang.ccms.contentspec.processor.exceptions.ProcessingException;
import org.jboss.pressgang.ccms.contentspec.processor.structures.ProcessingOptions;
import org.jboss.pressgang.ccms.contentspec.processor.utils.ProcessorUtilities;
import org.jboss.pressgang.ccms.contentspec.provider.CSNodeProvider;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.provider.PropertyTagProvider;
import org.jboss.pressgang.ccms.contentspec.provider.TagProvider;
import org.jboss.pressgang.ccms.contentspec.provider.TopicProvider;
import org.jboss.pressgang.ccms.contentspec.provider.TopicSourceURLProvider;
import org.jboss.pressgang.ccms.contentspec.utils.TopicPool;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLogger;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.contentspec.wrapper.CSNodeWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.CSRelatedNodeWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.PropertyTagInContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.PropertyTagInTopicWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.PropertyTagWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.TagWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.TopicSourceURLWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.TopicWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.collection.CollectionWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.collection.UpdateableCollectionWrapper;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;

/**
 * A class to fully process a Content Specification. It first parses the data using a ContentSpecParser,
 * then validates the Content Specification using a ContentSpecValidator and lastly saves the data to the database.
 * It can also be configured to only validate the data and not save it.
 *
 * @author lnewson
 */
@SuppressWarnings("rawtypes")
public class ContentSpecProcessor implements ShutdownAbleApp {
    private final Logger LOG = Logger.getLogger(ContentSpecProcessor.class.getPackage().getName() + ".CustomContentSpecProcessor");

    private final ErrorLogger log;
    private final DataProviderFactory providerFactory;

    private final ProcessingOptions processingOptions;
    private ContentSpecValidator validator;
    private final TopicPool topics;
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    /**
     * Constructor
     *
     * @param factory           A DBManager object that manages the REST connection and the functions to read/write to the REST Interface.
     * @param loggerManager
     * @param processingOptions The set of options to use when processing.
     */
    public ContentSpecProcessor(final DataProviderFactory factory, final ErrorLoggerManager loggerManager,
            final ProcessingOptions processingOptions) {

        providerFactory = factory;

        log = loggerManager.getLogger(ContentSpecProcessor.class);
        topics = new TopicPool(factory);
        this.processingOptions = processingOptions;
        validator = new ContentSpecValidator(factory, loggerManager, processingOptions);
    }

    /**
     * Process a content specification so that it is parsed, validated and saved.
     *
     * @param contentSpec The Content Specification that is to be processed.
     * @param user        The user who requested the process operation.
     * @param mode        The mode to parse the content specification in.
     * @return True if everything was processed successfully otherwise false.
     */
    public boolean processContentSpec(final ContentSpec contentSpec, final UserWrapper user, final ContentSpecParser.ParsingMode mode) {
        return processContentSpec(contentSpec, user, mode, null);
    }


    /**
     * Process a content specification so that it is parsed, validated and saved.
     *
     * @param contentSpec    The Content Specification that is to be processed.
     * @param user           The user who requested the process operation.
     * @param mode           The mode to parse the content specification in.
     * @param overrideLocale Override the default locale using this parameter.
     * @return True if everything was processed successfully otherwise false.
     */
    @SuppressWarnings({"unchecked"})
    public boolean processContentSpec(final ContentSpec contentSpec, final UserWrapper user, final ContentSpecParser.ParsingMode mode,
            final String overrideLocale) {
        boolean editing = false;
        if (mode == ContentSpecParser.ParsingMode.EDITED) {
            editing = true;
        }

        // Set the user as the assigned writer
        contentSpec.setAssignedWriter(user == null ? null : user.getUsername());

        // Change the locale if the overrideLocale isn't null
        if (overrideLocale != null) {
            contentSpec.setLocale(overrideLocale);
        }

        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            shutdown.set(true);
            return false;
        }

        // Validate the content specification before doing any rest calls
        LOG.info("Starting first validation pass...");

        // Validate the relationships
        if (!validator.preValidateRelationships(contentSpec) || !validator.preValidateContentSpec(contentSpec)) {
            log.error(ProcessorConstants.ERROR_INVALID_CS_MSG);
            return false;
        }

        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            shutdown.set(true);
            return false;
        }

        // Validate the content specification now that we have most of the data from the REST API
        LOG.info("Starting second validation pass...");

        if (!validator.postValidateContentSpec(contentSpec, user)) {
            log.error(ProcessorConstants.ERROR_INVALID_CS_MSG);
            return false;
        } else {
            log.info(ProcessorConstants.INFO_VALID_CS_MSG);

            // If we aren't validating then save the content specification
            if (!processingOptions.isValidating()) {
                // Check if the app should be shutdown
                if (isShuttingDown.get()) {
                    shutdown.set(true);
                    return false;
                }

                LOG.info("Saving the Content Specification to the server...");
                if (saveContentSpec(providerFactory, contentSpec, editing, user)) {
                    log.info(ProcessorConstants.INFO_SUCCESSFUL_SAVE_MSG);
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Creates an entity to be sent through the REST interface to create or update a DB entry.
     *
     * @param providerFactory
     * @param specTopic       The Content Specification Topic to create the topic entity from.
     * @return The new topic object if any changes where made otherwise null.
     */
    protected TopicWrapper createTopicEntity(final DataProviderFactory providerFactory, final SpecTopic specTopic) {
        // Duplicates reference another new or cloned topic and should not have a different new/updated underlying topic
        if (specTopic.isTopicAClonedDuplicateTopic() || specTopic.isTopicADuplicateTopic()) return null;

        final TagProvider tagProvider = providerFactory.getProvider(TagProvider.class);
        final TopicSourceURLProvider topicSourceURLProvider = providerFactory.getProvider(TopicSourceURLProvider.class);

        if (isShuttingDown.get()) {
            return null;
        }

        boolean changed = specTopic.isTopicAClonedTopic() || specTopic.isTopicANewTopic();
        final TopicWrapper topic = getTopicForSpecTopic(providerFactory, specTopic);

        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            return null;
        }

        // Process and set the assigned writer for new and cloned topics
        if (!specTopic.isTopicAnExistingTopic()) {
            processAssignedWriter(tagProvider, specTopic, topic);
            changed = true;
        }

        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            return null;
        }

        // Process the topic and add or remove any tags
        if (processTopicTags(tagProvider, specTopic, topic)) {
            changed = true;
        }

        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            return null;
        }

        // Process and set the source urls for new and cloned topics
        if (!specTopic.isTopicAnExistingTopic()) {
            if (processTopicSourceUrls(topicSourceURLProvider, specTopic, topic)) changed = true;
        }

        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            return null;
        }

        if (changed) {
            return topic;
        } else {
            return null;
        }
    }

    /**
     * Gets or creates the underlying Topic Entity for a spec topic.
     *
     * @param providerFactory
     * @param specTopic       The spec topic to get the topic entity for.
     * @return The topic entity if one could be found, otherwise null.
     */
    protected TopicWrapper getTopicForSpecTopic(final DataProviderFactory providerFactory, final SpecTopic specTopic) {
        TopicWrapper topic = null;

        if (specTopic.isTopicANewTopic()) {
            topic = getTopicForNewSpecTopic(providerFactory, specTopic);
        } else if (specTopic.isTopicAClonedTopic()) {
            topic = ProcessorUtilities.cloneTopic(providerFactory, specTopic);
        } else if (specTopic.isTopicAnExistingTopic()) {
            topic = getTopicForExistingSpecTopic(providerFactory, specTopic);
        }

        return topic;
    }

    /**
     * C
     *
     * @param providerFactory
     * @param specTopic
     * @return
     */
    private TopicWrapper getTopicForNewSpecTopic(final DataProviderFactory providerFactory, final SpecTopic specTopic) {
        final TopicProvider topicProvider = providerFactory.getProvider(TopicProvider.class);
        final TagProvider tagProvider = providerFactory.getProvider(TagProvider.class);
        final PropertyTagProvider propertyTagProvider = providerFactory.getProvider(PropertyTagProvider.class);

        // Create the topic entity.
        final TopicWrapper topic = topicProvider.newTopic();

        // Set the basics
        topic.setTitle(specTopic.getTitle());
        topic.setDescription(specTopic.getDescription(true));
        topic.setXml("");
        topic.setXmlDoctype(CommonConstants.DOCBOOK_45);

        // Write the type
        final CollectionWrapper<TagWrapper> tags = tagProvider.getTagsByName(specTopic.getType());
        if (tags == null && tags.size() != 1) {
            log.error(String.format(ProcessorConstants.ERROR_TYPE_NONEXIST_MSG, specTopic.getLineNumber(), specTopic.getText()));
            return null;
        }

        // Add the type to the topic
        topic.setTags(tagProvider.newTagCollection());
        topic.getTags().addNewItem(tags.getItems().get(0));

        // Create the unique ID for the property
        topic.setProperties(propertyTagProvider.newPropertyTagInTopicCollection());
        final PropertyTagWrapper propertyTag = propertyTagProvider.getPropertyTag(CSConstants.CSP_PROPERTY_ID);
        final PropertyTagInTopicWrapper cspProperty = propertyTagProvider.newPropertyTagInTopic(propertyTag);
        cspProperty.setValue(specTopic.getUniqueId());
        topic.getProperties().addNewItem(cspProperty);

        // Add the added by property tag
        final String assignedWriter = specTopic.getAssignedWriter(true);
        if (assignedWriter != null) {
            final PropertyTagWrapper addedByPropertyTag = propertyTagProvider.getPropertyTag(CSConstants.ADDED_BY_PROPERTY_TAG_ID);
            final PropertyTagInTopicWrapper addedByProperty = propertyTagProvider.newPropertyTagInTopic(addedByPropertyTag);
            addedByProperty.setValue(assignedWriter);
            topic.getProperties().addNewItem(addedByProperty);
        }

        return topic;
    }

    /**
     * @param providerFactory
     * @param specTopic
     * @return
     */
    private TopicWrapper getTopicForExistingSpecTopic(final DataProviderFactory providerFactory, final SpecTopic specTopic) {
        final TopicProvider topicProvider = providerFactory.getProvider(TopicProvider.class);
        final PropertyTagProvider propertyTagProvider = providerFactory.getProvider(PropertyTagProvider.class);

        // Get the current existing topic
        final TopicWrapper topic = topicProvider.getTopic(specTopic.getDBId(), null);

        // Update the CSP Property tag
        final UpdateableCollectionWrapper<PropertyTagInTopicWrapper> properties = propertyTagProvider.newPropertyTagInTopicCollection();
        final List<PropertyTagInTopicWrapper> propertyItems = topic.getProperties().getItems();
        boolean cspPropertyFound = false;
        for (final PropertyTagInTopicWrapper property : propertyItems) {
            // Remove the CSP Property ID as we will add a new one
            if (property.getId().equals(CSConstants.CSP_PROPERTY_ID)) {
                cspPropertyFound = true;

                property.setValue(specTopic.getUniqueId());
                properties.addUpdateItem(property);
            } else {
                properties.addItem(property);
            }
        }

        if (!cspPropertyFound) {
            final PropertyTagInTopicWrapper cspProperty = propertyTagProvider.newPropertyTagInTopic();
            cspProperty.setValue(specTopic.getUniqueId());
            cspProperty.setId(CSConstants.CSP_PROPERTY_ID);
            properties.addNewItem(cspProperty);
        }

        topic.setProperties(properties);

        return topic;
    }

    /**
     * Process a Spec Topic and add or remove tags defined by the spec topic.
     *
     * @param tagProvider
     * @param specTopic   The spec topic that represents the changes to the topic.
     * @param topic       The topic entity to be updated.
     * @return True if anything in the topic entity was changed, otherwise false.
     */
    protected boolean processTopicTags(final TagProvider tagProvider, final SpecTopic specTopic, final TopicWrapper topic) {
        boolean changed = false;

        // Get the tags for the topic
        final List<String> addTagNames = specTopic.getTags(true);
        final List<TagWrapper> addTags = new ArrayList<TagWrapper>();
        for (final String addTagName : addTagNames) {
            final List<TagWrapper> tagList = tagProvider.getTagsByName(addTagName).getItems();
            if (tagList.size() == 1) {
                addTags.add(tagList.get(0));
            }
        }

        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            return changed;
        }

        // If the topic is a cloned topic then only save new tags/urls
        if (specTopic.isTopicAClonedTopic()) {
            if (processClonedTopicTags(tagProvider, specTopic, topic, addTags)) changed = true;
        } else if (specTopic.isTopicAnExistingTopic() && specTopic.getRevision() == null) {
            if (processExistingTopicTags(tagProvider, topic, addTags)) changed = true;
        } else {
            if (processNewTopicTags(tagProvider, topic, addTags)) changed = true;
        }

        return changed;
    }

    /**
     * @param tagProvider
     * @param specTopic
     * @param topic
     * @param addTags
     * @return
     */
    private boolean processClonedTopicTags(final TagProvider tagProvider, final SpecTopic specTopic, final TopicWrapper topic,
            final List<TagWrapper> addTags) {
        boolean changed = false;
        // Find tags that aren't already in the database and adds them
        final List<TagWrapper> topicTagList = topic.getTags().getItems();
        for (final TagWrapper addTag : addTags) {
            boolean found = false;
            for (final TagWrapper topicTag : topicTagList) {
                if (topicTag.getId().equals(addTag.getId())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                topic.getTags().addNewItem(addTag);
                changed = true;
            }
        }

        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            return changed;
        }

        // Remove the database tags for - tags
        final List<String> removeTagNames = specTopic.getRemoveTags(true);
        final List<TagWrapper> removeTags = new ArrayList<TagWrapper>();
        for (final String removeTagName : removeTagNames) {
            final List<TagWrapper> tagList = tagProvider.getTagsByName(removeTagName).getItems();
            if (tagList.size() == 1) {
                removeTags.add(tagList.get(0));
            }
        }

        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            return changed;
        }

        for (final TagWrapper topicTag : topicTagList) {
            boolean found = false;
            for (final TagWrapper removeTag : removeTags) {
                if (topicTag.getId().equals(removeTag.getId())) {
                    found = true;
                }
            }

            if (found) {
                // Set the tag to be removed from the database
                topic.getTags().addRemoveItem(topicTag);
                changed = true;
            }

            // Remove the old writer tag as it will get replaced
            if (topicTag.containedInCategory(CSConstants.WRITER_CATEGORY_ID)) {
                topic.getTags().addRemoveItem(topicTag);
                changed = true;
            }
        }

        return changed;
    }

    /**
     * @param tagProvider
     * @param topic
     * @param addTags
     * @return
     */
    private boolean processExistingTopicTags(final TagProvider tagProvider, final TopicWrapper topic, final List<TagWrapper> addTags) {
        boolean changed = false;
        // Finds tags that aren't already in the database and adds them
        final List<TagWrapper> topicTagList = topic.getTags() == null ? new ArrayList<TagWrapper>() : topic.getTags().getItems();
        for (final TagWrapper addTag : addTags) {
            boolean found = false;
            for (final TagWrapper topicTag : topicTagList) {
                if (topicTag.getId().equals(addTag.getId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                if (topic.getTags() == null) {
                    topic.setTags(tagProvider.newTagCollection());
                }

                topic.getTags().addNewItem(addTag);
                changed = true;
            }
        }

        return changed;
    }

    /**
     * @param tagProvider
     * @param topic
     * @param addTags
     * @return
     */
    private boolean processNewTopicTags(final TagProvider tagProvider, final TopicWrapper topic, final List<TagWrapper> addTags) {
        boolean changed = false;
        // Save the tags
        for (final TagWrapper addTag : addTags) {
            if (topic.getTags() == null) {
                topic.setTags(tagProvider.newTagCollection());
            }

            topic.getTags().addNewItem(addTag);
            changed = true;
        }

        return changed;
    }

    /**
     * Processes a Spec Topic and adds the assigned writer for the topic it represents.
     *
     * @param tagProvider
     * @param specTopic   The spec topic object that contains the assigned writer.
     * @param topic       The topic entity to be updated.
     * @return True if anything in the topic entity was changed, otherwise false.
     */
    protected void processAssignedWriter(final TagProvider tagProvider, final SpecTopic specTopic, final TopicWrapper topic) {
        // Set the assigned writer (Tag Table)
        final List<TagWrapper> assignedWriterTags = tagProvider.getTagsByName(specTopic.getAssignedWriter(true)).getItems();
        final TagWrapper writerTag = assignedWriterTags.get(0);
        // Save a new assigned writer
        topic.getTags().addNewItem(writerTag);
    }

    /**
     * Processes a Spec Topic and adds any new Source Urls to the topic it represents.
     *
     * @param topicSourceURLProvider
     * @param specTopic              The spec topic object that contains the urls to add.
     * @param topic                  The topic entity to be updated.
     * @return True if anything in the topic entity was changed, otherwise false.
     */
    protected boolean processTopicSourceUrls(final TopicSourceURLProvider topicSourceURLProvider, final SpecTopic specTopic,
            final TopicWrapper topic) {
        boolean changed = false;
        // Save the new Source Urls
        final List<String> urls = specTopic.getSourceUrls();

        final CollectionWrapper<TopicSourceURLWrapper> sourceUrls = topic.getSourceURLs() == null ? topicSourceURLProvider
                .newTopicSourceURLCollection(
                topic) : topic.getSourceURLs();

        for (final String url : urls) {
            final TopicSourceURLWrapper sourceUrl = topicSourceURLProvider.newTopicSourceURL(topic);
            sourceUrl.setUrl(url);
            sourceUrls.addNewItem(sourceUrl);
        }

        if (sourceUrls.getItems() != null && !sourceUrls.getItems().isEmpty()) {
            topic.setSourceURLs(sourceUrls);
            changed = true;
        }

        return changed;
    }

    /**
     * Syncs all duplicated topics with their real topic counterpart in the content specification.
     *
     * @param specTopics A HashMap of the all the topics in the Content Specification. The key is the Topics ID.
     */
    protected void syncDuplicatedTopics(final List<SpecTopic> specTopics) {
        for (final SpecTopic topic : specTopics) {
            // Sync the normal duplicates first
            if (topic.isTopicADuplicateTopic()) {
                final String id = topic.getId();
                final String temp = "N" + id.substring(1);
                SpecTopic cloneTopic = null;
                for (final SpecTopic specTopic : specTopics) {
                    final String key = specTopic.getId();
                    if (key.equals(temp)) {
                        cloneTopic = specTopic;
                        break;
                    }
                }
                topic.setDBId(cloneTopic.getDBId());
            }
            // Sync the duplicate cloned topics
            else if (topic.isTopicAClonedDuplicateTopic()) {
                final String id = topic.getId();
                final String idType = id.substring(1);
                SpecTopic cloneTopic = null;
                for (final SpecTopic specTopic : specTopics) {
                    final String key = specTopic.getId();
                    if (key.endsWith(idType) && !key.endsWith(id)) {
                        cloneTopic = specTopic;
                        break;
                    }
                }
                topic.setDBId(cloneTopic.getDBId());

            }
        }
    }

    /**
     * Saves the Content Specification and all of the topics in the content specification
     *
     * @param providerFactory
     * @param contentSpec     The Content Specification to be saved.
     * @param edit            Whether the content specification is being edited or created.
     * @param user            The User who requested the Content Spec be saved.
     * @return True if the topic saved successfully otherwise false.
     */
    public boolean saveContentSpec(final DataProviderFactory providerFactory, final ContentSpec contentSpec, final boolean edit,
            final UserWrapper user) {
        final ContentSpecProvider contentSpecProvider = providerFactory.getProvider(ContentSpecProvider.class);

        try {
            // Create the new topic entities
            final List<SpecTopic> specTopics = contentSpec.getSpecTopics();
            for (final SpecTopic specTopic : specTopics) {

                // Check if the app should be shutdown
                if (isShuttingDown.get()) {
                    shutdown.set(true);
                    throw new Exception("Shutdown Requested");
                }

                // Add topics to the TopicPool that need to be added or updated
                if (specTopic.isTopicAClonedTopic() || specTopic.isTopicANewTopic()) {
                    try {
                        final TopicWrapper topic = createTopicEntity(providerFactory, specTopic);
                        if (topic != null) {
                            topics.addNewTopic(topic);
                        }
                    } catch (Exception e) {
                        throw new ProcessingException("Failed to create topic: " + specTopic.getId());
                    }
                } else if (specTopic.isTopicAnExistingTopic() && !specTopic.getTags(true).isEmpty() && specTopic.getRevision() == null) {
                    try {
                        final TopicWrapper topic = createTopicEntity(providerFactory, specTopic);
                        if (topic != null) {
                            topics.addUpdatedTopic(topic);
                        }
                    } catch (Exception e) {
                        throw new ProcessingException("Failed to create topic: " + specTopic.getId());
                    }
                }
            }

            // Check if the app should be shutdown
            if (isShuttingDown.get()) {
                shutdown.set(true);
                throw new Exception("Shutdown Requested");
            }

            // From here on the main saving happens so this shouldn't be interrupted

            // Save the new topic entities
            if (!topics.savePool()) {
                log.error(ProcessorConstants.ERROR_DATABASE_ERROR_MSG);
                throw new Exception("Failed to save the pool of topics.");
            }

            // Initialise the new and cloned topics using the populated topic pool
            for (final SpecTopic specTopic : specTopics) {
                topics.initialiseFromPool(specTopic);
            }

            // Sync the Duplicated Topics (ID = X<Number>)
            syncDuplicatedTopics(specTopics);

            // Save the content spec
            saveContentSpec(contentSpec, providerFactory, !edit, user);
        } catch (ProcessingException e) {
            if (providerFactory.isRollbackSupported()) {
                providerFactory.rollback();
            } else {
                // Clean up the data that was created
                if (contentSpec.getId() != null && !edit) {
                    try {
                        contentSpecProvider.deleteContentSpec(contentSpec.getId());
                    } catch (Exception e1) {
                        log.error("Unable to clean up the Content Specification from the database.", e);
                    }
                }
                if (topics.isInitialised()) topics.rollbackPool();
            }
            log.error(String.format("%s\n%7s%s", ProcessorConstants.ERROR_PROCESSING_ERROR_MSG, "", e.getMessage()));
            return false;
        } catch (Exception e) {
            if (providerFactory.isRollbackSupported()) {
                providerFactory.rollback();
            } else {
                // Clean up the data that was created
                if (contentSpec.getId() != null && !edit) {
                    try {
                        contentSpecProvider.deleteContentSpec(contentSpec.getId());
                    } catch (Exception e1) {
                        log.error("Unable to clean up the Content Specification from the database.", e);
                    }
                }
                if (topics.isInitialised()) topics.rollbackPool();
            }
            log.debug("", e);
            return false;
        }
        return true;
    }

    protected void saveContentSpec(final ContentSpec contentSpec, final DataProviderFactory providerFactory, boolean create,
            final UserWrapper user) throws Exception {
        // Get the providers
        final ContentSpecProvider contentSpecProvider = providerFactory.getProvider(ContentSpecProvider.class);
        final PropertyTagProvider propertyTagProvider = providerFactory.getProvider(PropertyTagProvider.class);
        final CSNodeProvider nodeProvider = providerFactory.getProvider(CSNodeProvider.class);

        // Create the temporary entity to store changes in and load the real entity if it exists.
        ContentSpecWrapper contentSpecEntity = null;
        if (contentSpec.getId() != null) {
            contentSpecEntity = contentSpecProvider.getContentSpec(contentSpec.getId());
        } else if (create) {
            contentSpecEntity = contentSpecProvider.newContentSpec();

            // Add the added by property tag
            final UpdateableCollectionWrapper<PropertyTagInContentSpecWrapper> propertyTagCollection = propertyTagProvider
                    .newPropertyTagInContentSpecCollection();

            // Create the new property tag
            final PropertyTagInContentSpecWrapper propertyTag = propertyTagProvider.newPropertyTagInContentSpec();
            propertyTag.setId(CSConstants.ADDED_BY_PROPERTY_TAG_ID);
            propertyTag.setValue(user.getUsername());
            propertyTagCollection.addNewItem(propertyTag);

            // Set the updated properties for the content spec
            contentSpecEntity.setProperties(propertyTagCollection);
        } else {
            throw new ProcessingException("Unable to find the existing Content Specification");
        }

        // Apply any changes to the content spec
        if (contentSpecEntity.getLocale() == null || !contentSpecEntity.getLocale().equals(contentSpec.getLocale())) {
            contentSpecEntity.setLocale(contentSpec.getLocale());
        }

        // Save the content spec entity so that we have a valid reference to add nodes to
        if (create) {
            contentSpecEntity = contentSpecProvider.createContentSpec(contentSpecEntity);
        } else {
            contentSpecEntity = contentSpecProvider.updateContentSpec(contentSpecEntity);
        }

        // Check that the content spec was updated/created successfully.
        if (contentSpecEntity == null) {
            // TODO Error Message
            return;
        }

        // Get the list of transformable child nodes for processing
        final List<Node> nodes = getTransformableNodes(contentSpec.getNodes());
        nodes.addAll(getTransformableNodes(contentSpec.getBaseLevel().getChildNodes()));

        // Create the container to hold all the changed nodes
        final UpdateableCollectionWrapper<CSNodeWrapper> updatedCSNodes = nodeProvider.newCSNodeCollection();

        // Merge the base level and comments
        mergeChildren(nodes, contentSpecEntity.getChildren(), providerFactory, null, contentSpecEntity, updatedCSNodes);

        // Save the updated content spec nodes
        if (nodeProvider.updateCSNodes(updatedCSNodes) == null) {
            // TODO error message
        }
    }

    /**
     * Merges a Content Specs meta data with a Content Spec Entities meta data
     *
     * @param metaData       The meta data object to be merged into a entity meta data object
     * @param metaDataEntity The meta data entity to merge with.
     */
    protected CSNodeWrapper mergeMetaData(final KeyValueNode<?> metaData, final CSNodeWrapper metaDataEntity,
            final CSNodeProvider nodeProvider) {
        final CSNodeWrapper updatedMetaDataEntity = nodeProvider.newCSNode();
        updatedMetaDataEntity.setId(metaDataEntity.getId());

        if (metaDataEntity.getTitle() == null || !metaDataEntity.getTitle().equals(metaData.getKey())) {
            updatedMetaDataEntity.setTitle(metaData.getKey());
        }

        if (metaDataEntity.getAdditionalText() == null || !metaDataEntity.getAdditionalText().equals(metaData.getValue().toString())) {
            updatedMetaDataEntity.setAdditionalText(metaData.getValue().toString());
        }

        return updatedMetaDataEntity;
    }

    protected void mergeChildren(final List<Node> childrenNodes, final CollectionWrapper<CSNodeWrapper> entityChildrenNodes,
            final DataProviderFactory providerFactory, final CSNodeWrapper parentNode, final ContentSpecWrapper contentSpec,
            final UpdateableCollectionWrapper<CSNodeWrapper> updatedCSNodes) throws Exception {
        if (entityChildrenNodes == null || entityChildrenNodes.isEmpty()) return;

        final CSNodeProvider nodeProvider = providerFactory.getProvider(CSNodeProvider.class);

        final List<CSNodeWrapper> processedNodes = new ArrayList<CSNodeWrapper>();
        final List<CSNodeWrapper> newNodes = new ArrayList<CSNodeWrapper>();

        // Update or create all of the children nodes that exist in the content spec
        CSNodeWrapper prevNode = null;
        for (final Node childNode : childrenNodes) {
            if (!(childNode instanceof SpecTopic || childNode instanceof Level || childNode instanceof Comment || childNode instanceof
                    KeyValueNode))
                continue;

            CSNodeWrapper foundNodeEntity = null;
            for (final CSNodeWrapper nodeEntity : entityChildrenNodes.getItems()) {
                if (childNode instanceof SpecTopic && doesTopicMatch((SpecTopic) childNode, nodeEntity)) {
                    foundNodeEntity = nodeEntity;
                    mergeTopic((SpecTopic) childNode, nodeEntity);
                    break;
                } else if (childNode instanceof Level && doesLevelMatch((Level) childNode, nodeEntity)) {
                    foundNodeEntity = nodeEntity;
                    mergeLevel((Level) childNode, nodeEntity, providerFactory, contentSpec, updatedCSNodes);
                    break;
                } else if (childNode instanceof Comment && doesCommentMatch((Comment) childNode, nodeEntity)) {
                    foundNodeEntity = nodeEntity;
                    mergeComment((Comment) childNode, nodeEntity);
                    break;
                } else if (childNode instanceof KeyValueNode) {
                    foundNodeEntity = nodeEntity;
                    mergeMetaData((KeyValueNode<?>) childNode, nodeProvider.newCSNode(), nodeProvider);
                    break;
                }
            }

            // If the node was not found create a new one
            if (foundNodeEntity == null) {
                final CSNodeWrapper newCSNodeEntity = nodeProvider.newCSNode();
                if (childNode instanceof SpecTopic) {
                    mergeTopic((SpecTopic) childNode, newCSNodeEntity);
                    newCSNodeEntity.setNodeType(CommonConstants.CS_NODE_TOPIC);
                } else if (childNode instanceof Level) {
                    mergeLevel((Level) childNode, newCSNodeEntity, providerFactory, contentSpec, updatedCSNodes);
                    newCSNodeEntity.setNodeType(((Level) childNode).getType().getId());
                } else if (childNode instanceof Comment) {
                    mergeComment((Comment) childNode, newCSNodeEntity);
                    newCSNodeEntity.setNodeType(CommonConstants.CS_NODE_COMMENT);
                } else if (childNode instanceof KeyValueNode) {
                    mergeMetaData((KeyValueNode<?>) childNode, newCSNodeEntity, nodeProvider);
                    newCSNodeEntity.setNodeType(CommonConstants.CS_NODE_META_DATA);
                } else {
                    continue;
                }

                // Save the basics of the node to get an id
                foundNodeEntity = nodeProvider.createCSNode(newCSNodeEntity);
                newNodes.add(foundNodeEntity);
            } else {
                processedNodes.add(foundNodeEntity);
            }

            // Set up the next/previous relationships as well
            final Integer previousNodeId = prevNode == null ? null : prevNode.getId();
            if (foundNodeEntity.getPreviousNodeId() != previousNodeId) {
                foundNodeEntity.setPreviousNodeId(previousNodeId);
            }
            if (prevNode != null && prevNode.getNextNodeId() != foundNodeEntity.getId()) {
                prevNode.setNextNodeId(foundNodeEntity.getId());
            }

            // setup the parent for the entity
            if ((parentNode == null && foundNodeEntity.getParent() != null) || (parentNode != null && foundNodeEntity.getParent() ==
                    null) ||
                    (foundNodeEntity.getParent().getId() != parentNode.getId())) {
                foundNodeEntity.setParent(parentNode);
            }

            // setup the contentSpec for the entity
            if ((foundNodeEntity.getContentSpec() == null) || (foundNodeEntity.getContentSpec().getId() != contentSpec.getId())) {
                foundNodeEntity.setContentSpec(contentSpec);
            }

            // The node has been updated so add it to the list of nodes to be saved
            updatedCSNodes.addUpdateItem(foundNodeEntity);

            prevNode = foundNodeEntity;
        }

        // Loop over the entities current nodes and remove any that no longer exist
        for (final CSNodeWrapper csNode : entityChildrenNodes.getItems()) {
            // if the node wasn't processed then it no longer exists, so set it for removal
            if (!processedNodes.contains(csNode)) {
                updatedCSNodes.addRemoveItem(csNode);
            }
        }
    }

    protected void mergeLevel(final Level level, final CSNodeWrapper levelEntity, final DataProviderFactory providerFactory,
            final ContentSpecWrapper contentSpec, final UpdateableCollectionWrapper<CSNodeWrapper> updatedCSNodes) throws Exception {

        // TITLE
        if (level.getTitle() != null && level.getTitle().equals(levelEntity.getTitle())) {
            levelEntity.setTitle(level.getTitle());
        }

        // TARGET ID
        if (level.getTargetId() != null && level.getTargetId().equals(levelEntity.getTargetId())) {
            levelEntity.setTargetId(level.getTargetId());
        }

        // CONDITION
        if (level.getConditionStatement() != null && !level.getConditionStatement().equals(levelEntity.getCondition())) {
            levelEntity.setCondition(level.getConditionStatement());
        }

        // Merge the child nodes
        mergeChildren(getTransformableNodes(level.getChildNodes()), levelEntity.getChildren(), providerFactory, levelEntity, contentSpec,
                updatedCSNodes);
    }

    // TODO Relationships
    protected void mergeTopic(final SpecTopic specTopic, final CSNodeWrapper topicEntity) {
        // TITLE
        if (specTopic.getTitle() != null && specTopic.getTitle().equals(topicEntity.getTitle())) {
            topicEntity.setTitle(specTopic.getTitle());
        }

        // TARGET ID
        if (specTopic.getTargetId() != null && specTopic.getTargetId().equals(
                topicEntity.getTargetId()) && !specTopic.isTargetIdAnInternalId()) {
            topicEntity.setTargetId(specTopic.getTargetId());
        }

        // CONDITION
        if (specTopic.getConditionStatement() != null && !specTopic.getConditionStatement().equals(topicEntity.getCondition())) {
            topicEntity.setCondition(specTopic.getConditionStatement());
        }

        // TOPIC ID
        if (specTopic.getDBId() != topicEntity.getEntityId()) {
            topicEntity.setEntityId(specTopic.getDBId());
        }

        // TOPIC REVISION
        if (specTopic.getRevision() != topicEntity.getRevision()) {
            topicEntity.setEntityRevision(specTopic.getRevision());
        }
    }

    protected void mergeComment(final Comment comment, final CSNodeWrapper commentEntity) {
        if (commentEntity.getAdditionalText() == null || !commentEntity.getAdditionalText().equals(comment.getText())) {
            commentEntity.setAdditionalText(comment.getText());
        }
    }

    protected void mergeTopicRelationships(final SpecTopic specTopic, final CSNodeWrapper topicEntity,
            final DataProviderFactory providerFactory) {
        // Check if anything needs to be processed.
        if (specTopic.getRelationships() != null && !specTopic.getRelationships().isEmpty()) {

            final CollectionWrapper<CSRelatedNodeWrapper> topicRelatedNodes = topicEntity.getRelatedToNodes();
            for (final Relationship relationship : specTopic.getRelationships()) {
                for (final CSRelatedNodeWrapper relatedNode : topicRelatedNodes.getItems()) {

                }
            }
        } else if (!topicEntity.getRelatedToNodes().isEmpty()) {

        }
    }

    /**
     * Gets a list of child nodes that can be transformed.
     *
     * @param childNodes The list of nodes to filter for translatable nodes.
     * @return A list of transformable nodes.
     */
    protected List<Node> getTransformableNodes(final List<Node> childNodes) {
        final List<Node> nodes = new LinkedList<Node>();
        for (final Node childNode : childNodes) {
            if (childNode instanceof SpecNode || childNode instanceof Comment || childNode instanceof KeyValueNode || childNode
                    instanceof Level) {
                nodes.add(childNode);
            }
        }

        return nodes;
    }

    /**
     * Checks to see if a ContentSpec level matches a Content Spec Entity level.
     *
     * @param level The ContentSpec level object.
     * @param node  The Content Spec Entity level.
     * @return True if the level is determined to match otherwise false.
     */
    protected boolean doesLevelMatch(final Level level, final CSNodeWrapper node) {
        if (node.getNodeType() == CommonConstants.CS_NODE_COMMENT || node.getNodeType() == CommonConstants.CS_NODE_TOPIC) return false;

        // If the unique id is not from the parser, than use the unique id to compare
        if (level.getUniqueId() != null) {
            return level.getUniqueId().equals(node.getId());
        } else {
            // Since a content spec doesn't contain the database ids for the nodes use what is available to see if the topics match

            // If the target ids match then the level should be the same
            if (level.getTargetId() != null && level.getTargetId() == node.getTargetId()) {
                return true;
            }

            return level.getTitle().equals(node.getTitle());
        }
    }

    /**
     * Checks to see if a ContentSpec topic matches a Content Spec Entity topic.
     *
     * @param specTopic The ContentSpec topic object.
     * @param node      The Content Spec Entity topic.
     * @return True if the topic is determined to match otherwise false.
     */
    protected boolean doesTopicMatch(final SpecTopic specTopic, final CSNodeWrapper node) {
        if (node.getNodeType() != CommonConstants.CS_NODE_TOPIC) return false;

        // If the unique id is not from the parser, in which case it will start with a number than use the unique id to compare
        if (specTopic.getUniqueId() != null && specTopic.getUniqueId().matches("^\\d.*")) {
            return specTopic.getUniqueId().equals(node.getId());
        } else {
            // Since a content spec doesn't contain the database ids for the nodes use what is available to see if the topics match
            if (specTopic.getRevision() != null && specTopic.getRevision() != node.getRevision()) {
                return false;
            }

            return specTopic.getDBId() == node.getEntityId();
        }
    }

    /**
     * Checks to see if a ContentSpec comment matches a Content Spec Entity comment.
     *
     * @param comment The ContentSpec comment object.
     * @param node    The Content Spec Entity comment.
     * @return True if the comment is determined to match otherwise false.
     */
    protected boolean doesCommentMatch(final Comment comment, final CSNodeWrapper node) {
        return node.getNodeType() == CommonConstants.CS_NODE_COMMENT;
    }

    @Override
    public void shutdown() {
        isShuttingDown.set(true);
        if (validator != null) {
            validator.shutdown();
        }
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }
}
