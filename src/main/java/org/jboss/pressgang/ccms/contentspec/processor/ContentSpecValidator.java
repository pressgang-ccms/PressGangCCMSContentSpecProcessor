package org.jboss.pressgang.ccms.contentspec.processor;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;

import com.j2bugzilla.base.ConnectionException;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.File;
import org.jboss.pressgang.ccms.contentspec.FileList;
import org.jboss.pressgang.ccms.contentspec.IOptionsNode;
import org.jboss.pressgang.ccms.contentspec.ITopicNode;
import org.jboss.pressgang.ccms.contentspec.InfoTopic;
import org.jboss.pressgang.ccms.contentspec.InitialContent;
import org.jboss.pressgang.ccms.contentspec.KeyValueNode;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.Node;
import org.jboss.pressgang.ccms.contentspec.SpecNode;
import org.jboss.pressgang.ccms.contentspec.SpecNodeWithRelationships;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.buglinks.BaseBugLinkStrategy;
import org.jboss.pressgang.ccms.contentspec.buglinks.BugLinkOptions;
import org.jboss.pressgang.ccms.contentspec.buglinks.BugLinkStrategyFactory;
import org.jboss.pressgang.ccms.contentspec.constants.CSConstants;
import org.jboss.pressgang.ccms.contentspec.entities.Relationship;
import org.jboss.pressgang.ccms.contentspec.entities.TargetRelationship;
import org.jboss.pressgang.ccms.contentspec.entities.TopicRelationship;
import org.jboss.pressgang.ccms.contentspec.enums.BookType;
import org.jboss.pressgang.ccms.contentspec.enums.BugLinkType;
import org.jboss.pressgang.ccms.contentspec.enums.LevelType;
import org.jboss.pressgang.ccms.contentspec.enums.RelationshipType;
import org.jboss.pressgang.ccms.contentspec.enums.TopicType;
import org.jboss.pressgang.ccms.contentspec.exceptions.ValidationException;
import org.jboss.pressgang.ccms.contentspec.interfaces.ShutdownAbleApp;
import org.jboss.pressgang.ccms.contentspec.processor.constants.ProcessorConstants;
import org.jboss.pressgang.ccms.contentspec.processor.structures.ProcessingOptions;
import org.jboss.pressgang.ccms.contentspec.sort.NullNumberSort;
import org.jboss.pressgang.ccms.contentspec.sort.TopicNodeLineNumberComparator;
import org.jboss.pressgang.ccms.contentspec.utils.ContentSpecUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.EntityUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLogger;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.provider.BlobConstantProvider;
import org.jboss.pressgang.ccms.provider.CategoryProvider;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.provider.FileProvider;
import org.jboss.pressgang.ccms.provider.ServerSettingsProvider;
import org.jboss.pressgang.ccms.provider.TagProvider;
import org.jboss.pressgang.ccms.provider.TextContentSpecProvider;
import org.jboss.pressgang.ccms.provider.TopicProvider;
import org.jboss.pressgang.ccms.provider.exception.NotFoundException;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.ExceptionUtilities;
import org.jboss.pressgang.ccms.utils.common.HashUtilities;
import org.jboss.pressgang.ccms.utils.common.XMLUtilities;
import org.jboss.pressgang.ccms.utils.common.XMLValidator;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;
import org.jboss.pressgang.ccms.wrapper.BlobConstantWrapper;
import org.jboss.pressgang.ccms.wrapper.CategoryWrapper;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.FileWrapper;
import org.jboss.pressgang.ccms.wrapper.ServerEntitiesWrapper;
import org.jboss.pressgang.ccms.wrapper.ServerSettingsWrapper;
import org.jboss.pressgang.ccms.wrapper.TagWrapper;
import org.jboss.pressgang.ccms.wrapper.TextContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.TopicWrapper;
import org.jboss.pressgang.ccms.wrapper.base.BaseTopicWrapper;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

/**
 * A class that is used to validate a Content Specification and the objects within a Content Specification. It
 * provides methods
 * for validating, ContentSpecs, Levels, Topics and Relationships. The Validator contains "Pre" and "Post" validation
 * methods
 * that will provide validation before doing any rest calls (pre) and after doing rest calls (post).
 *
 * @author lnewson
 */
public class ContentSpecValidator implements ShutdownAbleApp {

    private final DataProviderFactory factory;
    private final ServerSettingsWrapper serverSettings;
    private final ServerEntitiesWrapper serverEntities;
    private final TopicProvider topicProvider;
    private final ContentSpecProvider contentSpecProvider;
    private final TextContentSpecProvider textContentSpecProvider;
    private final TagProvider tagProvider;
    private final CategoryProvider categoryProvider;
    private final FileProvider fileProvider;
    private final BlobConstantProvider blobConstantProvider;
    private final ErrorLogger log;
    private final ProcessingOptions processingOptions;
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final String defaultLocale;

    @Override
    public void shutdown() {
        isShuttingDown.set(true);
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    /**
     * Constructor.
     *
     * @param factory           TODO
     * @param loggerManager     TODO
     * @param processingOptions The set of processing options to be used when validating.
     */
    public ContentSpecValidator(final DataProviderFactory factory, final ErrorLoggerManager loggerManager,
            final ProcessingOptions processingOptions) {
        this.factory = factory;
        topicProvider = factory.getProvider(TopicProvider.class);
        tagProvider = factory.getProvider(TagProvider.class);
        categoryProvider = factory.getProvider(CategoryProvider.class);
        contentSpecProvider = factory.getProvider(ContentSpecProvider.class);
        textContentSpecProvider = factory.getProvider(TextContentSpecProvider.class);
        fileProvider = factory.getProvider(FileProvider.class);
        blobConstantProvider = factory.getProvider(BlobConstantProvider.class);
        log = loggerManager.getLogger(ContentSpecValidator.class);
        this.processingOptions = processingOptions;

        serverSettings = factory.getProvider(ServerSettingsProvider.class).getServerSettings();
        serverEntities = serverSettings.getEntities();
        defaultLocale = serverSettings.getDefaultLocale();
    }

    /**
     * Validates that a Content Specification is valid by checking the META data,
     * child levels and topics. This method is a
     * wrapper to first call PreValidate and then PostValidate.
     *
     * @param contentSpec The content specification to be validated.
     * @param username    The user who requested the content spec validation.
     * @return True if the content specification is valid, otherwise false.
     */
    public boolean validateContentSpec(final ContentSpec contentSpec, final String username) {
        boolean valid = preValidateContentSpec(contentSpec);

        if (!postValidateContentSpec(contentSpec, username)) {
            valid = false;
        }

        return valid;
    }

    /**
     * Validates that a Content Specification is valid by checking the META data, child levels and topics.
     *
     * @param contentSpec The content specification to be validated.
     * @return True if the content specification is valid, otherwise false.
     */
    public boolean preValidateContentSpec(final ContentSpec contentSpec) {
        // Create the map of unique ids to spec topics
        final Map<String, SpecTopic> specTopicMap = ContentSpecUtilities.getUniqueIdSpecTopicMap(contentSpec);
        final Map<String, InfoTopic> infoTopicMap = ContentSpecUtilities.getUniqueIdInfoTopicMap(contentSpec);

        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            shutdown.set(true);
            return false;
        }

        boolean valid = true;
        if (isNullOrEmpty(contentSpec.getTitle())) {
            log.error(ProcessorConstants.ERROR_CS_NO_TITLE_MSG);
            valid = false;
        }

        if (isNullOrEmpty(contentSpec.getProduct())) {
            log.error(ProcessorConstants.ERROR_CS_NO_PRODUCT_MSG);
            valid = false;
        }

        if (!isNullOrEmpty(contentSpec.getVersion()) && !contentSpec.getVersion().matches(
                ProcessorConstants.PRODUCT_VERSION_VALIDATE_REGEX)) {
            log.error(format(ProcessorConstants.ERROR_INVALID_VERSION_NUMBER_MSG, CommonConstants.CS_VERSION_TITLE));
            valid = false;
        }

        if (isNullOrEmpty(contentSpec.getFormat())) {
            log.error(ProcessorConstants.ERROR_CS_NO_DTD_MSG);
            valid = false;
            // Check that the DTD specified is a valid DTD format
        } else if (!contentSpec.getFormat().equalsIgnoreCase(CommonConstants.DOCBOOK_45_TITLE)
                && !contentSpec.getFormat().equalsIgnoreCase(CommonConstants.DOCBOOK_50_TITLE)) {
            log.error(ProcessorConstants.ERROR_CS_INVALID_DTD_MSG);
            valid = false;
        }

        if (isNullOrEmpty(contentSpec.getCopyrightHolder())) {
            log.error(ProcessorConstants.ERROR_CS_NO_COPYRIGHT_MSG);
            valid = false;
        }

        // Check that the book type is valid
        if (contentSpec.getBookType() == BookType.INVALID) {
            log.error(ProcessorConstants.ERROR_INVALID_BOOK_TYPE_MSG);
            valid = false;
        }

        // Check that the Copyright year is valid
        if (contentSpec.getCopyrightYear() != null && !contentSpec.getCopyrightYear().matches(
                ProcessorConstants.COPYRIGHT_YEAR_VALIDATE_REGEX)) {
            log.error(ProcessorConstants.ERROR_INVALID_CS_COPYRIGHT_YEAR_MSG);
            valid = false;
        }

        // Check the version variables are all valid
        if (contentSpec.getBookVersion() != null && !contentSpec.getBookVersion().matches(ProcessorConstants.VERSION_VALIDATE_REGEX)) {
            log.error(format(ProcessorConstants.ERROR_INVALID_VERSION_NUMBER_MSG, CommonConstants.CS_BOOK_VERSION_TITLE));
            valid = false;
        }

        // Check the POM version is valid if it's set
        if (!isNullOrEmpty(contentSpec.getPOMVersion()) && !contentSpec.getPOMVersion().matches(
                ProcessorConstants.PRODUCT_VERSION_VALIDATE_REGEX)) {
            log.error(format(ProcessorConstants.ERROR_INVALID_VERSION_NUMBER_MSG, CommonConstants.CS_MAVEN_POM_VERSION_TITLE));
            valid = false;
        }

        if (contentSpec.getEdition() != null && !contentSpec.getEdition().matches(ProcessorConstants.PRODUCT_VERSION_VALIDATE_REGEX)) {
            log.error(format(ProcessorConstants.ERROR_INVALID_VERSION_NUMBER_MSG, CommonConstants.CS_EDITION_TITLE));
            valid = false;
        }

        // Check for a negative pubsnumber
        if (contentSpec.getPubsNumber() != null && contentSpec.getPubsNumber() < 0) {
            log.error(ProcessorConstants.ERROR_INVALID_PUBSNUMBER_MSG);
            valid = false;
        }

        // Check that the default publican.cfg exists
        if (!contentSpec.getDefaultPublicanCfg().equals(CommonConstants.CS_PUBLICAN_CFG_TITLE)) {
            final String name = contentSpec.getDefaultPublicanCfg();
            final Matcher matcher = CSConstants.CUSTOM_PUBLICAN_CFG_PATTERN.matcher(name);
            final String fixedName = matcher.find() ? matcher.group(1) : name;
            if (contentSpec.getAdditionalPublicanCfg(fixedName) == null) {
                log.error(String.format(ProcessorConstants.ERROR_INVALID_DEFAULT_PUBLICAN_CFG_MSG, name));
                valid = false;
            }
        }

        // Check that any metadata topics are valid
        if (contentSpec.getRevisionHistory() != null && !preValidateTopic(contentSpec.getRevisionHistory(), specTopicMap,
                contentSpec.getBookType(), false, contentSpec)) {
            valid = false;
        }
        if (contentSpec.getFeedback() != null && !preValidateTopic(contentSpec.getFeedback(), specTopicMap, contentSpec.getBookType(),
                false, contentSpec)) {
            valid = false;
        }
        if (contentSpec.getLegalNotice() != null && !preValidateTopic(contentSpec.getLegalNotice(), specTopicMap, contentSpec.getBookType(),
                false, contentSpec)) {
            valid = false;
        }
        if (contentSpec.getAuthorGroup() != null && !preValidateTopic(contentSpec.getAuthorGroup(), specTopicMap, contentSpec.getBookType(),
                false, contentSpec)) {
            valid = false;
        }
        if (contentSpec.getAbstractTopic() != null && !preValidateTopic(contentSpec.getAbstractTopic(), specTopicMap,
                contentSpec.getBookType(), false, contentSpec)) {
            valid = false;
        }

        // Print Warnings for content that maybe important
        if (isNullOrEmpty(contentSpec.getSubtitle())) {
            log.warn(ProcessorConstants.WARN_CS_NO_SUBTITLE_MSG);
        }
        if (isNullOrEmpty(contentSpec.getAbstract()) && contentSpec.getAbstractTopic() == null) {
            log.warn(ProcessorConstants.WARN_CS_NO_ABSTRACT_MSG);
        } else if (!isNullOrEmpty(contentSpec.getAbstract())) {
            // Check to make sure the abstract is at least valid XML
            String wrappedAbstract = contentSpec.getAbstract();
            if (!contentSpec.getAbstract().matches("^<(formal|sim)?para>(.|\\s)*")) {
                wrappedAbstract = "<para>" + contentSpec.getAbstract() + "</para>";
            }
            wrappedAbstract = "<abstract>" + wrappedAbstract + "</abstract>";

            Document doc = null;

            String errorMsg = null;
            try {
                doc = XMLUtilities.convertStringToDocument(wrappedAbstract);
            } catch (Exception e) {
                errorMsg = e.getMessage();
            }

            // If the doc variable is null then an error occurred somewhere
            if (doc == null) {
                valid = false;
                final String line = CommonConstants.CS_ABSTRACT_TITLE + " = " + contentSpec.getAbstract();
                if (errorMsg != null) {
                    log.error(String.format(ProcessorConstants.ERROR_INVALID_ABSTRACT_MSG, errorMsg, line));
                } else {
                    log.error(String.format(ProcessorConstants.ERROR_INVALID_ABSTRACT_NO_ERROR_MSG, line));
                }
            }
        }

        // Check to make sure all key value nodes a valid (that is they have a key and value specified
        for (final Node node : contentSpec.getNodes()) {
            if (node instanceof KeyValueNode) {
                final KeyValueNode keyValueNode = (KeyValueNode) node;
                if (isNullOrEmpty(keyValueNode.getKey())) {
                    valid = false;
                    log.error(format(ProcessorConstants.ERROR_INVALID_METADATA_FORMAT_MSG, keyValueNode.getLineNumber(),
                            keyValueNode.getText()));
                } else {
                    final Object value = keyValueNode.getValue();
                    if (value instanceof String && isNullOrEmpty((String) value)) {
                        valid = false;
                        log.error(format(ProcessorConstants.ERROR_INVALID_METADATA_NO_VALUE_MSG, keyValueNode.getLineNumber(),
                                keyValueNode.getText()));
                    } else if (value == null) {
                        valid = false;
                        log.error(format(ProcessorConstants.ERROR_INVALID_METADATA_NO_VALUE_MSG, keyValueNode.getLineNumber(),
                                keyValueNode.getText()));
                    }

                    // Make sure the key is a valid meta data element
                    if (!ProcessorConstants.VALID_METADATA_KEYS.contains(
                            keyValueNode.getKey()) && !CSConstants.CUSTOM_PUBLICAN_CFG_PATTERN.matcher(keyValueNode.getKey()).matches()) {
                        valid = false;
                        log.error(format(ProcessorConstants.ERROR_UNRECOGNISED_METADATA_MSG, keyValueNode.getLineNumber(),
                                keyValueNode.getText()));
                    }
                }
            }
        }

        // Validate the custom entities
        if (!validateEntities(contentSpec)) {
            valid = false;
        }

        // Validate the basic bug link data
        if (!preValidateBugLinks(contentSpec)) {
            valid = false;
        }

        // Check that each level is valid
        if (!preValidateLevel(contentSpec.getBaseLevel(), specTopicMap, infoTopicMap, contentSpec.getBookType(),
                contentSpec)) {
            valid = false;
        }

        // Check that the relationships are valid
        if (!preValidateRelationships(contentSpec)) {
            valid = false;
        }

        /*
         * Ensure that no topics exist that have the same ID but different revisions. This needs to be done at the Content Spec
         * level rather than the Topic level as it isn't the topic that would be invalid but rather the set of topics
          * in the content specification.
         */
        if (!checkTopicsForInvalidDuplicates(contentSpec)) {
            valid = false;
        }

        return valid;
    }

