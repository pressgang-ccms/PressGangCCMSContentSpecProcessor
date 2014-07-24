package org.jboss.pressgang.ccms.contentspec.processor;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.ITopicNode;
import org.jboss.pressgang.ccms.contentspec.KeyValueNode;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.Node;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.interfaces.ShutdownAbleApp;
import org.jboss.pressgang.ccms.contentspec.processor.structures.SnapshotOptions;
import org.jboss.pressgang.ccms.contentspec.utils.EntityUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.FixedURLGenerator;
import org.jboss.pressgang.ccms.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.provider.ServerSettingsProvider;
import org.jboss.pressgang.ccms.provider.TopicProvider;
import org.jboss.pressgang.ccms.provider.exception.NotFoundException;
import org.jboss.pressgang.ccms.wrapper.ServerSettingsWrapper;
import org.jboss.pressgang.ccms.wrapper.base.BaseTopicWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnapshotProcessor implements ShutdownAbleApp {
    private static Logger log = LoggerFactory.getLogger(SnapshotProcessor.class);

    private final DataProviderFactory factory;
    private final TopicProvider topicProvider;
    private final ServerSettingsWrapper serverSettings;
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
     */
    public SnapshotProcessor(final DataProviderFactory factory) {
        this.factory = factory;
        topicProvider = factory.getProvider(TopicProvider.class);

        serverSettings = factory.getProvider(ServerSettingsProvider.class).getServerSettings();
        defaultLocale = serverSettings.getDefaultLocale();
    }

    /**
     *
     * @param contentSpec
     * @param processingOptions The set of processing options to be used when creating the snapshot.
     */
    public void processContentSpec(final ContentSpec contentSpec, final SnapshotOptions processingOptions) {
        // Process the metadata for any spec topic metadata
        for (final Node node : contentSpec.getNodes()) {
            if (node instanceof KeyValueNode) {
                final KeyValueNode keyValueNode = ((KeyValueNode) node);
                if (keyValueNode.getValue() != null && keyValueNode.getValue() instanceof ITopicNode) {
                    processTopic((ITopicNode) keyValueNode.getValue(), processingOptions);
                }
            }
        }

        // Process the levels
        processLevel(contentSpec.getBaseLevel(), processingOptions);

        // Set the fixed urls for the content spec
        if (processingOptions.isAddFixedUrls()) {
            FixedURLGenerator.generateFixedUrls(contentSpec, true, serverSettings.getEntities().getFixedUrlPropertyTagId());
        }
    }

    /**
     *
     * @param level
     * @param processingOptions The set of processing options to be used when creating the snapshot.
     */
    protected void processLevel(final Level level, final SnapshotOptions processingOptions) {
        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            shutdown.set(true);
            return;
        }

        // Process the info topic
        if (level.getInfoTopic() != null) {
            processTopic(level.getInfoTopic(), processingOptions);
        }

        // Validate the sub levels and topics
        for (final Node childNode : level.getChildNodes()) {
            if (childNode instanceof Level) {
                processLevel((Level) childNode, processingOptions);
            } else if (childNode instanceof SpecTopic) {
                processTopic((SpecTopic) childNode, processingOptions);
            }
        }
    }

    /**
     *
     * @param topicNode
     * @param processingOptions The set of processing options to be used when creating the snapshot.
     */
    protected void processTopic(final ITopicNode topicNode, final SnapshotOptions processingOptions) {
        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            shutdown.set(true);
            return;
        }

        // Find the revision to use
        final Integer revision;
        if (topicNode.getRevision() == null || processingOptions.isUpdateRevisions()) {
            revision = processingOptions.getRevision();
        } else {
            revision = topicNode.getRevision();
        }

        // Look up the topic
        BaseTopicWrapper<?> topic = null;
        try {
            if (processingOptions.isTranslation()) {
                topic = EntityUtilities.getTranslatedTopicByTopicId(factory, Integer.parseInt(topicNode.getId()), revision,
                        processingOptions.getTranslationLocale() == null ? defaultLocale : processingOptions.getTranslationLocale());
            } else {
                topic = topicProvider.getTopic(Integer.parseInt(topicNode.getId()), revision);
            }
            topicNode.setTopic(topic);
        } catch (NotFoundException e) {
            log.debug("Could not find topic for id " + topicNode.getDBId());
            throw e;
        }

        if (!processingOptions.isAddRevisions()) {
            // If we aren't adding revisions then we have nothing to do here, so just return
            return;
        } else if (!(topicNode.getRevision() == null || processingOptions.isUpdateRevisions())) {
            // If the topic already has a revision and we aren't updating it, then just return
            return;
        } else if (topicNode.isTopicAnExistingTopic()) {
            topicNode.setRevision(topic.getTopicRevision());
        }
    }
}
