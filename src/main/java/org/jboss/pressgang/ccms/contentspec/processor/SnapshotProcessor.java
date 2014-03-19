package org.jboss.pressgang.ccms.contentspec.processor;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.KeyValueNode;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.Node;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.interfaces.ShutdownAbleApp;
import org.jboss.pressgang.ccms.contentspec.processor.structures.SnapshotOptions;
import org.jboss.pressgang.ccms.contentspec.utils.EntityUtilities;
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
                if (keyValueNode.getValue() != null && keyValueNode.getValue() instanceof SpecTopic) {
                    processTopic(contentSpec, (SpecTopic) keyValueNode.getValue(), processingOptions);
                }
            }
        }

        processLevel(contentSpec, contentSpec.getBaseLevel(), processingOptions);
    }

    /**
     *
     * @param contentSpec
     * @param level
     * @param processingOptions The set of processing options to be used when creating the snapshot.
     */
    protected void processLevel(final ContentSpec contentSpec, final Level level, final SnapshotOptions processingOptions) {
        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            shutdown.set(true);
            return;
        }

        // Validate the sub levels and topics
        for (final Node childNode : level.getChildNodes()) {
            if (childNode instanceof Level) {
                processLevel(contentSpec, (Level) childNode, processingOptions);
            } else if (childNode instanceof SpecTopic) {
                processTopic(contentSpec, (SpecTopic) childNode, processingOptions);
            }
        }
    }

    /**
     *
     * @param contentSpec
     * @param specTopic
     * @param processingOptions The set of processing options to be used when creating the snapshot.
     */
    protected void processTopic(final ContentSpec contentSpec, final SpecTopic specTopic, final SnapshotOptions processingOptions) {
        // Check if the app should be shutdown
        if (isShuttingDown.get()) {
            shutdown.set(true);
            return;
        }

        if (specTopic.isTopicAnExistingTopic()) {
            // Calculate the revision for the topic
            final Integer revision;
            if (specTopic.getRevision() == null || processingOptions.isUpdateRevisions()) {
                revision = processingOptions.getRevision();
            } else {
                revision = specTopic.getRevision();
            }

            // Check that the id actually exists
            BaseTopicWrapper<?> topic = null;
            try {
                if (processingOptions.isTranslation()) {
                    topic = EntityUtilities.getTranslatedTopicByTopicId(factory, Integer.parseInt(specTopic.getId()), revision,
                            processingOptions.getTranslationLocale() == null ? defaultLocale : processingOptions.getTranslationLocale());
                    if (processingOptions.isAddRevisions() && (specTopic.getRevision() == null || processingOptions.isUpdateRevisions())) {
                        specTopic.setRevision(topic.getTopicRevision());
                    }
                } else {
                    topic = topicProvider.getTopic(Integer.parseInt(specTopic.getId()), revision);
                    if (processingOptions.isAddRevisions() && (specTopic.getRevision() == null || processingOptions.isUpdateRevisions())) {
                        specTopic.setRevision(topic.getTopicRevision());
                    }
                }
            } catch (NotFoundException e) {
                log.debug("Could not find topic for id " + specTopic.getDBId());
            }
        }
    }
}