    /**
     * Check if the condition on a node will conflict with a condition in the defined publican.cfg file.
     *
     * @param node        The node to be checked.
     * @param contentSpec The content spec the node belongs to.
     */
    protected void checkForConflictingCondition(final IOptionsNode node, final ContentSpec contentSpec) {
        if (!isNullOrEmpty(node.getConditionStatement())) {
            final String publicanCfg;
            if (!contentSpec.getDefaultPublicanCfg().equals(CommonConstants.CS_PUBLICAN_CFG_TITLE)) {
                final String name = contentSpec.getDefaultPublicanCfg();
                final Matcher matcher = CSConstants.CUSTOM_PUBLICAN_CFG_PATTERN.matcher(name);
                final String fixedName = matcher.find() ? matcher.group(1) : name;
                publicanCfg = contentSpec.getAdditionalPublicanCfg(fixedName);
            } else {
                publicanCfg = contentSpec.getPublicanCfg();
            }

            // Make sure a publican.cfg is defined before doing any checks
            if (!isNullOrEmpty(publicanCfg)) {
                if (publicanCfg.contains("condition:")) {
                    log.warn(String.format(ProcessorConstants.WARN_CONDITION_IGNORED_MSG, node.getLineNumber(), node.getText()));
                }
            }
        }
    }

    /**
     * Validates the custom entities to ensure that only the defaults are overridden and that the content is valid XML.
     *
     * @param contentSpec The content spec that contains the custom entities to be validated.
     * @return True if the custom entities are valid and don't contain additional custom values, otherwise false.
     */
    protected boolean validateEntities(final ContentSpec contentSpec) {
        final String entities = contentSpec.getEntities();
        if (isNullOrEmpty(entities)) return true;

        boolean valid = true;

        // Make sure the input is valid XML.
        final String wrappedEntities = "<!DOCTYPE section [" + entities + "]><section></section>";
        Document doc = null;
        try {
            doc = XMLUtilities.convertStringToDocument(wrappedEntities);
        } catch (Exception e) {
            final String line = CommonConstants.CS_ENTITIES_TITLE + " = [" + entities + "]";
            log.error(String.format(ProcessorConstants.ERROR_INVALID_ENTITIES_MSG, e.getMessage(), line));
            valid = false;
        }

        // Check that no reserved entities are defined.
        if (doc != null) {
            final List<String> invalidEntities = new ArrayList<String>();
            final NamedNodeMap entityNodes = doc.getDoctype().getEntities();
            for (int i = 0; i < entityNodes.getLength(); i++) {
                final org.w3c.dom.Node entityNode = entityNodes.item(i);
                if (ProcessorConstants.RESERVED_ENTITIES.contains(entityNode.getNodeName())) {
                    invalidEntities.add(entityNode.getNodeName());
                }
            }

            if (!invalidEntities.isEmpty()) {
                final StringBuilder builder = new StringBuilder();
                for (int i = 0; i < invalidEntities.size(); i++) {
                    if (i != 0) {
                        if (i == invalidEntities.size() - 1) {
                            builder.append(" and ");
                        } else {
                            builder.append(", ");
                        }
                    }
                    builder.append(invalidEntities.get(i));
                }

                final String line = CommonConstants.CS_ENTITIES_TITLE + " = [" + entities + "]";
                if (invalidEntities.size() == 1) {
                    log.error(String.format(ProcessorConstants.ERROR_RESERVED_ENTITIES_SINGLE_DEFINED_MSG, builder.toString(), line));
                } else {
                    log.error(String.format(ProcessorConstants.ERROR_RESERVED_ENTITIES_DEFINED_MSG, builder.toString(), line));
                }
                valid = false;
            }
        }

        return valid;
    }

    /**
     * Checks a Content Specification to see if it contains existing topics that have the same ID but different
     * revisions.
     *
     * @param contentSpec The content specification to be validated.
     * @return True if no duplicates were found, otherwise false.
     */
    protected boolean checkTopicsForInvalidDuplicates(final ContentSpec contentSpec) {
        boolean valid = true;

        // Find all Topics that have two or more different revisions
        final List<ITopicNode> allTopicNodes = contentSpec.getAllTopicNodes();
        final Map<Integer, Map<Integer, Set<ITopicNode>>> invalidTopicNodes = new HashMap<Integer, Map<Integer, Set<ITopicNode>>>();

        for (final ITopicNode topicNode1 : allTopicNodes) {
            if (!topicNode1.isTopicAnExistingTopic()) continue;

            for (final ITopicNode topicNode2 : allTopicNodes) {
                // If the Topic isn't an existing topic and doesn't match the first spec topic's id, then continue
                if (topicNode1 == topicNode2 || !topicNode2.isTopicAnExistingTopic() || !topicNode1.getDBId().equals(topicNode2.getDBId()))
                    continue;

                // Check if the revisions between the two topics are the same
                if (topicNode1.getRevision() == null && topicNode2.getRevision() != null || topicNode1.getRevision() != null &&
                        topicNode2.getRevision() == null || topicNode1.getRevision() != null && !topicNode1.getRevision().equals(
                        topicNode2.getRevision())) {
                    if (!invalidTopicNodes.containsKey(topicNode1.getDBId())) {
                        invalidTopicNodes.put(topicNode1.getDBId(), new HashMap<Integer, Set<ITopicNode>>());
                    }

                    final Map<Integer, Set<ITopicNode>> revisionsToTopicNode = invalidTopicNodes.get(topicNode1.getDBId());
                    if (!revisionsToTopicNode.containsKey(topicNode1.getRevision())) {
                        revisionsToTopicNode.put(topicNode1.getRevision(), new HashSet<ITopicNode>());
                    }

                    revisionsToTopicNode.get(topicNode1.getRevision()).add(topicNode1);

                    valid = false;
                }
            }
        }

        // Loop through and generate an error message for each invalid topic
        for (final Entry<Integer, Map<Integer, Set<ITopicNode>>> entry : invalidTopicNodes.entrySet()) {
            final Integer topicId = entry.getKey();
            final Map<Integer, Set<ITopicNode>> revisionsToTopicNode = entry.getValue();

            final List<String> revNumbers = new ArrayList<String>();
            final List<Integer> revisions = new ArrayList<Integer>(revisionsToTopicNode.keySet());
            Collections.sort(revisions, new NullNumberSort<Integer>());

            for (final Integer revision : revisions) {
                final List<ITopicNode> topicNodes = new ArrayList<ITopicNode>(revisionsToTopicNode.get(revision));

                // Build up the line numbers message
                final StringBuilder lineNumbers = new StringBuilder();
                if (topicNodes.size() > 1) {
                    // Sort the Topics by line numbers
                    Collections.sort(topicNodes, new TopicNodeLineNumberComparator());

                    for (int i = 0; i < topicNodes.size(); i++) {
                        if (i == topicNodes.size() - 1) {
                            lineNumbers.append(" and ");
                        } else if (lineNumbers.length() != 0) {
                            lineNumbers.append(", ");
                        }

                        lineNumbers.append(topicNodes.get(i).getLineNumber());
                    }
                } else if (topicNodes.size() == 1) {
                    lineNumbers.append(topicNodes.get(0).getLineNumber());
                }

                // Build the revision message
                revNumbers.add(
                        String.format(ProcessorConstants.ERROR_TOPIC_WITH_DIFFERENT_REVS_REV_MSG, (revision == null ? "Latest" : revision),
                                lineNumbers));
            }

            final StringBuilder message = new StringBuilder(String.format(ProcessorConstants.ERROR_TOPIC_WITH_DIFFERENT_REVS_MSG, topicId));
            for (final String revNumber : revNumbers) {
                message.append(String.format(ProcessorConstants.CSLINE_MSG, revNumber));
            }

            log.error(message.toString());
        }

        return valid;
    }

