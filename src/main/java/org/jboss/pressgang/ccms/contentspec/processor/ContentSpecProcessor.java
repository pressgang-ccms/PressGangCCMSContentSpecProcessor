package org.jboss.pressgang.ccms.contentspec.processor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.jboss.pressgang.ccms.contentspec.interfaces.ShutdownAbleApp;
import org.jboss.pressgang.ccms.contentspec.processor.constants.ProcessorConstants;
import org.jboss.pressgang.ccms.contentspec.processor.exceptions.ProcessingException;
import org.jboss.pressgang.ccms.contentspec.processor.structures.ProcessingOptions;
import org.jboss.pressgang.ccms.contentspec.provider.CSNodeProvider;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.provider.PropertyTagProvider;
import org.jboss.pressgang.ccms.contentspec.provider.TagProvider;
import org.jboss.pressgang.ccms.contentspec.provider.TopicProvider;
import org.jboss.pressgang.ccms.contentspec.provider.TopicSourceURLProvider;
import org.jboss.pressgang.ccms.contentspec.utils.EntityUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.TopicPool;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLogger;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.contentspec.wrapper.CSNodeWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.CategoryInTagWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.PropertyTagInContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.PropertyTagInTopicWrapper;
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
    private final TopicProvider topicProvider;
    private final TagProvider tagProvider;
    private final PropertyTagProvider propertyTagProvider;
    private final TopicSourceURLProvider topicSourceUrlProvider;

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
        topicProvider = factory.getProvider(TopicProvider.class);
        tagProvider = factory.getProvider(TagProvider.class);
        propertyTagProvider = factory.getProvider(PropertyTagProvider.class);
        topicSourceUrlProvider = factory.getProvider(TopicSourceURLProvider.class);

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
     * @param specTopic The Content Specification Topic to create the topic entity from.
     * @return The new topic object if any changes where made otherwise null.
     * @throws Exception Any error that occurs when trying to build the new topic Entity.
     */
    protected TopicWrapper createTopicEntity(final SpecTopic specTopic) throws Exception {
        if (isShuttingDown.get()) {
            return null;
        }

        boolean changed = false;

        try {
            final UpdateableCollectionWrapper<PropertyTagInTopicWrapper> properties = propertyTagProvider.newPropertyTagInTopicCollection();

            TopicWrapper topic = null;

            // Create a Tag collection that will hold the tags for this topic entity
            CollectionWrapper<TagWrapper> topicTags = tagProvider.newTagCollection();

            if (specTopic.isTopicANewTopic()) {
                // Create the topic entity.
                topic = topicProvider.newTopic();

                // Set the basics
                topic.setTitle(specTopic.getTitle());
                topic.setDescription(specTopic.getDescription(true));
                topic.setXml("");

                // Write the type
                final CollectionWrapper<TagWrapper> tags = tagProvider.getTagsByName(specTopic.getType());
                if (tags == null && tags.size() != 1) {
                    log.error(String.format(ProcessorConstants.ERROR_TYPE_NONEXIST_MSG, specTopic.getLineNumber(), specTopic.getText()));
                    return null;
                }

                // Add the type to the topic
                topicTags.addNewItem(tags.getItems().get(0));

                // Create the unique ID for the property
                final PropertyTagInTopicWrapper cspProperty = propertyTagProvider.newPropertyTagInTopic();
                cspProperty.setValue(specTopic.getUniqueId());
                cspProperty.setId(CSConstants.CSP_PROPERTY_ID);
                properties.addNewItem(cspProperty);

                // Since this is a new topic the data has already changed
                changed = true;
            } else if (specTopic.isTopicAClonedTopic()) {
                // Get the existing topic from the database
                int clonedId = Integer.parseInt(specTopic.getId().substring(1));
                final TopicWrapper originalTopic = topicProvider.getTopic(clonedId, null);
                topic = originalTopic.clone(true);

                // Set the ID to null so a new ID will be created
                topic.setId(null);
                // Set other items to null that should be recreated
                topic.setCreated(null);
                topic.setLastModified(null);
                // Set-up the configured parameters so that everything gets saved
                topic.setTitle(topic.getTitle());
                topic.setDescription(topic.getDescription());
                topic.setHtml(topic.getHtml());
                topic.setXml(topic.getXml());

                // Go through each collection and set the "addItem" attribute to true
                final CollectionWrapper<TopicWrapper> incomingTopics = topicProvider.newTopicCollection();
                for (final TopicWrapper incomingRelationship : topic.getIncomingRelationships().getItems()) {
                    incomingTopics.addNewItem(incomingRelationship);
                }
                topic.setIncomingRelationships(incomingTopics);

                final CollectionWrapper<TopicWrapper> outgoingTopics = topicProvider.newTopicCollection();
                for (final TopicWrapper outgoingRelationship : topic.getOutgoingRelationships().getItems()) {
                    outgoingTopics.addNewItem(outgoingRelationship);
                }
                topic.setOutgoingRelationships(outgoingTopics);

                final CollectionWrapper<TopicSourceURLWrapper> sourceUrls = topicSourceUrlProvider.newTopicSourceURLCollection(topic);
                for (final TopicSourceURLWrapper sourceUrl : topic.getSourceURLs().getItems()) {
                    sourceUrl.setTitle(sourceUrl.getTitle());
                    sourceUrl.setDescription(sourceUrl.getTitle());
                    sourceUrl.setUrl(sourceUrl.getUrl());
                    sourceUrls.addNewItem(sourceUrl);
                }
                topic.setSourceURLs(sourceUrls);

                final List<PropertyTagInTopicWrapper> propertyItems = topic.getProperties().getItems();
                boolean cspPropertyFound = false;
                for (final PropertyTagInTopicWrapper property : propertyItems) {
                    // Ignore the CSP Property ID as we will add a new one
                    if (!property.getId().equals(CSConstants.CSP_PROPERTY_ID)) {
                        properties.addNewItem(property);
                    } else {
                        cspPropertyFound = true;

                        property.setValue(specTopic.getUniqueId());
                        properties.addNewItem(property);
                    }
                }

                if (!cspPropertyFound) {
                    final PropertyTagInTopicWrapper cspProperty = propertyTagProvider.newPropertyTagInTopic();
                    cspProperty.setValue(specTopic.getUniqueId());
                    cspProperty.setId(CSConstants.CSP_PROPERTY_ID);
                    properties.addNewItem(cspProperty);
                }

                final List<TagWrapper> tags = topic.getTags().getItems();
                for (final TagWrapper tag : tags) {
                    topicTags.addNewItem(tag);
                }

                // Since this is a new topic the data has already changed
                changed = true;
            } else if (specTopic.isTopicAnExistingTopic()) {
                final TopicWrapper originalTopic = topicProvider.getTopic(specTopic.getDBId(), null);
                topic = originalTopic.clone(true);

                // Remove any existing property tags
                final List<PropertyTagInTopicWrapper> propertyItems = topic.getProperties().getItems();
                boolean cspPropertyFound = false;
                for (final PropertyTagInTopicWrapper property : propertyItems) {
                    // Remove the CSP Property ID as we will add a new one
                    if (property.getId().equals(CSConstants.CSP_PROPERTY_ID)) {
                        cspPropertyFound = true;

                        property.setValue(specTopic.getUniqueId());
                        properties.addUpdateItem(property);
                    }
                }

                if (!cspPropertyFound) {
                    final PropertyTagInTopicWrapper cspProperty = propertyTagProvider.newPropertyTagInTopic();
                    cspProperty.setValue(specTopic.getUniqueId());
                    cspProperty.setId(CSConstants.CSP_PROPERTY_ID);
                    properties.addNewItem(cspProperty);
                }
            }
            topic.setProperties(properties);

            // Check if the app should be shutdown
            if (isShuttingDown.get()) {
                shutdown.set(true);
                return null;
            }

            if (!specTopic.isTopicAnExistingTopic()) {
                // Set the assigned writer (Tag Table)
                final List<TagWrapper> assignedWriterTags = tagProvider.getTagsByName(specTopic.getAssignedWriter(true)).getItems();
                if (assignedWriterTags.size() != 1) {
                    log.error(String.format(ProcessorConstants.ERROR_WRITER_NONEXIST_MSG, specTopic.getLineNumber(), specTopic.getText()));
                    return null;
                }
                final TagWrapper writerTag = assignedWriterTags.get(0);
                // Save a new assigned writer
                topicTags.addNewItem(writerTag);
            }

            // Check if the app should be shutdown
            if (isShuttingDown.get()) {
                shutdown.set(true);
                return null;
            }

            // Get the tags for the topic
            List<String> tagNames = specTopic.getTags(true);
            final List<TagWrapper> tags = new ArrayList<TagWrapper>();
            for (final String tagName : tagNames) {
                final List<TagWrapper> tagList = tagProvider.getTagsByName(tagName).getItems();
                if (tagList.size() == 1) {
                    tags.add(tagList.get(0));
                }
            }
            final Map<CategoryInTagWrapper, List<TagWrapper>> mapping = EntityUtilities.getCategoryMappingFromTagList(tags);

            // Check if the app should be shutdown
            if (isShuttingDown.get()) {
                shutdown.set(true);
                return null;
            }

            // If the topic is a cloned topic then only save new tags/urls
            if (specTopic.isTopicAClonedTopic()) {
                // Save the new tags
                // Find tags that aren't already in the database and adds them
                final List<TagWrapper> tttList = topic.getTags().getItems();
                for (final Entry<CategoryInTagWrapper, List<TagWrapper>> catEntry : mapping.entrySet()) {
                    for (final TagWrapper tag : catEntry.getValue()) {
                        boolean found = false;
                        for (final TagWrapper ttt : tttList) {
                            if (ttt.getId().equals(tag.getId())) {
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            topicTags.addNewItem(tag);
                        }
                    }
                }

                // Check if the app should be shutdown
                if (isShuttingDown.get()) {
                    shutdown.set(true);
                    return null;
                }

                // Remove the database tags for - tags
                tagNames = specTopic.getRemoveTags(true);
                final List<TagWrapper> removeTags = new ArrayList<TagWrapper>();
                for (final String tagName : tagNames) {
                    final List<TagWrapper> tagList = tagProvider.getTagsByName(tagName).getItems();
                    if (tagList.size() == 1) {
                        tags.add(tagList.get(0));
                    }
                }

                for (final TagWrapper ttt : tttList) {
                    boolean found = false;
                    for (final TagWrapper tag : removeTags) {
                        if (ttt.getId().equals(tag.getId())) {
                            found = true;
                        }
                    }

                    if (found) {
                        // Set the tag to be removed from the database
                        topicTags.addRemoveItem(ttt);
                    }

                    // Remove the old writer tag as it will get replaced
                    if (ttt.containedInCategory(CSConstants.WRITER_CATEGORY_ID)) {
                        topicTags.addRemoveItem(ttt);
                    }
                }
            } else if (specTopic.isTopicAnExistingTopic() && specTopic.getRevision() == null) {
                // Finds tags that aren't already in the database and adds them
                final List<TagWrapper> tttList = topic.getTags().getItems();
                for (final Entry<CategoryInTagWrapper, List<TagWrapper>> cat : mapping.entrySet()) {
                    for (final TagWrapper tag : cat.getValue()) {
                        boolean found = false;
                        for (final TagWrapper ttt : tttList) {
                            if (ttt.getId().equals(tag.getId())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            topicTags.addNewItem(tag);
                        }
                    }
                }
            } else {
                // Save the tags
                for (final Entry<CategoryInTagWrapper, List<TagWrapper>> cat : mapping.entrySet()) {
                    for (final TagWrapper tag : cat.getValue()) {
                        topicTags.addNewItem(tag);
                    }
                }
            }

            // Check if the app should be shutdown
            if (isShuttingDown.get()) {
                shutdown.set(true);
                return null;
            }

            if (!specTopic.isTopicAnExistingTopic()) {
                // Save the new Source Urls
                final List<String> urls = specTopic.getSourceUrls();

                final CollectionWrapper<TopicSourceURLWrapper> sourceUrls = topic.getSourceURLs() == null ? topicSourceUrlProvider
                        .newTopicSourceURLCollection(
                        topic) : topic.getSourceURLs();

                for (final String url : urls) {
                    final TopicSourceURLWrapper sourceUrl = topicSourceUrlProvider.newTopicSourceURL(topic);
                    sourceUrl.setUrl(url);
                    sourceUrls.addNewItem(sourceUrl);
                }

                if (sourceUrls.getItems() != null && !sourceUrls.getItems().isEmpty()) {
                    topic.setSourceURLs(sourceUrls);
                    changed = true;
                }
            }

            // Check if the app should be shutdown
            if (isShuttingDown.get()) {
                shutdown.set(true);
                return null;
            }

            if (topicTags.getItems() != null && !topicTags.getItems().isEmpty()) {
                topic.setTags(topicTags);
                changed = true;
            }

            if (changed) {
                return topic;
            } else {
                return null;
            }
        } catch (Exception e) {
            log.debug("", e);
            throw e;
        }
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
                if (specTopic.getId().matches("(" + CSConstants.NEW_TOPIC_ID_REGEX + "|" + CSConstants.CLONED_TOPIC_ID_REGEX + ")")) {
                    try {
                        final TopicWrapper topic = createTopicEntity(specTopic);
                        if (topic != null) {
                            topics.addNewTopic(topic);
                        }
                    } catch (Exception e) {
                        throw new ProcessingException("Failed to create topic: " + specTopic.getId());
                    }
                } else if (specTopic.isTopicAnExistingTopic() && !specTopic.getTags(true).isEmpty() && specTopic.getRevision() == null) {
                    try {
                        final TopicWrapper topic = createTopicEntity(specTopic);
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

            // TODO save the content spec
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
        /*
         * Note: When creating the entities that are to be saved. A new entity of the same type should be created and then any changes
         * should be merged in it.
         *
         * TODO The above won't work when saving to the database. So think of another method. Possibly in the REST Provider remove all
         * entities that don't have the parameter set to configured (excluding ids).
         */

        // Get the providers
        final ContentSpecProvider contentSpecProvider = providerFactory.getProvider(ContentSpecProvider.class);
        final PropertyTagProvider propertyTagProvider = providerFactory.getProvider(PropertyTagProvider.class);
        final CSNodeProvider nodeProvider = providerFactory.getProvider(CSNodeProvider.class);

        // Create the temporary entity to store changes in and load the real entity if it exists.
        ContentSpecWrapper contentSpecEntity = null;
        final ContentSpecWrapper tempContentSpecEntity = contentSpecProvider.newContentSpec();
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
            tempContentSpecEntity.setProperties(propertyTagCollection);
        } else {
            throw new ProcessingException("Unable to find the existing Content Specification");
        }

        // Apply any changes to the content spec
        if (contentSpecEntity.getLocale() == null || !contentSpecEntity.getLocale().equals(contentSpec.getLocale())) {
            tempContentSpecEntity.setLocale(contentSpec.getLocale());
        }

        // Save the content spec entity so that we have a valid reference to add nodes to
        if (create) {
            contentSpecEntity = contentSpecProvider.createContentSpec(tempContentSpecEntity);
        } else {
            contentSpecEntity = contentSpecProvider.updateContentSpec(tempContentSpecEntity);
        }

        // Check that the content spec was updated/created successfully.
        if (contentSpecEntity == null) {
            // TODO Error Message
            return;
        }

        // Get the list of transformable child nodes for processing
        final List<Node> nodes = getTransformableNodes(contentSpec.getNodes());
        nodes.addAll(getTransformableNodes(contentSpec.getBaseLevel().getChildNodes()));

        // Create an empty content spec entity since only the id is needed.
        final ContentSpecWrapper emptyContentSpecEntity = contentSpecProvider.newContentSpec();
        emptyContentSpecEntity.setId(contentSpecEntity.getId());

        // Create the container to hold all the changed nodes
        final UpdateableCollectionWrapper<CSNodeWrapper> updatedCSNodes = nodeProvider.newCSNodeCollection();

        // Merge the base level and comments
        mergeChildren(nodes, contentSpecEntity.getChildren(), providerFactory, null, emptyContentSpecEntity, updatedCSNodes);

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

    // TODO handle previous/next and relationships
    // TODO handle parents
    protected void mergeChildren(final List<Node> childrenNodes, final CollectionWrapper<CSNodeWrapper> entityChildrenNodes,
            final DataProviderFactory providerFactory, final CSNodeWrapper dummyParent, final ContentSpecWrapper dummyContentSpec,
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
            CSNodeWrapper updatedNodeEntity = null;
            for (final CSNodeWrapper nodeEntity : entityChildrenNodes.getItems()) {
                if (childNode instanceof SpecTopic && doesTopicMatch((SpecTopic) childNode, nodeEntity)) {
                    foundNodeEntity = nodeEntity;
                    updatedNodeEntity = mergeTopic((SpecTopic) childNode, nodeEntity, nodeProvider);
                    break;
                } else if (childNode instanceof Level && doesLevelMatch((Level) childNode, nodeEntity)) {
                    foundNodeEntity = nodeEntity;
                    updatedNodeEntity = mergeLevel((Level) childNode, nodeEntity, providerFactory, dummyContentSpec, updatedCSNodes);
                    break;
                } else if (childNode instanceof Comment && doesCommentMatch((Comment) childNode, nodeEntity)) {
                    foundNodeEntity = nodeEntity;
                    updatedNodeEntity = mergeComment((Comment) childNode, nodeEntity, nodeProvider);
                    break;
                } else if (childNode instanceof KeyValueNode) {
                    foundNodeEntity = nodeEntity;
                    updatedNodeEntity = mergeMetaData((KeyValueNode<?>) childNode, nodeProvider.newCSNode(), nodeProvider);
                    break;
                }
            }

            // If the node was not found create a new one
            if (foundNodeEntity == null) {
                final CSNodeWrapper newCSNodeEntity;
                if (childNode instanceof SpecTopic) {
                    newCSNodeEntity = mergeTopic((SpecTopic) childNode, nodeProvider.newCSNode(), nodeProvider);
                    newCSNodeEntity.setNodeType(CommonConstants.CS_NODE_TOPIC);
                } else if (childNode instanceof Level) {
                    newCSNodeEntity = mergeLevel((Level) childNode, nodeProvider.newCSNode(), providerFactory, dummyContentSpec,
                            updatedCSNodes);
                    newCSNodeEntity.setNodeType(((Level) childNode).getType().getId());
                } else if (childNode instanceof Comment) {
                    newCSNodeEntity = mergeComment((Comment) childNode, nodeProvider.newCSNode(), nodeProvider);
                    newCSNodeEntity.setNodeType(CommonConstants.CS_NODE_COMMENT);
                } else if (childNode instanceof KeyValueNode) {
                    newCSNodeEntity = mergeMetaData((KeyValueNode<?>) childNode, nodeProvider.newCSNode(), nodeProvider);
                    newCSNodeEntity.setNodeType(CommonConstants.CS_NODE_META_DATA);
                } else {
                    continue;
                }

                // Save the basics of the node to get an id
                foundNodeEntity = nodeProvider.createCSNode(newCSNodeEntity);
                newNodes.add(foundNodeEntity);

                // create a new entity for further updates since all other changes should be saved already
                updatedNodeEntity = nodeProvider.newCSNode();
                updatedNodeEntity.setId(foundNodeEntity.getId());
            } else {
                processedNodes.add(foundNodeEntity);
            }

            // Set up the next/previous relationships as well
            final Integer previousNodeId = prevNode == null ? null : prevNode.getId();
            if (foundNodeEntity.getPreviousNodeId() != previousNodeId) {
                updatedNodeEntity.setPreviousNodeId(previousNodeId);
            }
            if (prevNode != null && prevNode.getNextNodeId() != foundNodeEntity.getId()) {
                prevNode.setNextNodeId(foundNodeEntity.getId());
            }

            // setup the parent for the entity
            if ((dummyParent == null && foundNodeEntity.getParent() != null) || (dummyParent != null && foundNodeEntity.getParent() ==
                    null) ||
                    (foundNodeEntity.getParent().getId() != dummyParent.getId())) {
                updatedNodeEntity.setParent(dummyParent);
            }

            // setup the contentSpec for the entity
            if ((foundNodeEntity.getContentSpec() == null) || (foundNodeEntity.getContentSpec().getId() != dummyContentSpec.getId())) {
                updatedNodeEntity.setContentSpec(dummyContentSpec);
            }

            updatedCSNodes.addUpdateItem(updatedNodeEntity);

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

    protected CSNodeWrapper mergeLevel(final Level level, final CSNodeWrapper levelEntity, final DataProviderFactory providerFactory,
            final ContentSpecWrapper contentSpec, final UpdateableCollectionWrapper<CSNodeWrapper> updatedCSNodes) throws Exception {
        final CSNodeProvider nodeProvider = providerFactory.getProvider(CSNodeProvider.class);
        final CSNodeWrapper newLevelEntity = nodeProvider.newCSNode();
        newLevelEntity.setId(levelEntity.getId());

        // TITLE
        if (level.getTitle() != null && level.getTitle().equals(levelEntity.getTitle())) {
            newLevelEntity.setTitle(level.getTitle());
        }

        // TARGET ID
        if (level.getTargetId() != null && level.getTargetId().equals(levelEntity.getTargetId())) {
            newLevelEntity.setTargetId(level.getTargetId());
        }

        // CONDITION
        if (level.getConditionStatement() != null && !level.getConditionStatement().equals(levelEntity.getCondition())) {
            newLevelEntity.setCondition(level.getConditionStatement());
        }

        // Merge the child levels
        mergeChildren(getTransformableNodes(level.getChildNodes()), levelEntity.getChildren(), providerFactory, newLevelEntity, contentSpec,
                updatedCSNodes);

        return newLevelEntity;
    }

    // TODO Relationships
    protected CSNodeWrapper mergeTopic(final SpecTopic specTopic, final CSNodeWrapper topicEntity, final CSNodeProvider nodeProvider) {
        final CSNodeWrapper updatedTopicEntity = nodeProvider.newCSNode();
        updatedTopicEntity.setId(topicEntity.getId());

        // TITLE
        if (specTopic.getTitle() != null && specTopic.getTitle().equals(topicEntity.getTitle())) {
            updatedTopicEntity.setTitle(specTopic.getTitle());
        }

        // TARGET ID
        if (specTopic.getTargetId() != null && specTopic.getTargetId().equals(topicEntity.getTargetId())) {
            updatedTopicEntity.setTargetId(specTopic.getTargetId());
        }

        // CONDITION
        if (specTopic.getConditionStatement() != null && !specTopic.getConditionStatement().equals(topicEntity.getCondition())) {
            updatedTopicEntity.setCondition(specTopic.getConditionStatement());
        }

        // TOPIC ID
        if (specTopic.getDBId() != topicEntity.getEntityId()) {
            updatedTopicEntity.setEntityId(specTopic.getDBId());
        }

        // TOPIC REVISION
        if (specTopic.getRevision() != topicEntity.getRevision()) {
            updatedTopicEntity.setEntityRevision(specTopic.getRevision());
        }

        return updatedTopicEntity;
    }

    protected CSNodeWrapper mergeComment(final Comment comment, final CSNodeWrapper commentEntity, final CSNodeProvider nodeProvider) {
        final CSNodeWrapper updatedTopicEntity = nodeProvider.newCSNode();
        updatedTopicEntity.setId(commentEntity.getId());

        updatedTopicEntity.setAdditionalText(comment.getText());

        return updatedTopicEntity;
    }

    protected void mergeTopicRelationships(final SpecTopic specTopic, final CSNodeWrapper topicEntity,
            final DataProviderFactory providerFactory) {

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
