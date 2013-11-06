package org.jboss.pressgang.ccms.contentspec.processor;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
import org.jboss.pressgang.ccms.contentspec.KeyValueNode;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.Node;
import org.jboss.pressgang.ccms.contentspec.Process;
import org.jboss.pressgang.ccms.contentspec.SpecNode;
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
import org.jboss.pressgang.ccms.contentspec.processor.utils.ProcessorUtilities;
import org.jboss.pressgang.ccms.contentspec.sort.NullNumberSort;
import org.jboss.pressgang.ccms.contentspec.sort.SpecTopicLineNumberComparator;
import org.jboss.pressgang.ccms.contentspec.utils.ContentSpecUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.EntityUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLogger;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.provider.BlobConstantProvider;
import org.jboss.pressgang.ccms.provider.CategoryProvider;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.provider.FileProvider;
import org.jboss.pressgang.ccms.provider.TagProvider;
import org.jboss.pressgang.ccms.provider.TextContentSpecProvider;
import org.jboss.pressgang.ccms.provider.TopicProvider;
import org.jboss.pressgang.ccms.provider.exception.NotFoundException;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.ExceptionUtilities;
import org.jboss.pressgang.ccms.utils.common.HashUtilities;
import org.jboss.pressgang.ccms.utils.common.SAXXMLValidator;
import org.jboss.pressgang.ccms.utils.common.XMLUtilities;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;
import org.jboss.pressgang.ccms.wrapper.BlobConstantWrapper;
import org.jboss.pressgang.ccms.wrapper.CategoryWrapper;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.FileWrapper;
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

    private String locale;

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
        locale = CommonConstants.DEFAULT_LOCALE;
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
        locale = contentSpec.getLocale() == null ? locale : contentSpec.getLocale();

        // Create the map of unique ids to spec topics
        final Map<String, SpecTopic> specTopicMap = ContentSpecUtilities.getUniqueIdSpecTopicMap(contentSpec);

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

        if (isNullOrEmpty(contentSpec.getDtd())) {
            log.error(ProcessorConstants.ERROR_CS_NO_DTD_MSG);
            valid = false;
            // Check that the DTD specified is a valid DTD format
        } else if (!contentSpec.getDtd().equalsIgnoreCase("Docbook 4.5")) {
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

        // Print Warnings for content that maybe important
        if (isNullOrEmpty(contentSpec.getSubtitle())) {
            log.warn(ProcessorConstants.WARN_CS_NO_SUBTITLE_MSG);
        }
        if (isNullOrEmpty(contentSpec.getAbstract())) {
            log.warn(ProcessorConstants.WARN_CS_NO_ABSTRACT_MSG);
        } else {
            // Check to make sure the abstract is at least valid XML
            final String wrappedAbstract = "<para>" + contentSpec.getAbstract() + "</para>";
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
        if (!preValidateLevel(contentSpec.getBaseLevel(), specTopicMap, contentSpec.getAllowEmptyLevels(), contentSpec.getBookType(),
                contentSpec)) {
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

        // reset the locale back to its default
        locale = CommonConstants.DEFAULT_LOCALE;

        return valid;
    }

    /**
     * Check if the condition on a node will conflict with a condition in the defined publican.cfg file.
     *
     * @param node        The node to be checked.
     * @param contentSpec The content spec the node belongs to.
     */
    protected void checkForConflictingCondition(final SpecNode node, final ContentSpec contentSpec) {
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

        // Check that no custom entities are defined.
        if (doc != null) {
            final List<String> invalidEntities = new ArrayList<String>();
            final List<String> validEntities = ProcessorUtilities.loadValidXMLEntities(factory);
            final NamedNodeMap entityNodes = doc.getDoctype().getEntities();
            for (int i = 0; i < entityNodes.getLength(); i++) {
                final org.w3c.dom.Node entityNode = entityNodes.item(i);
                if (!validEntities.contains(entityNode.getNodeName())) {
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
                    log.error(String.format(ProcessorConstants.ERROR_CUSTOM_ENTITIES_SINGLE_DEFINED_MSG, builder.toString(), line));
                } else {
                    log.error(String.format(ProcessorConstants.ERROR_CUSTOM_ENTITIES_DEFINED_MSG, builder.toString(), line));
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
    private boolean checkTopicsForInvalidDuplicates(final ContentSpec contentSpec) {
        boolean valid = true;

        // Find all Topics that have two or more different revisions
        final List<SpecTopic> allSpecTopics = contentSpec.getSpecTopics();
        final Map<Integer, Map<Integer, Set<SpecTopic>>> invalidSpecTopics = new HashMap<Integer, Map<Integer, Set<SpecTopic>>>();

        for (final SpecTopic specTopic1 : allSpecTopics) {
            if (!specTopic1.isTopicAnExistingTopic()) continue;

            for (final SpecTopic specTopic2 : allSpecTopics) {
                // If the Topic isn't an existing topic and doesn't match the first spec topic's id, then continue
                if (specTopic1 == specTopic2 || !specTopic2.isTopicAnExistingTopic() || !specTopic1.getDBId().equals(specTopic2.getDBId()))
                    continue;

                // Check if the revisions between the two topics are the same
                if (specTopic1.getRevision() == null && specTopic2.getRevision() != null || specTopic1.getRevision() != null &&
                        specTopic2.getRevision() == null || specTopic1.getRevision() != null && !specTopic1.getRevision().equals(
                        specTopic2.getRevision())) {
                    if (!invalidSpecTopics.containsKey(specTopic1.getDBId())) {
                        invalidSpecTopics.put(specTopic1.getDBId(), new HashMap<Integer, Set<SpecTopic>>());
                    }

                    final Map<Integer, Set<SpecTopic>> revisionsToSpecTopic = invalidSpecTopics.get(specTopic1.getDBId());
                    if (!revisionsToSpecTopic.containsKey(specTopic1.getRevision())) {
                        revisionsToSpecTopic.put(specTopic1.getRevision(), new HashSet<SpecTopic>());
                    }

                    revisionsToSpecTopic.get(specTopic1.getRevision()).add(specTopic1);

                    valid = false;
                }
            }
        }

        // Loop through and generate an error message for each invalid topic
        for (final Entry<Integer, Map<Integer, Set<SpecTopic>>> entry : invalidSpecTopics.entrySet()) {
            final Integer topicId = entry.getKey();
            final Map<Integer, Set<SpecTopic>> revisionsToSpecTopic = entry.getValue();

            final List<String> revNumbers = new ArrayList<String>();
            final List<Integer> revisions = new ArrayList<Integer>(revisionsToSpecTopic.keySet());
            Collections.sort(revisions, new NullNumberSort<Integer>());

            for (final Integer revision : revisions) {
                final List<SpecTopic> specTopics = new ArrayList<SpecTopic>(revisionsToSpecTopic.get(revision));

                // Build up the line numbers message
                final StringBuilder lineNumbers = new StringBuilder();
                if (specTopics.size() > 1) {
                    // Sort the Topics by line numbers
                    Collections.sort(specTopics, new SpecTopicLineNumberComparator());

                    for (int i = 0; i < specTopics.size(); i++) {
                        if (i == specTopics.size() - 1) {
                            lineNumbers.append(" and ");
                        } else if (lineNumbers.length() != 0) {
                            lineNumbers.append(", ");
                        }

                        lineNumbers.append(specTopics.get(i).getLineNumber());
                    }
                } else if (specTopics.size() == 1) {
                    lineNumbers.append(specTopics.get(0).getLineNumber());
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
        locale = contentSpec.getLocale() == null ? locale : contentSpec.getLocale();

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
                if (contentSpecEntity.getProperty(CSConstants.CSP_READ_ONLY_PROPERTY_TAG_ID) != null) {
                    if (!contentSpecEntity.getProperty(CSConstants.CSP_READ_ONLY_PROPERTY_TAG_ID).getValue().matches(
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
                    if (!tag.containedInCategory(CSConstants.TYPE_CATEGORY_ID)) {
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
        if (contentSpec.getRevisionHistory() != null && !postValidateTopic(contentSpec.getRevisionHistory())) {
            valid = false;
        }
        if (contentSpec.getFeedback() != null && !postValidateTopic(contentSpec.getFeedback())) {
            valid = false;
        }
        if (contentSpec.getLegalNotice() != null && !postValidateTopic(contentSpec.getLegalNotice())) {
            valid = false;
        }
        if (contentSpec.getAuthorGroup() != null && !postValidateTopic(contentSpec.getAuthorGroup())) {
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
            final String wrappedAbstract = "<para>" + contentSpec.getAbstract() + "</para>";

            // Get the docbook DTD
            final BlobConstantWrapper rocbookDtd = blobConstantProvider.getBlobConstant(CommonConstants.ROCBOOK_DTD_BLOB_ID);

            // Validate the XML content against the dtd
            final SAXXMLValidator validator = new SAXXMLValidator(false);
            if (!validator.validateXML(wrappedAbstract, "rocbook.dtd", rocbookDtd.getValue(), "para")) {
                valid = false;
                final String line = CommonConstants.CS_ABSTRACT_TITLE + " = " + contentSpec.getAbstract();
                log.error(String.format(ProcessorConstants.ERROR_INVALID_ABSTRACT_MSG, validator.getErrorText(), line));
            }
        }

        // Check that each level is valid
        if (!postValidateLevel(contentSpec.getBaseLevel())) {
            valid = false;
        }

        // reset the locale back to its default
        locale = CommonConstants.DEFAULT_LOCALE;

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
                } else {
                    // Make sure the titles sync up.
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
        final Map<String, List<SpecTopic>> specTopicMap = ContentSpecUtilities.getIdSpecTopicMap(contentSpec);

        final Map<SpecTopic, List<Relationship>> relationships = contentSpec.getRelationships();
        for (final Entry<SpecTopic, List<Relationship>> relationshipEntry : relationships.entrySet()) {
            final SpecTopic specTopic = relationshipEntry.getKey();

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

                final String relatedId = relationship.getSecondaryRelationshipTopicId();
                // The relationship points to a target so it must be a level or topic
                if (relationship instanceof TargetRelationship) {
                    final SpecNode node = ((TargetRelationship) relationship).getSecondaryRelationship();
                    if (node instanceof SpecTopic) {
                        final SpecTopic targetTopic = (SpecTopic) node;
                        if (!validateTopicRelationship(relationship, specTopic, relatedId, targetTopic)) {
                            error = true;
                        }
                    } else if (node instanceof Level) {
                        final Level targetLevel = (Level) node;
                        if (relationship.getType() == RelationshipType.NEXT) {
                            log.error(String.format(ProcessorConstants.ERROR_NEXT_RELATED_LEVEL_MSG, specTopic.getLineNumber(),
                                    specTopic.getText()));
                            error = true;
                        } else if (relationship.getType() == RelationshipType.PREVIOUS) {
                            log.error(String.format(ProcessorConstants.ERROR_PREV_RELATED_LEVEL_MSG, specTopic.getLineNumber(),
                                    specTopic.getText()));
                            error = true;
                        } else if (relationship.getRelationshipTitle() != null && !relationship.getRelationshipTitle().equals(
                                targetLevel.getTitle())) {
                            if (processingOptions.isStrictTitles()) {
                                log.error(String.format(ProcessorConstants.ERROR_RELATED_TITLE_NO_MATCH_MSG, specTopic.getLineNumber(),
                                        relationship.getRelationshipTitle(), targetLevel.getTitle()));
                                error = true;
                            } else {
                                log.warn(String.format(ProcessorConstants.WARN_RELATED_TITLE_NO_MATCH_MSG, specTopic.getLineNumber(),
                                        relationship.getRelationshipTitle(), targetLevel.getTitle()));
                            }
                        }
                    }
                } else if (relationship instanceof TopicRelationship) {
                    final SpecTopic relatedTopic = ((TopicRelationship) relationship).getSecondaryRelationship();
                    final List<SpecTopic> relatedSpecTopics = specTopicMap.get(relatedTopic.getId());
                    if (relatedId.startsWith("X")) {
                        // Duplicated topics are never unique so throw an error straight away.
                        log.error(String.format(ProcessorConstants.ERROR_INVALID_DUPLICATE_RELATIONSHIP_MSG, specTopic.getLineNumber(),
                                specTopic.getText()));
                        error = true;
                    } else if (relatedSpecTopics == null) {
                        log.error(String.format(ProcessorConstants.ERROR_RELATED_TOPIC_NONEXIST_MSG, specTopic.getLineNumber(), relatedId,
                                specTopic.getText()));
                        error = true;
                    } else if (relatedSpecTopics.size() > 1) {
                        // Check to make sure the topic isn't duplicated
                        final List<SpecTopic> relatedTopics = specTopicMap.get(relatedTopic.getId());

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

                        log.error(String.format(ProcessorConstants.ERROR_INVALID_RELATIONSHIP_MSG, specTopic.getLineNumber(), relatedId,
                                lineNumbers.toString(), specTopic.getText()));
                        error = true;
                    } else {
                        if (!validateTopicRelationship(relationship, specTopic, relatedId, relatedTopic)) {
                            error = true;
                        }
                    }
                }
            }
        }
        return !error;
    }

    /**
     * @param relationship
     * @param specTopic
     * @param relatedId
     * @param relatedTopic
     * @return True if the topic relationship is valid, otherwise false.
     */
    private boolean validateTopicRelationship(final Relationship relationship, final SpecTopic specTopic, final String relatedId,
            final SpecTopic relatedTopic) {
        if (relatedTopic.getDBId() != null && relatedTopic.getDBId() < 0) {
            log.error(String.format(ProcessorConstants.ERROR_RELATED_TOPIC_NONEXIST_MSG, specTopic.getLineNumber(), relatedId,
                    specTopic.getText()));
            return false;
        } else if (relatedTopic == specTopic) {
            // Check to make sure the topic doesn't relate to itself
            log.error(String.format(ProcessorConstants.ERROR_TOPIC_RELATED_TO_ITSELF_MSG, specTopic.getLineNumber(), specTopic.getText()));
            return false;
        } else {
            final String relatedTitle = TopicType.LEVEL.equals(
                    relatedTopic.getTopicType()) && relatedTopic.getParent() instanceof Level ? ((Level) relatedTopic.getParent())
                    .getTitle() : relatedTopic.getTitle();
            if (relationship.getRelationshipTitle() != null && !relationship.getRelationshipTitle().equals(relatedTitle)) {
                if (processingOptions.isStrictTitles()) {
                    log.error(String.format(ProcessorConstants.ERROR_RELATED_TITLE_NO_MATCH_MSG, specTopic.getLineNumber(),
                            relationship.getRelationshipTitle(), relatedTitle));
                    return false;
                } else {
                    log.warn(String.format(ProcessorConstants.WARN_RELATED_TITLE_NO_MATCH_MSG, specTopic.getLineNumber(),
                            relationship.getRelationshipTitle(), relatedTitle));
                }
            }
        }

        return true;
    }

    /**
     * Validates a level to ensure its format and child levels/topics are valid.
     *
     * @param level              The level to be validated.
     * @param specTopics         The list of topics that exist within the content specification.
     * @param csAllowEmptyLevels If the "Allow Empty Levels" bit is set in a content specification.
     * @param bookType           The type of book the level should be validated for.
     * @param contentSpec        The content spec the level belongs to.
     * @return True if the level is valid otherwise false.
     */
    public boolean preValidateLevel(final Level level, final Map<String, SpecTopic> specTopics, final boolean csAllowEmptyLevels,
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
        if (levelType != LevelType.PART && level.getNumberOfSpecTopics() <= 0 && level.getNumberOfChildLevels() <= 0 /*
                                                                                       * && !allowEmptyLevels &&
                                                                                       * (allowEmptyLevels &&
                                                                                       * !csAllowEmptyLevels)
                                                                                       */) {
            // Check to make sure an inner topic doesn't exist, unless its a section level as in that case the section should just be a
            // normal topic
            if (levelType == LevelType.SECTION || level.getInnerTopic() == null) {
                log.error(format(ProcessorConstants.ERROR_LEVEL_NO_TOPICS_MSG, level.getLineNumber(), levelType.getTitle(),
                        levelType.getTitle(), level.getText()));
                valid = false;
            }
        } else if (levelType == LevelType.PART && level.getNumberOfChildLevels() <= 0) {
            log.error(format(ProcessorConstants.ERROR_LEVEL_NO_CHILD_LEVELS_MSG, level.getLineNumber(), levelType.getTitle(),
                    levelType.getTitle(), level.getText()));
            valid = false;
        }

        if (isNullOrEmpty(level.getTitle())) {
            log.error(String.format(ProcessorConstants.ERROR_LEVEL_NO_TITLE_MSG, level.getLineNumber(), levelType.getTitle(),
                    level.getText()));
            valid = false;
        }

        // Validate the topics level
        if (level.getInnerTopic() != null && !preValidateTopic(level.getInnerTopic(), specTopics, bookType, contentSpec)) {
            valid = false;
        }

        // Validate the sub levels and topics
        for (final Node childNode : level.getChildNodes()) {
            if (childNode instanceof Level) {
                if (!preValidateLevel((Level) childNode, specTopics, csAllowEmptyLevels, bookType, contentSpec)) {
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

                    /* Check that the appendix is at the end of the article */
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

                    /* Check that the appendix is at the end of the book */
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
                    Process process = (Process) level;
                    if (process.getNumberOfChildLevels() != 0) {
                        log.error(format(ProcessorConstants.ERROR_PROCESS_HAS_LEVELS_MSG, process.getLineNumber(), process.getText()));
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
                default:
                    break;
            }
        }

        // Validate the tags
        if (!level.getTags(false).isEmpty() && level.hasRevisionSpecTopics()) {
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
     * @param level The level to be validated.
     * @return True if the level is valid otherwise false.
     */
    public boolean postValidateLevel(final Level level) {
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

        // Validate the topics level
        if (level.getInnerTopic() != null && !postValidateTopic(level.getInnerTopic())) {
            valid = false;
        }

        // Validate the sub levels and topics
        for (final Node childNode : level.getChildNodes()) {
            if (childNode instanceof Level) {
                if (!postValidateLevel((Level) childNode)) {
                    valid = false;
                }
            } else if (childNode instanceof SpecTopic) {
                if (!postValidateTopic((SpecTopic) childNode)) {
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

        // Check that the topic exists in the spec by checking it's step
        if (specTopic.getStep() == 0) {
            log.error(ProcessorConstants.ERROR_PROCESSING_ERROR_MSG);
            valid = false;
        }

        // Checks that the id isn't null and is a valid topic ID
        if (specTopic.getId() == null || !specTopic.getId().matches(CSConstants.ALL_TOPIC_ID_REGEX)) {
            log.error(String.format(ProcessorConstants.ERROR_INVALID_TOPIC_ID_MSG, specTopic.getLineNumber(), specTopic.getText()));
            valid = false;
        }

        if ((bookType == BookType.BOOK || bookType == BookType.BOOK_DRAFT) && specTopic.getParent() instanceof Level) {
            final Level parent = (Level) specTopic.getParent();
            // Check that the topic is inside a chapter/section/process/appendix/part/preface
            final LevelType parentLevelType = parent.getLevelType();
            if (parent == null || !(parentLevelType == LevelType.CHAPTER || parentLevelType == LevelType.APPENDIX ||
                    parentLevelType == LevelType.PROCESS || parentLevelType == LevelType.SECTION || parentLevelType == LevelType.PART ||
                    parentLevelType == LevelType.PREFACE)) {
                log.error(format(ProcessorConstants.ERROR_TOPIC_OUTSIDE_CHAPTER_MSG, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            }

            // Check that there are no levels in the parent part (ie the topic is in the intro)
            if (parent != null && parentLevelType == LevelType.PART) {
                final LinkedList<Node> parentChildren = parent.getChildNodes();
                final int index = parentChildren.indexOf(specTopic);

                for (int i = 0; i < index; i++) {
                    final Node node = parentChildren.get(i);
                    if (node instanceof Level) {
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
        // Check that it is valid when escaped
        else if (DocBookUtilities.escapeTitle(specTopic.getTitle()).isEmpty()) {
            log.error(String.format(ProcessorConstants.ERROR_INVALID_TOPIC_TITLE_MSG, specTopic.getLineNumber(), specTopic.getText()));
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
            if (specTopic.getRevision() != null && !specTopic.getTags(false).isEmpty()) {
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
     * @param specTopic The topic to be validated.
     * @return True if the topic is valid otherwise false.
     */
    @SuppressWarnings("unchecked")
    public boolean postValidateTopic(final SpecTopic specTopic) {
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

            if (type == null || !type.containedInCategory(CSConstants.TYPE_CATEGORY_ID)) {
                log.error(String.format(ProcessorConstants.ERROR_TYPE_NONEXIST_MSG, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            } else if (specTopic.getTopicType() == TopicType.LEGAL_NOTICE && !type.getId().equals(CSConstants.LEGAL_NOTICE_TAG_ID)) {
                log.error(format(ProcessorConstants.ERROR_INVALID_TYPE_MSG, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            } else if (specTopic.getTopicType() == TopicType.REVISION_HISTORY && !type.getId().equals(
                    CSConstants.REVISION_HISTORY_TAG_ID)) {
                log.error(format(ProcessorConstants.ERROR_INVALID_TYPE_MSG, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            } else if (specTopic.getTopicType() == TopicType.AUTHOR_GROUP && !type.getId().equals(CSConstants.AUTHOR_GROUP_TAG_ID)) {
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
                    topic = EntityUtilities.getTranslatedTopicByTopicId(factory, Integer.parseInt(specTopic.getId()), revision, locale);
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
                if (topic.hasTag(CSConstants.RH_INTERNAL_TAG_ID)) {
                    log.warn(String.format(ProcessorConstants.WARN_INTERNAL_TOPIC_MSG, specTopic.getLineNumber(), specTopic.getText()));
                }

                if (!postValidateExistingTopic(specTopic, topic)) {
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
                if (!postValidateExistingTopic(specTopic, topic)) {
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

    private boolean postValidateExistingTopic(final SpecTopic specTopic, final BaseTopicWrapper<?> topic) {
        boolean valid = true;

        if (specTopic.getTopicType() == TopicType.NORMAL) {
            // Validate the title matches for normal topics
            final String topicTitle = getTopicTitleWithConditions(specTopic, topic);
            if (!specTopic.getTitle().equals(topicTitle)) {
                if (processingOptions.isStrictTitles()) {
                    final String topicTitleMsg = "Topic " + specTopic.getId() + ": " + topicTitle;
                    log.error(format(ProcessorConstants.ERROR_TOPIC_TITLES_NONMATCH_MSG, specTopic.getLineNumber(),
                            "Specified: " + specTopic.getTitle(), topicTitleMsg));
                    valid = false;
                } else {
                    final String topicTitleMsg = "Topic " + specTopic.getId() + ": " + topicTitle;
                    log.warn(format(ProcessorConstants.WARN_TOPIC_TITLES_NONMATCH_MSG, specTopic.getLineNumber(),
                            "Specified: " + specTopic.getTitle(), topicTitleMsg));
                    specTopic.setTitle(topicTitle);
                }
            }
        } else if (specTopic.getTopicType() == TopicType.LEVEL) {
            // Validate the title matches for inner level topics
            final Level parent = (Level) specTopic.getParent();
            final String topicTitle = getTopicTitleWithConditions(specTopic, topic);
            if (!specTopic.getTitle().equals(topicTitle)) {
                // Add the warning message
                String topicTitleMsg = "Topic " + specTopic.getId() + ": " + topicTitle;
                if (processingOptions.isStrictTitles()) {
                    log.error(format(ProcessorConstants.ERROR_LEVEL_TOPIC_TITLES_NONMATCH_MSG, specTopic.getLineNumber(),
                            parent.getLevelType().getTitle(), "Specified: " + specTopic.getTitle(), topicTitleMsg));
                    valid = false;
                } else {
                    log.warn(format(ProcessorConstants.WARN_LEVEL_TOPIC_TITLES_NONMATCH_MSG, specTopic.getLineNumber(),
                            parent.getLevelType().getTitle(), "Specified: " + specTopic.getTitle(), topicTitleMsg));

                    // Change the title
                    specTopic.setTitle(topicTitle);
                }
            }
        }

        if (specTopic.getTopicType() == TopicType.NORMAL || specTopic.getTopicType() == TopicType.FEEDBACK || specTopic.getTopicType() ==
                TopicType.LEVEL) {
            // Check to make sure the topic is a normal topic and not a special case
            if (topic.hasTag(CSConstants.LEGAL_NOTICE_TAG_ID) || topic.hasTag(CSConstants.REVISION_HISTORY_TAG_ID) || topic.hasTag(
                    CSConstants.AUTHOR_GROUP_TAG_ID)) {
                log.error(format(ProcessorConstants.ERROR_TOPIC_NOT_ALLOWED_MSG, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            }
        } else if (specTopic.getTopicType() == TopicType.LEGAL_NOTICE) {
            // Check to make sure the topic is a legal notice topic
            if (!topic.hasTag(CSConstants.LEGAL_NOTICE_TAG_ID)) {
                log.error(
                        format(ProcessorConstants.ERROR_LEGAL_NOTICE_TOPIC_TYPE_INCORRECT, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            }
        } else if (specTopic.getTopicType() == TopicType.REVISION_HISTORY) {
            // Check to make sure the topic is a revision history topic
            if (!topic.hasTag(CSConstants.REVISION_HISTORY_TAG_ID)) {
                log.error(
                        format(ProcessorConstants.ERROR_REV_HISTORY_TOPIC_TYPE_INCORRECT, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            }
        } else if (specTopic.getTopicType() == TopicType.AUTHOR_GROUP) {
            // Check to make sure the topic is a author group topic
            if (!topic.hasTag(CSConstants.AUTHOR_GROUP_TAG_ID)) {
                log.error(
                        format(ProcessorConstants.ERROR_AUTHOR_GROUP_TOPIC_TYPE_INCORRECT, specTopic.getLineNumber(), specTopic.getText()));
                valid = false;
            }
        }

        // Check the revision
        if (specTopic.getRevision() != null && !specTopic.getRevision().equals(topic.getRevision())) {
            log.warn(format(ProcessorConstants.WARN_REVISION_NOT_EXIST_USING_X_MSG, specTopic.getLineNumber(), topic.getRevision(),
                    specTopic.getText()));
        }

        // Validate the tags
        if (!validateTopicTags(specTopic, specTopic.getTags(false))) {
            valid = false;
        } else if (!validateExistingTopicTags(specTopic, topic)) {
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
     * Checks to make sure that the assigned writer for the topic is valid.
     *
     * @param topic The topic to check the assigned writer for.
     * @return True if the assigned writer exists in the database and is under the Assigned Writer category otherwise
     *         false.
     */
    private boolean preValidateAssignedWriter(final SpecTopic topic) {
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
    private boolean postValidateAssignedWriter(final SpecTopic topic) {

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
        if (!tag.containedInCategory(CSConstants.WRITER_CATEGORY_ID)) {
            log.error(String.format(ProcessorConstants.ERROR_INVALID_WRITER_MSG, topic.getLineNumber(), topic.getText()));
            return false;
        }

        return true;
    }

    /**
     * Checks to see if the tags are valid for a particular topic.
     *
     * @param specNode The topic or level the tags below to.
     * @param tagNames A list of all the tags in their string form to be validate.
     * @return True if the tags are valid otherwise false.
     */
    private boolean validateTopicTags(final SpecNode specNode, final List<String> tagNames) {
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
                    log.error(String.format(ProcessorConstants.ERROR_TAG_NONEXIST_MSG, specNode.getLineNumber(), tagName,
                            specNode.getText()));
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
                    if (specNode instanceof Level) {
                        final String baseErrorMsg = String.format(ProcessorConstants.ERROR_LEVEL_TOO_MANY_CATS_MSG,
                                ((Level) specNode).getLevelType().getTitle(), cat.getName(), specNode.getText());
                        if (((Level) specNode).getLevelType() == LevelType.BASE) {
                            errorMsg = baseErrorMsg;
                        } else {
                            errorMsg = String.format(ProcessorConstants.LINE, specNode.getLineNumber()) + baseErrorMsg;
                        }
                    } else {
                        errorMsg = String.format(ProcessorConstants.ERROR_TOPIC_TOO_MANY_CATS_MSG, specNode.getLineNumber(), cat.getName(),
                                specNode.getText());
                    }
                    log.error(errorMsg);
                    valid = false;
                }

                // Check that the tag isn't a type or writer
                if (cat.getId().equals(CSConstants.WRITER_CATEGORY_ID)) {
                    final String errorMsg;
                    if (specNode instanceof Level) {
                        final String baseErrorMsg = String.format(ProcessorConstants.ERROR_LEVEL_WRITER_AS_TAG_MSG,
                                ((Level) specNode).getLevelType().getTitle(), specNode.getText());
                        if (((Level) specNode).getLevelType() == LevelType.BASE) {
                            errorMsg = baseErrorMsg;
                        } else {
                            errorMsg = String.format(ProcessorConstants.LINE, specNode.getLineNumber()) + baseErrorMsg;
                        }
                    } else {
                        errorMsg = String.format(ProcessorConstants.ERROR_TOPIC_WRITER_AS_TAG_MSG, specNode.getLineNumber(),
                                specNode.getText());
                    }
                    log.error(errorMsg);
                    valid = false;
                }

                // Check that the tag isn't a topic type
                if (cat.getId().equals(CSConstants.TYPE_CATEGORY_ID)) {
                    final String errorMsg;
                    if (specNode instanceof Level) {
                        final String baseErrorMsg = String.format(ProcessorConstants.ERROR_LEVEL_TYPE_AS_TAG_MSG,
                                ((Level) specNode).getLevelType().getTitle(), specNode.getText());
                        if (((Level) specNode).getLevelType() == LevelType.BASE) {
                            errorMsg = baseErrorMsg;
                        } else {
                            errorMsg = String.format(ProcessorConstants.LINE, specNode.getLineNumber()) + baseErrorMsg;
                        }
                    } else {
                        errorMsg = String.format(ProcessorConstants.ERROR_TOPIC_TYPE_AS_TAG_MSG, specNode.getLineNumber(),
                                specNode.getText());
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
     * @param specTopic The topic the tags below to.
     * @return True if the tags are valid otherwise false.
     */
    private boolean validateExistingTopicTags(final SpecTopic specTopic, final BaseTopicWrapper<?> topic) {
        // Ignore validating revision topics
        if (specTopic.getRevision() != null) {
            return true;
        }

        boolean valid = true;
        final List<String> tagNames = specTopic.getTags(true);
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
                    log.error(String.format(ProcessorConstants.ERROR_TOPIC_TOO_MANY_CATS_MSG, specTopic.getLineNumber(), cat.getName(),
                            specTopic.getText()));
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