    /**
     * Validates that a Content Specification is valid by checking the META data, child levels and topics.
     *
     * @param contentSpec The content specification to be validated.
     * @param username    The user who requested the content spec validation.
     * @return True if the content specification is valid, otherwise false.
     */
    @SuppressWarnings("deprecation")
    public boolean postValidateContentSpec(final ContentSpec contentSpec, final String username) {

        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            shutdown.set(true);
            return false;
        }

        boolean valid = true;

        // If editing then check that the ID exists & the CHECKSUM/SpecRevision match
        if (contentSpec.getId() != null) {
            ContentSpecWrapper contentSpecEntity = null;
            String serverContentSpec = null;
            try {
                contentSpecEntity = contentSpecProvider.getContentSpec(contentSpec.getId(), contentSpec.getRevision());
                final TextContentSpecWrapper textContentSpecEntity = textContentSpecProvider.getTextContentSpec(contentSpec.getId(),
                        contentSpec.getRevision());
                if (textContentSpecEntity != null) {
                    serverContentSpec = textContentSpecEntity.getText();
                }
            } catch (NotFoundException e) {

            }
            if (contentSpecEntity == null || serverContentSpec == null) {
                log.error(String.format(ProcessorConstants.ERROR_INVALID_CS_ID_MSG, "ID=" + contentSpec.getId()));
                valid = false;
            } else {
                // Check that the checksum is valid
                if (!processingOptions.isIgnoreChecksum()) {
                    final String currentChecksum = HashUtilities.generateMD5(ContentSpecUtilities.removeChecksum(serverContentSpec));
                    if (contentSpec.getChecksum() != null) {
                        if (!contentSpec.getChecksum().equals(currentChecksum)) {
                            log.error(String.format(ProcessorConstants.ERROR_CS_NONMATCH_CHECKSUM_MSG, contentSpec.getChecksum(),
                                    currentChecksum));
                            valid = false;
                        }
                    } else {
                        log.error(String.format(ProcessorConstants.ERROR_CS_NONMATCH_CHECKSUM_MSG, null, currentChecksum));
                        valid = false;
                    }
                }

                // Check that the Content Spec isn't read only
                if (contentSpecEntity.getProperty(serverEntities.getReadOnlyPropertyTagId()) != null) {
                    if (!contentSpecEntity.getProperty(serverEntities.getReadOnlyPropertyTagId()).getValue().matches(
                            "(^|.*,)" + username + "(,.*|$)")) {
                        log.error(ProcessorConstants.ERROR_CS_READ_ONLY_MSG);
                        valid = false;
                    }
                }
            }
        }

        // Check that the injection options are valid
        if (contentSpec.getInjectionOptions() != null) {
            for (final String injectionType : contentSpec.getInjectionOptions().getStrictTopicTypes()) {
                TagWrapper tag = null;
                try {
                    tag = tagProvider.getTagByName(injectionType);
                } catch (NotFoundException e) {

                }
                if (tag != null) {
                    if (!tag.containedInCategory(serverEntities.getTypeCategoryId())) {
                        log.error(String.format(ProcessorConstants.ERROR_INVALID_INJECTION_TYPE_MSG, injectionType));
                        valid = false;
                    }
                } else {
                    log.error(String.format(ProcessorConstants.ERROR_INVALID_INJECTION_TYPE_MSG, injectionType));
                    valid = false;
                }
            }
        }

        // Check that any metadata topics are valid
        if (contentSpec.getRevisionHistory() != null && !postValidateTopic(contentSpec.getRevisionHistory(), contentSpec)) {
            valid = false;
        }
        if (contentSpec.getFeedback() != null && !postValidateTopic(contentSpec.getFeedback(), contentSpec)) {
            valid = false;
        }
        if (contentSpec.getLegalNotice() != null && !postValidateTopic(contentSpec.getLegalNotice(), contentSpec)) {
            valid = false;
        }
        if (contentSpec.getAuthorGroup() != null && !postValidateTopic(contentSpec.getAuthorGroup(), contentSpec)) {
            valid = false;
        }
        if (contentSpec.getAbstractTopic() != null && !postValidateTopic(contentSpec.getAbstractTopic(), contentSpec)) {
            valid = false;
        }

        // Validate that the files exist
        if (contentSpec.getFiles() != null) {
            if (!validateFiles(contentSpec)) {
                valid = false;
            }
        }

        // Make sure that the abstract is valid docbook xml
        if (!isNullOrEmpty(contentSpec.getAbstract())) {
            String wrappedAbstract = contentSpec.getAbstract();
            if (!contentSpec.getAbstract().matches("^<(formal|sim)?para>(.|\\s)*")) {
                wrappedAbstract = "<para>" + contentSpec.getAbstract() + "</para>";
            }
            wrappedAbstract = "<abstract>" + wrappedAbstract + "</abstract>";

            // Get the docbook DTD
            final String docbookFileName;
            final XMLValidator.ValidationMethod validationMethod;
            final byte[] docbookSchema;
            if (contentSpec.getFormat().equalsIgnoreCase(CommonConstants.DOCBOOK_50_TITLE)) {
                final BlobConstantWrapper docbookRng = blobConstantProvider.getBlobConstant(serverEntities.getDocBook50RNGBlobConstantId());
                docbookFileName = docbookRng.getName();
                validationMethod = XMLValidator.ValidationMethod.RELAXNG;
                docbookSchema = docbookRng.getValue();
                // Further wrap the abstract for docbook 5.0
                wrappedAbstract = DocBookUtilities.addDocBook50Namespace("<book><info><title />" + wrappedAbstract + "</info></book>");
            } else {
                final BlobConstantWrapper rocbookDtd = blobConstantProvider.getBlobConstant(serverEntities.getRocBook45DTDBlobConstantId());
                docbookFileName = rocbookDtd.getName();
                validationMethod = XMLValidator.ValidationMethod.DTD;
                docbookSchema = rocbookDtd.getValue();
            }

            // Create the dummy XML entities file
            final StringBuilder xmlEntities = new StringBuilder(CSConstants.DUMMY_CS_NAME_ENT_FILE);
            if (!isNullOrEmpty(contentSpec.getEntities())) {
                xmlEntities.append(contentSpec.getEntities());
            }

            // Validate the XML content against the dtd
            final XMLValidator validator = new XMLValidator(false);
            if (!validator.validate(validationMethod, wrappedAbstract, docbookFileName, docbookSchema, xmlEntities.toString(),
                    "abstract")) {
                valid = false;
                final String line = CommonConstants.CS_ABSTRACT_TITLE + " = " + contentSpec.getAbstract();
                log.error(String.format(ProcessorConstants.ERROR_INVALID_ABSTRACT_MSG, validator.getErrorText(), line));
            }
        }

        // Check that each level is valid
        if (!postValidateLevel(contentSpec.getBaseLevel(), contentSpec)) {
            valid = false;
        }

        return valid;
    }

    /**
     * Checks to make sure that the files specified in a content spec are valid and exist.
     *
     * @param contentSpec The content spec to check.
     * @return True if all the files are valid, otherwise false.
     */
    protected boolean validateFiles(final ContentSpec contentSpec) {
        final FileList fileList = contentSpec.getFileList();
        boolean valid = true;

        if (fileList != null && !fileList.getValue().isEmpty()) {
            for (final File file : fileList.getValue()) {
                FileWrapper fileWrapper = null;
                try {
                    fileWrapper = fileProvider.getFile(file.getId(), file.getRevision());
                } catch (NotFoundException e) {
                    log.debug("Could not find file for id " + file.getId());
                }

                if (fileWrapper == null) {
                    log.error(String.format(ProcessorConstants.ERROR_FILE_ID_NONEXIST_MSG, fileList.getLineNumber(), file.getText()));
                    valid = false;
                } else if (file.getTitle() != null) {
                    // Make sure the titles sync up, as a title was specified.
                    if (!fileWrapper.getFilename().equals(file.getTitle())) {
                        final String errorMsg = String.format(ProcessorConstants.ERROR_FILE_TITLE_NO_MATCH_MSG, fileList.getLineNumber(),
                                file.getTitle(), fileWrapper.getFilename());
                        if (processingOptions.isStrictTitles()) {
                            log.error(errorMsg);
                            valid = false;
                        } else {
                            log.warn(errorMsg);
                            file.setTitle(fileWrapper.getFilename());
                        }
                    }
                } else {
                    // The short format was used if the title is null, so add it
                    file.setTitle(fileWrapper.getFilename());
                }
            }
        }

        return valid;
    }

    /**
     * Validate a set of relationships created when parsing.
     *
     * @param contentSpec The content spec to be validated.
     * @return True if the relationships are valid, otherwise false.
     */
    public boolean preValidateRelationships(final ContentSpec contentSpec) {
        boolean error = false;

        // Create the map of unique ids to spec topics
        final Map<String, List<ITopicNode>> specTopicMap = ContentSpecUtilities.getIdTopicNodeMap(contentSpec);

        final Map<SpecNodeWithRelationships, List<Relationship>> relationships = contentSpec.getRelationships();
        for (final Entry<SpecNodeWithRelationships, List<Relationship>> relationshipEntry : relationships.entrySet()) {
            final SpecNodeWithRelationships specNode = relationshipEntry.getKey();

            // Check if the app should be shutdown
            if (isShuttingDown.get()) {
                shutdown.set(true);
                return false;
            }

            for (final Relationship relationship : relationshipEntry.getValue()) {
                // Check if the app should be shutdown
                if (isShuttingDown.get()) {
                    shutdown.set(true);
                    return false;
                }

                final String relatedId = relationship.getSecondaryRelationshipId();
                // The relationship points to a target so it must be a level or topic
                if (relationship instanceof TargetRelationship) {
                    final SpecNode node = ((TargetRelationship) relationship).getSecondaryRelationship();
                    if (node instanceof SpecTopic) {
                        final SpecTopic targetTopic = (SpecTopic) node;
                        if (!validateRelationshipToTopic(relationship, specNode, relatedId, targetTopic, specTopicMap)) {
                            error = true;
                        }
                    } else if (node instanceof Level) {
                        final Level targetLevel = (Level) node;
                        if (!validateRelationshipToLevel(relationship, specNode, targetLevel)) {
                            error = true;
                        }
                    }
                } else if (relationship instanceof TopicRelationship) {
                    final SpecTopic relatedTopic = ((TopicRelationship) relationship).getSecondaryRelationship();
                    if (!validateRelationshipToTopic(relationship, specNode, relatedId, relatedTopic, specTopicMap)) {
                        error = true;
                    }
                }
            }
        }
        return !error;
    }

    /**
     * @param relationship
     * @param node
     * @param relatedId
     * @param relatedTopic
     * @return True if the topic relationship is valid, otherwise false.
     */
    private boolean validateRelationshipToTopic(final Relationship relationship, final SpecNodeWithRelationships node,
            final String relatedId, final SpecTopic relatedTopic, final Map<String, List<ITopicNode>> topicNodeMap) {
        final List<ITopicNode> relatedSpecTopics = topicNodeMap.get(relatedTopic.getId());
        if (relatedSpecTopics == null) {
            log.error(String.format(ProcessorConstants.ERROR_RELATED_TOPIC_NONEXIST_MSG, node.getLineNumber(), relatedId,
                    node.getText()));
            return false;
        } else if (relationship instanceof TopicRelationship && relatedId.startsWith("X")) {
            // Duplicated topics are never unique so throw an error straight away.
            log.error(String.format(ProcessorConstants.ERROR_INVALID_DUPLICATE_RELATIONSHIP_MSG, node.getLineNumber(),
                    node.getText()));
            return false;
        } else if (relationship instanceof TopicRelationship &&  relatedSpecTopics.size() > 1) {
            // Check to make sure the topic isn't duplicated
            final List<ITopicNode> relatedTopics = topicNodeMap.get(relatedTopic.getId());

            // Build up the line numbers message
            final StringBuilder lineNumbers = new StringBuilder();
            for (int i = 0; i < relatedTopics.size(); i++) {
                if (i == relatedTopics.size() - 1) {
                    lineNumbers.append(" and ");
                } else if (lineNumbers.length() != 0) {
                    lineNumbers.append(", ");
                }

                lineNumbers.append(relatedTopics.get(i).getLineNumber());
            }

            log.error(String.format(ProcessorConstants.ERROR_INVALID_RELATIONSHIP_MSG, node.getLineNumber(), relatedId,
                    lineNumbers.toString(), node.getText()));
            return false;
        } else if (relatedSpecTopics.get(0) instanceof InfoTopic) {
            // Make sure we aren't trying to relate to an info topic
            log.error(String.format(ProcessorConstants.ERROR_RELATED_TOPIC_IS_INFO_MSG, node.getLineNumber(), relatedId, node.getText()));
            return false;
        } else if (relatedTopic == node) {
            // Check to make sure the topic doesn't relate to itself
            log.error(String.format(ProcessorConstants.ERROR_TOPIC_RELATED_TO_ITSELF_MSG, node.getLineNumber(), node.getText()));
            return false;
        } else if (node instanceof InitialContent && ((InitialContent) node).getSpecTopics().contains(relatedTopic)) {
            // Check that the relationship isn't to something in the front matter content, as it would relate to itself
            log.error(String.format(ProcessorConstants.ERROR_TOPIC_RELATED_TO_ITSELF_MSG, node.getLineNumber(), node.getText()));
            return false;
        } else if (node instanceof SpecTopic && ((SpecTopic) node).getTopicType() == TopicType.INITIAL_CONTENT) {
            // Initial Content topics can't have relationships
            log.error(String.format(ProcessorConstants.ERROR_INITIAL_CONTENT_TOPIC_RELATIONSHIP_MSG, node.getLineNumber(), node.getText()));
            return false;
        } else {
            final String relatedTitle = relatedTopic.getTitle();
            if (relationship.getRelationshipTitle() != null && !relationship.getRelationshipTitle().equals(relatedTitle)) {
                if (processingOptions.isStrictTitles()) {
                    log.error(String.format(ProcessorConstants.ERROR_RELATED_TITLE_NO_MATCH_MSG, node.getLineNumber(),
                            relationship.getRelationshipTitle(), relatedTitle));
                    return false;
                } else {
                    log.warn(String.format(ProcessorConstants.WARN_RELATED_TITLE_NO_MATCH_MSG, node.getLineNumber(),
                            relationship.getRelationshipTitle(), relatedTitle));
                }
            }
        }

        return true;
    }

    /**
     * @param relationship
     * @param node
     * @param relatedLevel
     * @return True if the level relationship is valid, otherwise false.
     */
    private boolean validateRelationshipToLevel(final Relationship relationship, final SpecNodeWithRelationships node,
            final Level relatedLevel) {
        if (relationship.getType() == RelationshipType.NEXT) {
            log.error(String.format(ProcessorConstants.ERROR_NEXT_RELATED_LEVEL_MSG, node.getLineNumber(),
                    node.getText()));
            return false;
        } else if (relationship.getType() == RelationshipType.PREVIOUS) {
            log.error(String.format(ProcessorConstants.ERROR_PREV_RELATED_LEVEL_MSG, node.getLineNumber(),
                    node.getText()));
            return false;
        } else if (node == relatedLevel) {
            // Check to make sure the relationship doesn't relate to itself
            log.error(String.format(ProcessorConstants.ERROR_LEVEL_RELATED_TO_ITSELF_MSG, node.getLineNumber(),
                    relatedLevel.getLevelType().getTitle(), node.getText()));
            return false;
        } else if (node instanceof SpecTopic && ((SpecTopic) node).getTopicType() == TopicType.INITIAL_CONTENT) {
            // Initial Content topics can't have relationships
            log.error(String.format(ProcessorConstants.ERROR_INITIAL_CONTENT_TOPIC_RELATIONSHIP_MSG, node.getLineNumber(), node.getText()));
            return false;
        } else if (relationship.getRelationshipTitle() != null && !relationship.getRelationshipTitle().equals(
                relatedLevel.getTitle())) {
            if (processingOptions.isStrictTitles()) {
                log.error(String.format(ProcessorConstants.ERROR_RELATED_TITLE_NO_MATCH_MSG, relatedLevel.getLineNumber(),
                        relationship.getRelationshipTitle(), relatedLevel.getTitle()));
                return false;
            } else {
                log.warn(String.format(ProcessorConstants.WARN_RELATED_TITLE_NO_MATCH_MSG, node.getLineNumber(),
                        relationship.getRelationshipTitle(), relatedLevel.getTitle()));
            }
        }

        return true;
    }

    /**
     * Validates a level to ensure its format and child levels/topics are valid.
     *
     *
     * @param level              The level to be validated.
     * @param specTopics         The list of topics that exist within the content specification.
     * @param infoTopics
     *@param bookType           The type of book the level should be validated for.
     * @param contentSpec        The content spec the level belongs to.   @return True if the level is valid otherwise false.
     */
    public boolean preValidateLevel(final Level level, final Map<String, SpecTopic> specTopics, final Map<String, InfoTopic> infoTopics,
            final BookType bookType, final ContentSpec contentSpec) {
        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            shutdown.set(true);
            return false;
        }

        boolean valid = true;

        // Make sure the level has a type, if it doesn't then return false immediately
        final LevelType levelType = level.getLevelType();
        if (levelType == null) {
            log.error(ProcessorConstants.ERROR_PROCESSING_ERROR_MSG);
            return false;
        }

        // Check that the level isn't empty
        if (levelType != LevelType.PART && level.getNumberOfSpecTopics() <= 0 && level.getNumberOfChildLevels() <= 0) {
            log.error(format(ProcessorConstants.ERROR_LEVEL_NO_TOPICS_MSG, level.getLineNumber(), levelType.getTitle(),
                    levelType.getTitle(), level.getText()));
            valid = false;
        } else if (levelType == LevelType.PART && level.getNumberOfChildLevels() <= 0) {
            log.error(format(ProcessorConstants.ERROR_LEVEL_NO_CHILD_LEVELS_MSG, level.getLineNumber(), levelType.getTitle(),
                    levelType.getTitle(), level.getText()));
            valid = false;
        }

        // Sections have to have more than just one initial text topic
        if (levelType == LevelType.SECTION && level.getNumberOfSpecTopics() <= 0 && level.getNumberOfChildLevels() <= 1 && level
                .getFirstSpecNode() instanceof InitialContent) {
            if (((InitialContent) level.getFirstSpecNode()).getNumberOfSpecTopics() <= 1) {
                log.error(format(ProcessorConstants.ERROR_SECTION_NO_TOPICS_OR_INITIAL_CONTENT_MSG, level.getLineNumber(), level.getText()));
                valid = false;
            }
        }

        if (isNullOrEmpty(level.getTitle())) {
            log.error(String.format(ProcessorConstants.ERROR_LEVEL_NO_TITLE_MSG, level.getLineNumber(), levelType.getTitle(),
                    level.getText()));
            valid = false;
        }

        // Validate the info topic
        if (level.getInfoTopic() != null) {
            preValidateInfoTopic(level.getInfoTopic(), contentSpec, infoTopics);
        }

        // Validate the sub levels and topics
        for (final Node childNode : level.getChildNodes()) {
            if (childNode instanceof Level) {
                if (!preValidateLevel((Level) childNode, specTopics, infoTopics, bookType, contentSpec)) {
                    valid = false;
                }
            } else if (childNode instanceof SpecTopic) {
                if (!preValidateTopic((SpecTopic) childNode, specTopics, bookType, contentSpec)) {
                    valid = false;
                }
            }
        }

        // Validate certain requirements depending on the type of level
        LevelType parentLevelType = level.getParent() != null ? level.getParent().getLevelType() : null;
        if (bookType == BookType.ARTICLE || bookType == BookType.ARTICLE_DRAFT) {
            switch (levelType) {
                case APPENDIX:
                    if (parentLevelType != LevelType.BASE) {
                        log.error(String.format(ProcessorConstants.ERROR_ARTICLE_NESTED_APPENDIX_MSG, level.getLineNumber(),
                                level.getText()));
                        valid = false;
                    }

                    // Check that the appendix is at the end of the article
                    final Integer nodeListId = level.getParent().getChildNodes().indexOf(level);
                    final ListIterator<Node> parentNodes = level.getParent().getChildNodes().listIterator(nodeListId);

                    while (parentNodes.hasNext()) {
                        final Node node = parentNodes.next();
                        if (node instanceof Level && ((Level) node).getLevelType() != LevelType.APPENDIX) {
                            log.error(format(ProcessorConstants.ERROR_CS_APPENDIX_STRUCTURE_MSG, level.getLineNumber(), level.getText()));
                            valid = false;
                        }
                    }
                    break;
                case CHAPTER:
                    log.error(String.format(ProcessorConstants.ERROR_ARTICLE_CHAPTER_MSG, level.getLineNumber(), level.getText()));
                    valid = false;
                    break;
                case PROCESS:
                    log.error(String.format(ProcessorConstants.ERROR_ARTICLE_PROCESS_MSG, level.getLineNumber(), level.getText()));
                    valid = false;
                    break;
                case PART:
                    log.error(String.format(ProcessorConstants.ERROR_ARTICLE_PART_MSG, level.getLineNumber(), level.getText()));
                    valid = false;
                    break;
                case PREFACE:
                    log.error(format(ProcessorConstants.ERROR_ARTICLE_PREFACE_MSG, level.getLineNumber(), level.getText()));
                    valid = false;
                    break;
                case SECTION:
                    if (!(parentLevelType == LevelType.BASE || parentLevelType == LevelType.SECTION)) {
                        log.error(format(ProcessorConstants.ERROR_ARTICLE_SECTION_MSG, level.getLineNumber(), level.getText()));
                        valid = false;
                    }
                    break;
                case INITIAL_CONTENT:
                    // Check that the initial content is at the start of the container
                    if (level.getParent() == null || level.getParent().getFirstSpecNode() != level) {
                        log.error(format(ProcessorConstants.ERROR_CS_INITIAL_CONTENT_STRUCTURE_MSG, level.getLineNumber(),
                                parentLevelType.getTitle(), level.getText()));
                        valid = false;
                    }
                    break;
                default:
                    break;
            }
        }
        // Generic book based validation
        else {
            switch (levelType) {
                case APPENDIX:
                    if (!(parentLevelType == LevelType.BASE || parentLevelType == LevelType.PART)) {
                        log.error(format(ProcessorConstants.ERROR_CS_NESTED_APPENDIX_MSG, level.getLineNumber(), level.getText()));
                        valid = false;
                    }

                    // Check that the appendix is at the end of the book
                    final Integer nodeListId = level.getParent().getChildNodes().indexOf(level);
                    final ListIterator<Node> parentNodes = level.getParent().getChildNodes().listIterator(nodeListId);

                    while (parentNodes.hasNext()) {
                        final Node node = parentNodes.next();
                        if (node instanceof Level && ((Level) node).getLevelType() != LevelType.APPENDIX) {
                            log.error(format(ProcessorConstants.ERROR_CS_APPENDIX_STRUCTURE_MSG, level.getLineNumber(), level.getText()));
                            valid = false;
                        }
                    }

                    break;
                case CHAPTER:
                    if (!(parentLevelType == LevelType.BASE || parentLevelType == LevelType.PART)) {
                        log.error(format(ProcessorConstants.ERROR_CS_NESTED_CHAPTER_MSG, level.getLineNumber(), level.getText()));
                        valid = false;
                    }
                    break;
                case PROCESS:
                    // Check that the process has no children
                    if (level.getNumberOfChildLevels() != 0) {
                        log.error(format(ProcessorConstants.ERROR_PROCESS_HAS_LEVELS_MSG, level.getLineNumber(), level.getText()));
                        valid = false;
                    }
                    break;
                case PART:
                    if (parentLevelType != LevelType.BASE) {
                        log.error(format(ProcessorConstants.ERROR_CS_NESTED_PART_MSG, level.getLineNumber(), level.getText()));
                        valid = false;
                    }
                    break;
                case PREFACE:
                    if (parentLevelType != LevelType.BASE || parentLevelType == LevelType.PART) {
                        log.error(format(ProcessorConstants.ERROR_CS_NESTED_PREFACE_MSG, level.getLineNumber(), level.getText()));
                        valid = false;
                    }
                    break;
                case SECTION:
                    if (!(parentLevelType == LevelType.APPENDIX || parentLevelType == LevelType.CHAPTER || parentLevelType == LevelType
                            .PREFACE || parentLevelType == LevelType.SECTION)) {
                        log.error(format(ProcessorConstants.ERROR_CS_SECTION_NO_CHAPTER_MSG, level.getLineNumber(), level.getText()));
                        valid = false;
                    }
                    break;
                case INITIAL_CONTENT:
                    // Check that the initial content is at the start of the container
                    if (level.getParent() == null || level.getParent().getFirstSpecNode() != level) {
                        log.error(format(ProcessorConstants.ERROR_CS_INITIAL_CONTENT_STRUCTURE_MSG, level.getLineNumber(),
                                parentLevelType.getTitle(), level.getText()));
                        valid = false;
                    }
                    break;
                default:
                    break;
            }
        }

        // Validate the tags
        if (processingOptions.isPrintChangeWarnings() && !level.getTags(false).isEmpty() && level.hasRevisionSpecTopics()) {
            log.warn(String.format(ProcessorConstants.WARN_LEVEL_TAGS_IGNORE_MSG, level.getLineNumber(), level.getLevelType().getTitle(),
                    "revision", level.getText()));
        }

        // Check for conflicting conditions
        checkForConflictingCondition(level, contentSpec);

        return valid;
    }

    /**
     * Validates a level to ensure its format and child levels/topics are valid.
     *
     *
     * @param level The level to be validated.
     * @param contentSpec
     * @return True if the level is valid otherwise false.
     */
    public boolean postValidateLevel(final Level level, final ContentSpec contentSpec) {
        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            shutdown.set(true);
            return false;
        }

        boolean valid = true;

        // Validate the tags
        if (!validateTopicTags(level, level.getTags(false))) {
            valid = false;
        }

        // Validate the info topic
        if (level.getInfoTopic() != null) {
            postValidateInfoTopic(level.getInfoTopic(), contentSpec);
        }

        // Validate the sub levels and topics
        for (final Node childNode : level.getChildNodes()) {
            if (childNode instanceof Level) {
                if (!postValidateLevel((Level) childNode, contentSpec)) {
                    valid = false;
                }
            } else if (childNode instanceof SpecTopic) {
                if (!postValidateTopic((SpecTopic) childNode, contentSpec)) {
                    valid = false;
                }
            }
        }

        return valid;
    }

    /**
     * Validates a topic against the database and for formatting issues.
     *
     * @param specTopic   The topic to be validated.
     * @param specTopics  The list of topics that exist within the content specification.
     * @param bookType    The type of book the topic is to be validated against.
     * @param contentSpec The content spec the topic belongs to.
     * @return True if the topic is valid otherwise false.
     */
    public boolean preValidateTopic(final SpecTopic specTopic, final Map<String, SpecTopic> specTopics, final BookType bookType,
            final ContentSpec contentSpec) {
        return preValidateTopic(specTopic, specTopics, bookType, true, contentSpec);
    }

    /**
     * Validates a topic against the database and for formatting issues.
     *
     * @param specTopic   The topic to be validated.
     * @param specTopics  The list of topics that exist within the content specification.
     * @param bookType    The type of book the topic is to be validated against.
     * @param contentSpec The content spec the topic belongs to.
     * @return True if the topic is valid otherwise false.
     */
    public boolean preValidateTopic(final SpecTopic specTopic, final Map<String, SpecTopic> specTopics, final BookType bookType,
            boolean allowRelationships, final ContentSpec contentSpec) {
        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            shutdown.set(true);
            return false;
        }

        boolean valid = true;

        // Checks that the id isn't null and is a valid topic ID
        if (specTopic.getId() == null || !specTopic.getId().matches(CSConstants.ALL_TOPIC_ID_REGEX)) {
            log.error(String.format(ProcessorConstants.ERROR_INVALID_TOPIC_ID_MSG, specTopic.getLineNumber(), specTopic.getText()));
            valid = false;
        }

        if ((bookType == BookType.BOOK || bookType == BookType.BOOK_DRAFT) && specTopic.getParent() instanceof Level) {
            final Level parent = (Level) specTopic.getParent();
            // Check that the topic is inside a chapter/section/process/appendix/part/preface
            final LevelType parentLevelType = parent.getLevelType();
            if (parent == null || parentLevelType == LevelType.BASE) {
                log.error(format(ProcessorConstants.ERROR_TOPIC_OUTSIDE_CHAPTER_MSG, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            }

            // Check that there are no levels in the parent part (ie the topic is in the intro)
            if (parent != null && parentLevelType == LevelType.PART) {
                final List<Node> parentChildren = parent.getChildNodes();
                final int index = parentChildren.indexOf(specTopic);

                for (int i = 0; i < index; i++) {
                    final Node node = parentChildren.get(i);
                    if (node instanceof Level && ((Level) node).getLevelType() != LevelType.INITIAL_CONTENT) {
                        log.error(String.format(ProcessorConstants.ERROR_TOPIC_NOT_IN_PART_INTRO_MSG, specTopic.getLineNumber(),
                                specTopic.getText()));
                        valid = false;
                        break;
                    }
                }
            }
        }

        // Check that the title exists
        if (isNullOrEmpty(specTopic.getTitle())) {
            log.error(String.format(ProcessorConstants.ERROR_TOPIC_NO_TITLE_MSG, specTopic.getLineNumber(), specTopic.getText()));
            valid = false;
        }

        // Check that we aren't using translations for anything but existing topics
        if (!specTopic.isTopicAnExistingTopic()) {
            // Check that we aren't processing translations
            if (processingOptions.isTranslation()) {
                log.error(String.format(ProcessorConstants.ERROR_TOPIC_NO_NEW_TRANSLATION_TOPIC, specTopic.getLineNumber(),
                        specTopic.getText()));
                valid = false;
            }
        }

        // Check that we are allowed to create new topics
        if (!specTopic.isTopicAnExistingTopic() && !processingOptions.isAllowNewTopics()) {
            log.error(String.format(ProcessorConstants.ERROR_TOPIC_NO_NEW_TOPIC_BUILD, specTopic.getLineNumber(), specTopic.getText()));
            valid = false;
        }

        // New Topics
        if (specTopic.isTopicANewTopic()) {
            if (isNullOrEmpty(specTopic.getType())) {
                log.error(String.format(ProcessorConstants.ERROR_TOPIC_NO_TYPE_MSG, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            }

            // Check Assigned Writer exists
            if (!preValidateAssignedWriter(specTopic)) {
                valid = false;
            }
            // Existing Topics
        } else if (specTopic.isTopicAnExistingTopic()) {
            // Check that tags aren't trying to be removed
            if (!specTopic.getRemoveTags(false).isEmpty()) {
                log.error(String.format(ProcessorConstants.ERROR_TOPIC_EXISTING_TOPIC_CANNOT_REMOVE_TAGS, specTopic.getLineNumber(),
                        specTopic.getText()));
                valid = false;
            }

            // Check that tags aren't trying to be added to a revision
            if (processingOptions.isPrintChangeWarnings() && specTopic.getRevision() != null && !specTopic.getTags(false).isEmpty()) {
                log.warn(
                        String.format(ProcessorConstants.WARN_TAGS_IGNORE_MSG, specTopic.getLineNumber(), "revision", specTopic.getText()));
            }

            // Check that urls aren't trying to be added
            if (!specTopic.getSourceUrls(false).isEmpty()) {
                log.error(String.format(ProcessorConstants.ERROR_TOPIC_EXISTING_TOPIC_CANNOT_ADD_SOURCE_URLS, specTopic.getLineNumber(),
                        specTopic.getText()));
                valid = false;
            }

            // Check that the assigned writer and description haven't been set
            if (specTopic.getAssignedWriter(false) != null || specTopic.getDescription(false) != null) {
                log.error(
                        String.format(ProcessorConstants.ERROR_TOPIC_EXISTING_BAD_OPTIONS, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            }

            // Check that we aren't processing translations
            if (!specTopic.getTags(true).isEmpty() && processingOptions.isTranslation()) {
                log.error(String.format(ProcessorConstants.ERROR_TOPIC_NO_TAGS_TRANSLATION_TOPIC, specTopic.getLineNumber(),
                        specTopic.getText()));
                valid = false;
            }
        }
        // Duplicated Topics
        else if (specTopic.isTopicADuplicateTopic()) {
            String temp = "N" + specTopic.getId().substring(1);

            // Check that the topic exists in the content specification
            if (!specTopics.containsKey(temp)) {
                log.error(String.format(ProcessorConstants.ERROR_TOPIC_NONEXIST_MSG, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            } else {
                // Check that the topic titles match the original
                if (!specTopic.getTitle().equals(specTopics.get(temp).getTitle())) {
                    String topicTitleMsg = "Topic " + specTopic.getId() + ": " + specTopics.get(temp).getTitle();
                    log.error(String.format(ProcessorConstants.ERROR_TOPIC_TITLES_NONMATCH_MSG, specTopic.getLineNumber(),
                            specTopic.getText(), topicTitleMsg));
                    valid = false;
                }
            }
            // Cloned Topics
        } else if (specTopic.isTopicAClonedTopic()) {
            // Check if a description or type exists. If one does then generate an error.
            if ((specTopic.getType() != null && !specTopic.getType().equals("")) || (specTopic.getDescription(
                    false) != null && !specTopic.getDescription(false).equals(""))) {
                log.error(String.format(ProcessorConstants.ERROR_TOPIC_CLONED_BAD_OPTIONS, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            }
            // Duplicated Cloned Topics
        } else if (specTopic.isTopicAClonedDuplicateTopic()) {
            // Find the duplicate topic in the content spec
            final String topicId = specTopic.getId().substring(1);
            int count = 0;
            SpecTopic clonedTopic = null;
            for (final Entry<String, SpecTopic> entry : specTopics.entrySet()) {
                final String uniqueTopicId = entry.getKey();

                if (uniqueTopicId.endsWith(topicId) && !uniqueTopicId.endsWith(specTopic.getId())) {
                    clonedTopic = entry.getValue();
                    count++;
                }
            }

            // Check that the topic exists
            if (count == 0) {
                log.error(String.format(ProcessorConstants.ERROR_TOPIC_NONEXIST_MSG, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            }
            // Check that the referenced topic is unique
            else if (count > 1) {
                log.error(
                        String.format(ProcessorConstants.ERROR_TOPIC_DUPLICATE_CLONES_MSG, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            } else {
                // Check that the title matches
                if (!specTopic.getTitle().equals(clonedTopic.getTitle())) {
                    String topicTitleMsg = "Topic " + specTopic.getId() + ": " + clonedTopic.getTitle();
                    log.error(String.format(ProcessorConstants.ERROR_TOPIC_TITLES_NONMATCH_MSG, specTopic.getLineNumber(),
                            specTopic.getText(), topicTitleMsg));
                    valid = false;
                }
            }
        }

        // Check to make sure no relationships exist if they aren't allowed
        if (!allowRelationships && specTopic.getRelationships().size() > 0) {
            log.error(format(ProcessorConstants.ERROR_TOPIC_HAS_RELATIONSHIPS_MSG, specTopic.getLineNumber(), specTopic.getText()));
            valid = false;
        }

        // Check for conflicting conditions
        checkForConflictingCondition(specTopic, contentSpec);

        return valid;
    }

    /**
     * Validates a topic against the database and for formatting issues.
     *
     *
     * @param infoTopic   The topic to be validated.
     * @param contentSpec The content spec the topic belongs to.
     * @param infoTopics
     * @return True if the topic is valid otherwise false.
     */
    public boolean preValidateInfoTopic(final InfoTopic infoTopic, final ContentSpec contentSpec, final Map<String,
            InfoTopic> infoTopics) {
        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            shutdown.set(true);
            return false;
        }

        boolean valid = true;

        // Checks that the id isn't null and is a valid topic ID
        if (infoTopic.getId() == null || !infoTopic.getId().matches(CSConstants.ALL_TOPIC_ID_REGEX)) {
            log.error(String.format(ProcessorConstants.ERROR_INVALID_TOPIC_ID_MSG, infoTopic.getLineNumber(), infoTopic.getText()));
            valid = false;
        }

        // Check that we aren't using translations for anything but existing topics
        if (!infoTopic.isTopicAnExistingTopic()) {
            // Check that we aren't processing translations
            if (processingOptions.isTranslation()) {
                log.error(String.format(ProcessorConstants.ERROR_TOPIC_NO_NEW_TRANSLATION_TOPIC, infoTopic.getLineNumber(),
                        infoTopic.getText()));
                valid = false;
            }
        }

        // Check that we are allowed to create new topics
        if (!infoTopic.isTopicAnExistingTopic() && !processingOptions.isAllowNewTopics()) {
            log.error(String.format(ProcessorConstants.ERROR_TOPIC_NO_NEW_TOPIC_BUILD, infoTopic.getLineNumber(), infoTopic.getText()));
            valid = false;
        }

        // New Topics
        if (infoTopic.isTopicANewTopic()) {
            // Check Assigned Writer exists
            if (!preValidateAssignedWriter(infoTopic)) {
                valid = false;
            }
            // Existing Topics
        } else if (infoTopic.isTopicAnExistingTopic()) {
            // Check that tags aren't trying to be removed
            if (!infoTopic.getRemoveTags(false).isEmpty()) {
                log.error(String.format(ProcessorConstants.ERROR_TOPIC_EXISTING_TOPIC_CANNOT_REMOVE_TAGS, infoTopic.getLineNumber(),
                        infoTopic.getText()));
                valid = false;
            }

            // Check that tags aren't trying to be added to a revision
            if (processingOptions.isPrintChangeWarnings() && infoTopic.getRevision() != null && !infoTopic.getTags(false).isEmpty()) {
                log.warn(
                        String.format(ProcessorConstants.WARN_TAGS_IGNORE_MSG, infoTopic.getLineNumber(), "revision", infoTopic.getText()));
            }

            // Check that the assigned writer and description haven't been set
            if (infoTopic.getAssignedWriter(false) != null || infoTopic.getDescription(false) != null) {
                log.error(
                        String.format(ProcessorConstants.ERROR_TOPIC_EXISTING_BAD_OPTIONS, infoTopic.getLineNumber(), infoTopic.getText()));
                valid = false;
            }

            // Check that we aren't processing translations
            if (!infoTopic.getTags(true).isEmpty() && processingOptions.isTranslation()) {
                log.error(String.format(ProcessorConstants.ERROR_TOPIC_NO_TAGS_TRANSLATION_TOPIC, infoTopic.getLineNumber(),
                        infoTopic.getText()));
                valid = false;
            }
        }
        // Duplicated Topics
        else if (infoTopic.isTopicADuplicateTopic()) {
            String temp = "N" + infoTopic.getId().substring(1);

            // Check that the topic exists in the content specification
            if (!infoTopics.containsKey(temp)) {
                log.error(String.format(ProcessorConstants.ERROR_TOPIC_NONEXIST_MSG, infoTopic.getLineNumber(), infoTopic.getText()));
                valid = false;
            }
            // Cloned Topics
        } else if (infoTopic.isTopicAClonedTopic()) {
            // Check if a description or type exists. If one does then generate an error.
            if (infoTopic.getDescription(false) != null && !infoTopic.getDescription(false).equals("")) {
                log.error(String.format(ProcessorConstants.ERROR_TOPIC_CLONED_BAD_OPTIONS, infoTopic.getLineNumber(), infoTopic.getText()));
                valid = false;
            }
            // Duplicated Cloned Topics
        } else if (infoTopic.isTopicAClonedDuplicateTopic()) {
            // Find the duplicate topic in the content spec
            final String topicId = infoTopic.getId().substring(1);
            int count = 0;
            for (final Entry<String, InfoTopic> entry : infoTopics.entrySet()) {
                final String uniqueTopicId = entry.getKey();

                if (uniqueTopicId.endsWith(topicId) && !uniqueTopicId.endsWith(infoTopic.getId())) {
                    count++;
                }
            }

            // Check that the topic exists
            if (count == 0) {
                log.error(String.format(ProcessorConstants.ERROR_TOPIC_NONEXIST_MSG, infoTopic.getLineNumber(), infoTopic.getText()));
                valid = false;
            }
            // Check that the referenced topic is unique
            else if (count > 1) {
                log.error(
                        String.format(ProcessorConstants.ERROR_TOPIC_DUPLICATE_CLONES_MSG, infoTopic.getLineNumber(), infoTopic.getText()));
                valid = false;
            }
        }

        // Check for conflicting conditions
        checkForConflictingCondition(infoTopic, contentSpec);

        return valid;
    }

    /**
     * Validates a topic against the database and for formatting issues.
     *
     *
     * @param specTopic The topic to be validated.
     * @param contentSpec
     * @return True if the topic is valid otherwise false.
     */
    @SuppressWarnings("unchecked")
    public boolean postValidateTopic(final SpecTopic specTopic, final ContentSpec contentSpec) {
        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            shutdown.set(true);
            return false;
        }

        boolean valid = true;

        // New Topics
        if (specTopic.isTopicANewTopic()) {
            // Check that the type entered exists
            TagWrapper type = null;
            try {
                type = tagProvider.getTagByName(specTopic.getType());
            } catch (NotFoundException e) {

            }

            if (type == null || !type.containedInCategory(serverEntities.getTypeCategoryId())) {
                log.error(String.format(ProcessorConstants.ERROR_TYPE_NONEXIST_MSG, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            } else if (specTopic.getTopicType() == TopicType.LEGAL_NOTICE && !type.getId().equals(serverEntities.getLegalNoticeTagId())) {
                log.error(format(ProcessorConstants.ERROR_INVALID_TYPE_MSG, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            } else if (specTopic.getTopicType() == TopicType.REVISION_HISTORY && !type.getId().equals(
                    serverEntities.getRevisionHistoryTagId())) {
                log.error(format(ProcessorConstants.ERROR_INVALID_TYPE_MSG, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            } else if (specTopic.getTopicType() == TopicType.AUTHOR_GROUP && !type.getId().equals(serverEntities.getAuthorGroupTagId())) {
                log.error(format(ProcessorConstants.ERROR_INVALID_TYPE_MSG, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            } else if (specTopic.getTopicType() == TopicType.ABSTRACT && !type.getId().equals(serverEntities.getAbstractTagId())) {
                log.error(format(ProcessorConstants.ERROR_INVALID_TYPE_MSG, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            }

            // Validate the tags
            if (!validateTopicTags(specTopic, specTopic.getTags(false))) {
                valid = false;
            }

            // Check Assigned Writer exists
            if (!postValidateAssignedWriter(specTopic)) {
                valid = false;
            }
        }
        // Existing Topics
        else if (specTopic.isTopicAnExistingTopic()) {
            // Calculate the revision for the topic
            final Integer revision;
            if (specTopic.getRevision() == null && processingOptions.getMaxRevision() != null) {
                revision = processingOptions.getMaxRevision();
            } else {
                revision = specTopic.getRevision();
            }

            // Check that the id actually exists
            BaseTopicWrapper<?> topic = null;
            try {
                if (processingOptions.isTranslation()) {
                    topic = EntityUtilities.getTranslatedTopicByTopicId(factory, Integer.parseInt(specTopic.getId()), revision,
                            processingOptions.getTranslationLocale() == null ? defaultLocale : processingOptions.getTranslationLocale());
                } else {
                    topic = topicProvider.getTopic(Integer.parseInt(specTopic.getId()), revision);
                }
            } catch (NotFoundException e) {
                log.debug("Could not find topic for id " + specTopic.getDBId());
            }

            // Check that the topic actually exists
            if (topic == null) {
                log.error(String.format(ProcessorConstants.ERROR_TOPIC_NONEXIST_MSG, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            } else {
                specTopic.setTopic(topic);

                // Check to see if the topic contains the "Internal-Only" tag
                if (serverEntities.getInternalOnlyTagId() != null && topic.hasTag(serverEntities.getInternalOnlyTagId())) {
                    log.warn(String.format(ProcessorConstants.WARN_INTERNAL_TOPIC_MSG, specTopic.getLineNumber(), specTopic.getText()));
                }

                if (!postValidateExistingTopic(specTopic, topic, contentSpec)) {
                    valid = false;
                }
            }
            // Cloned Topics
        } else if (specTopic.isTopicAClonedTopic()) {
            // Get the original topic from the database
            int topicId = Integer.parseInt(specTopic.getId().substring(1));
            TopicWrapper topic = null;
            try {
                topic = topicProvider.getTopic(topicId, specTopic.getRevision());
            } catch (NotFoundException e) {
                log.debug("Could not find topic for id " + topicId);
            }

            // Check that the original topic was found
            if (topic == null) {
                log.error(String.format(ProcessorConstants.ERROR_TOPIC_NONEXIST_MSG, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            } else {
                if (!postValidateExistingTopic(specTopic, topic, contentSpec)) {
                    valid = false;
                }

                // Check Assigned Writer exists
                if (!postValidateAssignedWriter(specTopic)) {
                    valid = false;
                }
            }
        }

        return valid;
    }

    /**
     * Validates a topic against the database and for formatting issues.
     *
     *
     * @param infoTopic The topic to be validated.
     * @param contentSpec
     * @return True if the topic is valid otherwise false.
     */
    @SuppressWarnings("unchecked")
    public boolean postValidateInfoTopic(final InfoTopic infoTopic, final ContentSpec contentSpec) {
        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            shutdown.set(true);
            return false;
        }

        boolean valid = true;

        // New Topics
        if (infoTopic.isTopicANewTopic()) {
            // Validate the tags
            if (!validateTopicTags(infoTopic, infoTopic.getTags(false))) {
                valid = false;
            }

            // Check Assigned Writer exists
            if (!postValidateAssignedWriter(infoTopic)) {
                valid = false;
            }
        }
        // Existing Topics
        else if (infoTopic.isTopicAnExistingTopic()) {
            // Calculate the revision for the topic
            final Integer revision;
            if (infoTopic.getRevision() == null && processingOptions.getMaxRevision() != null) {
                revision = processingOptions.getMaxRevision();
            } else {
                revision = infoTopic.getRevision();
            }

            // Check that the id actually exists
            BaseTopicWrapper<?> topic = null;
            try {
                if (processingOptions.isTranslation()) {
                    topic = EntityUtilities.getTranslatedTopicByTopicId(factory, Integer.parseInt(infoTopic.getId()), revision,
                            processingOptions.getTranslationLocale() == null ? defaultLocale : processingOptions.getTranslationLocale());
                } else {
                    topic = topicProvider.getTopic(Integer.parseInt(infoTopic.getId()), revision);
                }
            } catch (NotFoundException e) {
                log.debug("Could not find topic for id " + infoTopic.getDBId());
            }

            // Check that the topic actually exists
            if (topic == null) {
                log.error(String.format(ProcessorConstants.ERROR_TOPIC_NONEXIST_MSG, infoTopic.getLineNumber(), infoTopic.getText()));
                valid = false;
            } else {
                infoTopic.setTopic(topic);

                // Check to see if the topic contains the "Internal-Only" tag
                if (serverEntities.getInternalOnlyTagId() != null && topic.hasTag(serverEntities.getInternalOnlyTagId())) {
                    log.warn(String.format(ProcessorConstants.WARN_INTERNAL_TOPIC_MSG, infoTopic.getLineNumber(), infoTopic.getText()));
                }

                if (!postValidateExistingTopic(infoTopic, topic, contentSpec)) {
                    valid = false;
                }
            }
            // Cloned Topics
        } else if (infoTopic.isTopicAClonedTopic()) {
            // Get the original topic from the database
            int topicId = Integer.parseInt(infoTopic.getId().substring(1));
            TopicWrapper topic = null;
            try {
                topic = topicProvider.getTopic(topicId, infoTopic.getRevision());
            } catch (NotFoundException e) {
                log.debug("Could not find topic for id " + topicId);
            }

            // Check that the original topic was found
            if (topic == null) {
                log.error(String.format(ProcessorConstants.ERROR_TOPIC_NONEXIST_MSG, infoTopic.getLineNumber(), infoTopic.getText()));
                valid = false;
            } else {
                if (!postValidateExistingTopic(infoTopic, topic, contentSpec)) {
                    valid = false;
                }

                // Check Assigned Writer exists
                if (!postValidateAssignedWriter(infoTopic)) {
                    valid = false;
                }
            }
        }

        return valid;
    }

    private boolean postValidateExistingTopic(final ITopicNode topicNode, final BaseTopicWrapper<?> topic, final ContentSpec contentSpec) {
        boolean valid = true;

        // Check the title matches for spec topics
        if (topicNode instanceof SpecTopic
                && (topicNode.getTopicType() == TopicType.NORMAL || topicNode.getTopicType() == TopicType.INITIAL_CONTENT)) {
            final SpecTopic specTopic = (SpecTopic) topicNode;
            // Validate the title matches for normal topics
            final String topicTitle = getTopicTitleWithConditions(specTopic, topic);
            if (!specTopic.getTitle().equals(topicTitle)) {
                if (processingOptions.isStrictTitles()) {
                    final String topicTitleMsg = "Topic " + topicNode.getId() + ": " + topicTitle;
                    log.error(format(ProcessorConstants.ERROR_TOPIC_TITLES_NONMATCH_MSG, specTopic.getLineNumber(),
                            "Specified: " + specTopic.getTitle(), topicTitleMsg));
                    valid = false;
                } else {
                    final String topicTitleMsg = "Topic " + topicNode.getId() + ": " + topicTitle;
                    log.warn(format(ProcessorConstants.WARN_TOPIC_TITLES_NONMATCH_MSG, specTopic.getLineNumber(),
                            "Specified: " + specTopic.getTitle(), topicTitleMsg));
                    specTopic.setTitle(topicTitle);
                }
            }

            if (topicNode.getTopicType() == TopicType.INITIAL_CONTENT) {
                // Make sure the topics XML can be used as front matter content
                if (!validateInitialContentTopicXML(specTopic, topic)) {
                    valid = false;
                }
            }
        }

        if (topicNode.getTopicType() == TopicType.NORMAL || topicNode.getTopicType() == TopicType.FEEDBACK || topicNode.getTopicType() ==
                TopicType.INITIAL_CONTENT) {
            // Check to make sure the topic is a normal topic and not a special case
            if (!EntityUtilities.isANormalTopic(topic, serverEntities)) {
                log.error(format(ProcessorConstants.ERROR_TOPIC_NOT_ALLOWED_MSG, topicNode.getLineNumber(), topicNode.getText()));
                valid = false;
            }
        } else if (topicNode.getTopicType() == TopicType.LEGAL_NOTICE) {
            // Check to make sure the topic is a legal notice topic
            if (!topic.hasTag(serverEntities.getLegalNoticeTagId())) {
                log.error(
                        format(ProcessorConstants.ERROR_LEGAL_NOTICE_TOPIC_TYPE_INCORRECT, topicNode.getLineNumber(), topicNode.getText()));
                valid = false;
            }
        } else if (topicNode.getTopicType() == TopicType.REVISION_HISTORY) {
            // Check to make sure the topic is a revision history topic
            if (!topic.hasTag(serverEntities.getRevisionHistoryTagId())) {
                log.error(
                        format(ProcessorConstants.ERROR_REV_HISTORY_TOPIC_TYPE_INCORRECT, topicNode.getLineNumber(), topicNode.getText()));
                valid = false;
            }
        } else if (topicNode.getTopicType() == TopicType.AUTHOR_GROUP) {
            // Check to make sure the topic is an author group topic
            if (!topic.hasTag(serverEntities.getAuthorGroupTagId())) {
                log.error(
                        format(ProcessorConstants.ERROR_AUTHOR_GROUP_TOPIC_TYPE_INCORRECT, topicNode.getLineNumber(), topicNode.getText()));
                valid = false;
            }
        } else if (topicNode.getTopicType() == TopicType.ABSTRACT) {
            // Check to make sure the topic is an abstract topic
            if (!topic.hasTag(serverEntities.getAbstractTagId())) {
                log.error(format(ProcessorConstants.ERROR_ABSTRACT_TOPIC_TYPE_INCORRECT, topicNode.getLineNumber(), topicNode.getText()));
                valid = false;
            }
        } else if (topicNode.getTopicType() == TopicType.INFO) {
            // Check to make sure the topic is an info topic
            if (!topic.hasTag(serverEntities.getInfoTagId())) {
                log.error(format(ProcessorConstants.ERROR_INFO_TOPIC_TYPE_INCORRECT, topicNode.getLineNumber(), topicNode.getText()));
                valid = false;
            }
        }

        // Check the revision
        if (topicNode.getRevision() != null && !topicNode.getRevision().equals(topic.getRevision())) {
            log.warn(format(ProcessorConstants.WARN_REVISION_NOT_EXIST_USING_X_MSG, topicNode.getLineNumber(), topic.getRevision(),
                    topicNode.getText()));
        }

        // Check the docbook versions match
        if (contentSpec.getFormat().equalsIgnoreCase(CommonConstants.DOCBOOK_45_TITLE) && topic.getXmlFormat() != CommonConstants.DOCBOOK_45) {
            log.error(format(ProcessorConstants.ERROR_TOPIC_DOESNT_MATCH_FORMAT_MSG, topicNode.getLineNumber(), contentSpec.getFormat(),
                    topicNode.getText()));
            valid = false;
        } else if (contentSpec.getFormat().equalsIgnoreCase(CommonConstants.DOCBOOK_50_TITLE) && topic.getXmlFormat() != CommonConstants
                .DOCBOOK_50) {
            log.error(format(ProcessorConstants.ERROR_TOPIC_DOESNT_MATCH_FORMAT_MSG, topicNode.getLineNumber(), contentSpec.getFormat(),
                    topicNode.getText()));
            valid = false;
        }

        // Check the languages match
        final String locale = contentSpec.getLocale() == null ? defaultLocale : contentSpec.getLocale();
        if (locale != null && !locale.equals(topic.getLocale())) {
            log.error(format(ProcessorConstants.ERROR_TOPIC_DOESNT_MATCH_LOCALE_MSG, topicNode.getLineNumber(), topicNode.getText()));
            valid = false;
        }

        // Validate the tags
        if (!validateTopicTags(topicNode, topicNode.getTags(false))) {
            valid = false;
        } else if (!validateExistingTopicTags(topicNode, topic)) {
            valid = false;
        }

        return valid;
    }

    /**
     * Gets a Topics title with conditional statements applied
     *
     * @param specTopic The SpecTopic of the topic to get the title for.
     * @param topic     The actual topic to get the non-processed title from.
     * @return The processed title that has the conditions applied.
     */
    private String getTopicTitleWithConditions(final SpecTopic specTopic, final BaseTopicWrapper<?> topic) {
        final String condition = specTopic.getConditionStatement(true);
        if (condition != null) {
            try {
                final Document doc = XMLUtilities.convertStringToDocument("<title>" + topic.getTitle() + "</title>");

                // Process the condition on the title
                DocBookUtilities.processConditions(condition, doc);

                // Return the processed title
                return XMLUtilities.convertNodeToString(doc, false);
            } catch (Exception e) {
                log.debug(e.getMessage());
            }

            return topic.getTitle();
        } else {
            return topic.getTitle();
        }
    }

    /**
     * Checks a topics XML to make sure it has content that can be used as front matter for a level.
     *
     * @param specTopic The SpecTopic of the topic to check the XML for.
     * @param topic     The actual topic to check the XML from.
     * @return True if the XML can be used, otherwise false.
     */
    private boolean validateInitialContentTopicXML(final SpecTopic specTopic, final BaseTopicWrapper<?> topic) {
        boolean valid = true;
        final InitialContent initialContent = (InitialContent) specTopic.getParent();
        final int numSpecTopics = initialContent.getNumberOfSpecTopics();
        if (numSpecTopics >= 1) {
            final String condition = specTopic.getConditionStatement(true);
            try {
                final Document doc = XMLUtilities.convertStringToDocument(topic.getXml());

                // Process the conditions to remove anything that isn't used
                DocBookUtilities.processConditions(condition, doc);

                // Make sure no <simplesect> or <refentry> elements are used
                final List<org.w3c.dom.Node> invalidElements = XMLUtilities.getDirectChildNodes(doc.getDocumentElement(), "refentry",
                        "simplesect");
                // Make sure no <info> or <sectioninfo> elements aren't used if it's not the first element
                final List<org.w3c.dom.Node> invalidInfoElements = XMLUtilities.getDirectChildNodes(doc.getDocumentElement(), "info",
                        "sectioninfo");
                if ((numSpecTopics > 1 && invalidElements.size() > 0)) {
                    log.error(format(ProcessorConstants.ERROR_TOPIC_CANNOT_BE_USED_AS_INITIAL_CONTENT, specTopic.getLineNumber(),
                            specTopic.getText()));
                    valid = false;
                } else if ((initialContent.getFirstSpecNode() != specTopic && invalidInfoElements.size() > 0)
                        || (initialContent.getParent().getInfoTopic() != null && invalidInfoElements.size() > 0)) {
                    log.error(format(ProcessorConstants.ERROR_TOPIC_WITH_INFO_CANNOT_BE_USED_AS_INITIAL_CONTENT, specTopic.getLineNumber(),
                            specTopic.getText()));
                    valid = false;
                }
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
        }

        return valid;
    }

    /**
     * Checks to make sure that the assigned writer for the topic is valid.
     *
     * @param topic The topic to check the assigned writer for.
     * @return True if the assigned writer exists in the database and is under the Assigned Writer category otherwise
     *         false.
     */
    private boolean preValidateAssignedWriter(final ITopicNode topic) {
        if (topic.getAssignedWriter(true) == null) {
            log.error(String.format(ProcessorConstants.ERROR_NO_WRITER_MSG, topic.getLineNumber(), topic.getText()));
            return false;
        }

        return true;
    }

    /**
     * Checks to make sure that the assigned writer for the topic is valid.
     *
     * @param topic The topic to check the assigned writer for.
     * @return True if the assigned writer exists in the database and is under the Assigned Writer category otherwise
     *         false.
     */
    private boolean postValidateAssignedWriter(final ITopicNode topic) {
        // Check Assigned Writer exists
        TagWrapper tag = null;
        try {
            tag = tagProvider.getTagByName(topic.getAssignedWriter(true));
        } catch (NotFoundException e) {

        }
        if (tag == null) {
            log.error(String.format(ProcessorConstants.ERROR_WRITER_NONEXIST_MSG, topic.getLineNumber(), topic.getText()));
            return false;
        }

        // Check that the writer tag is actually part of the Assigned Writer category
        if (!tag.containedInCategory(serverEntities.getWriterCategoryId())) {
            log.error(String.format(ProcessorConstants.ERROR_INVALID_WRITER_MSG, topic.getLineNumber(), topic.getText()));
            return false;
        }

        return true;
    }

    /**
     * Checks to see if the tags are valid for a particular topic.
     *
     * @param optionsNode The topic or level the tags below to.
     * @param tagNames A list of all the tags in their string form to be validate.
     * @return True if the tags are valid otherwise false.
     */
    private boolean validateTopicTags(final IOptionsNode optionsNode, final List<String> tagNames) {
        boolean valid = true;
        if (!tagNames.isEmpty()) {
            final List<TagWrapper> tags = new ArrayList<TagWrapper>();
            for (final String tagName : tagNames) {
                // Check if the app should be shutdown
                if (isShuttingDown.get()) {
                    shutdown.set(true);
                    return false;
                }
                // Get the tag from the database
                TagWrapper tag = null;
                try {
                    tag = tagProvider.getTagByName(tagName);
                } catch (NotFoundException e) {

                }

                // Check that it exists
                if (tag != null) {
                    tags.add(tag);
                } else {
                    log.error(
                            String.format(ProcessorConstants.ERROR_TAG_NONEXIST_MSG, optionsNode.getLineNumber(), tagName, optionsNode.getText()));
                    valid = false;
                }
            }

            // Check that the mutex value entered is correct
            final Map<Integer, List<TagWrapper>> mapping = EntityUtilities.getCategoryMappingFromTagList(tags);
            for (final Entry<Integer, List<TagWrapper>> catEntry : mapping.entrySet()) {
                final CategoryWrapper cat = categoryProvider.getCategory(catEntry.getKey());
                final List<TagWrapper> catTags = catEntry.getValue();

                // Check if the app should be shutdown
                if (isShuttingDown.get()) {
                    shutdown.set(true);
                    return false;
                }

                // Check that only one tag has been set if the category is mutually exclusive
                if (cat.isMutuallyExclusive() && catTags.size() > 1) {
                    final String errorMsg;
                    if (optionsNode instanceof Level) {
                        final String baseErrorMsg = String.format(ProcessorConstants.ERROR_LEVEL_TOO_MANY_CATS_MSG,
                                ((Level) optionsNode).getLevelType().getTitle(), cat.getName(), optionsNode.getText());
                        if (((Level) optionsNode).getLevelType() == LevelType.BASE) {
                            errorMsg = baseErrorMsg;
                        } else {
                            errorMsg = String.format(ProcessorConstants.LINE, optionsNode.getLineNumber()) + baseErrorMsg;
                        }
                    } else {
                        errorMsg = String.format(ProcessorConstants.ERROR_TOPIC_TOO_MANY_CATS_MSG, optionsNode.getLineNumber(), cat.getName(),
                                optionsNode.getText());
                    }
                    log.error(errorMsg);
                    valid = false;
                }

                // Check that the tag isn't a type or writer
                if (cat.getId().equals(serverEntities.getWriterCategoryId())) {
                    final String errorMsg;
                    if (optionsNode instanceof Level) {
                        final String baseErrorMsg = String.format(ProcessorConstants.ERROR_LEVEL_WRITER_AS_TAG_MSG,
                                ((Level) optionsNode).getLevelType().getTitle(), optionsNode.getText());
                        if (((Level) optionsNode).getLevelType() == LevelType.BASE) {
                            errorMsg = baseErrorMsg;
                        } else {
                            errorMsg = String.format(ProcessorConstants.LINE, optionsNode.getLineNumber()) + baseErrorMsg;
                        }
                    } else {
                        errorMsg = String.format(ProcessorConstants.ERROR_TOPIC_WRITER_AS_TAG_MSG, optionsNode.getLineNumber(),
                                optionsNode.getText());
                    }
                    log.error(errorMsg);
                    valid = false;
                }

                // Check that the tag isn't a topic type
                if (cat.getId().equals(serverEntities.getTypeCategoryId())) {
                    final String errorMsg;
                    if (optionsNode instanceof Level) {
                        final String baseErrorMsg = String.format(ProcessorConstants.ERROR_LEVEL_TYPE_AS_TAG_MSG,
                                ((Level) optionsNode).getLevelType().getTitle(), optionsNode.getText());
                        if (((Level) optionsNode).getLevelType() == LevelType.BASE) {
                            errorMsg = baseErrorMsg;
                        } else {
                            errorMsg = String.format(ProcessorConstants.LINE, optionsNode.getLineNumber()) + baseErrorMsg;
                        }
                    } else {
                        errorMsg = String.format(ProcessorConstants.ERROR_TOPIC_TYPE_AS_TAG_MSG, optionsNode.getLineNumber(),
                                optionsNode.getText());
                    }
                    log.error(errorMsg);
                    valid = false;
                }
            }
        }
        return valid;
    }

    /**
     * Checks that adding tags to existing topic won't cause problems
     *
     * @param topicNode The topic the tags below to.
     * @return True if the tags are valid otherwise false.
     */
    private boolean validateExistingTopicTags(final ITopicNode topicNode, final BaseTopicWrapper<?> topic) {
        // Ignore validating revision topics
        if (topicNode.getRevision() != null) {
            return true;
        }

        boolean valid = true;
        final List<String> tagNames = topicNode.getTags(true);
        if (!tagNames.isEmpty()) {
            final Set<TagWrapper> tags = new HashSet<TagWrapper>();
            for (final String tagName : tagNames) {
                // Check if the app should be shutdown
                if (isShuttingDown.get()) {
                    shutdown.set(true);
                    return false;
                }
                // Get the tag from the database
                TagWrapper tag = null;
                try {
                    tag = tagProvider.getTagByName(tagName);
                } catch (NotFoundException e) {

                }

                // Check that it exists
                if (tag != null) {
                    tags.add(tag);
                }
            }

            // Add all the existing tags
            if (topic.getTags() != null) {
                tags.addAll(topic.getTags().getItems());
            }

            // Check that the mutex value entered is correct
            final Map<Integer, List<TagWrapper>> mapping = EntityUtilities.getCategoryMappingFromTagList(tags);
            for (final Entry<Integer, List<TagWrapper>> catEntry : mapping.entrySet()) {
                final CategoryWrapper cat = categoryProvider.getCategory(catEntry.getKey());
                final List<TagWrapper> catTags = catEntry.getValue();

                // Check if the app should be shutdown
                if (isShuttingDown.get()) {
                    shutdown.set(true);
                    return false;
                }

                // Check that only one tag has been set if the category is mutually exclusive
                if (cat.isMutuallyExclusive() && catTags.size() > 1) {
                    log.error(String.format(ProcessorConstants.ERROR_TOPIC_TOO_MANY_CATS_MSG, topicNode.getLineNumber(), cat.getName(),
                            topicNode.getText()));
                    valid = false;
                }
            }
        }
        return valid;
    }

    /**
     * Validate the Bug Links MetaData for a Content Specification without doing any external calls.
     *
     * @param contentSpec The Content Spec to validate.
     * @return True if the links values are valid, otherwise false.
     */
    public boolean preValidateBugLinks(final ContentSpec contentSpec) {
        // If Bug Links are turned off then there isn't any need to validate them.
        if (!contentSpec.isInjectBugLinks()) {
            return true;
        }

        final BugLinkOptions bugOptions;
        final BugLinkType type;
        if (contentSpec.getBugLinks().equals(BugLinkType.JIRA)) {
            type = BugLinkType.JIRA;
            bugOptions = contentSpec.getJIRABugLinkOptions();
        } else {
            type = BugLinkType.BUGZILLA;
            bugOptions = contentSpec.getBugzillaBugLinkOptions();
        }
        final BaseBugLinkStrategy bugLinkStrategy = BugLinkStrategyFactory.getInstance().create(type, bugOptions.getBaseUrl());

        // Validate the content in the bug options using the appropriate bug link strategy
        try {
            bugLinkStrategy.checkValidValues(bugOptions);
        } catch (ValidationException e) {
            final Throwable cause = ExceptionUtilities.getRootCause(e);
            log.error(cause.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Validate the Bug Links MetaData for a Content Specification.
     *
     * @param contentSpec The Content Spec to validate.
     * @param strict      If strict validation should be performed (invalid matches throws an error instead of a warning)
     * @return True if the links are valid, otherwise false.
     */
    public boolean postValidateBugLinks(final ContentSpec contentSpec, boolean strict) {
        // If Bug Links are turned off then there isn't any need to validate them.
        if (!contentSpec.isInjectBugLinks()) {
            return true;
        }

        try {
            final BugLinkOptions bugOptions;
            final BugLinkType type;
            if (contentSpec.getBugLinks().equals(BugLinkType.JIRA)) {
                type = BugLinkType.JIRA;
                bugOptions = contentSpec.getJIRABugLinkOptions();
            } else {
                type = BugLinkType.BUGZILLA;
                bugOptions = contentSpec.getBugzillaBugLinkOptions();
            }
            final BaseBugLinkStrategy bugLinkStrategy = BugLinkStrategyFactory.getInstance().create(type, bugOptions.getBaseUrl());

            // This step should have been performed by the preValidate method, so just make sure incase it hasn't been called.
            try {
                bugLinkStrategy.checkValidValues(bugOptions);
            } catch (ValidationException e) {
                return false;
            }

            // Validate the content in the bug options against the external service using the appropriate bug link strategy
            try {
                bugLinkStrategy.validate(bugOptions);
            } catch (ValidationException e) {
                final Throwable cause = ExceptionUtilities.getRootCause(e);
                if (strict) {
                    log.error(cause.getMessage());
                    return false;
                } else {
                    log.warn(cause.getMessage());
                }
            }

            return true;
        } catch (Exception e) {
            if (e instanceof ConnectException || e instanceof MalformedURLException) {
                if (strict) {
                    log.error(ProcessorConstants.ERROR_BUG_LINKS_UNABLE_TO_CONNECT);
                    return false;
                } else {
                    log.warn(ProcessorConstants.ERROR_BUG_LINKS_UNABLE_TO_CONNECT);
                }
            } else if (e.getCause() instanceof ConnectionException) {
                if (strict) {
                    log.error(ProcessorConstants.ERROR_BUGZILLA_UNABLE_TO_CONNECT);
                    return false;
                } else {
                    log.warn(ProcessorConstants.ERROR_BUGZILLA_UNABLE_TO_CONNECT);
                }
            } else {
                if (strict) {
                    log.error(ProcessorConstants.ERROR_BUG_LINKS_UNABLE_TO_VALIDATE);
                    log.error(e.toString());
                    return false;
                } else {
                    log.warn(ProcessorConstants.ERROR_BUG_LINKS_UNABLE_TO_VALIDATE);
                    log.warn(e.toString());
                }
            }
        }

        return true;
    }
}
