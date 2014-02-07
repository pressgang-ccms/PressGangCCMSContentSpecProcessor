package org.jboss.pressgang.ccms.contentspec.processor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.jboss.pressgang.ccms.contentspec.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.processor.structures.ProcessingOptions;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLogger;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.provider.CSNodeProvider;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.provider.PropertyTagProvider;
import org.jboss.pressgang.ccms.provider.ServerSettingsProvider;
import org.jboss.pressgang.ccms.provider.TagProvider;
import org.jboss.pressgang.ccms.provider.TopicProvider;
import org.jboss.pressgang.ccms.provider.TopicSourceURLProvider;
import org.jboss.pressgang.ccms.wrapper.CSNodeWrapper;
import org.jboss.pressgang.ccms.wrapper.ServerEntitiesWrapper;
import org.jboss.pressgang.ccms.wrapper.ServerSettingsWrapper;
import org.jboss.pressgang.ccms.wrapper.TagWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.CollectionWrapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.mockito.Mock;
import org.powermock.modules.junit4.rule.PowerMockRule;

@Ignore
public class ContentSpecProcessorTest extends BaseUnitTest {
    protected static final String DOCBOOK_45 = "DocBook 4.5";
    protected static final String DOCBOOK_50 = "DocBook 4.5";
    protected static String DEFAULT_LOCALE = "en-US";
    protected static Integer ADDED_BY_PROPERTY_TAG_ID = 14;
    protected static Integer CSP_PROPERTY_ID = 15;
    protected static Integer WRITER_CATEGORY_ID = 12;

    @Rule public PowerMockRule rule = new PowerMockRule();

    @Mock DataProviderFactory providerFactory;
    @Mock ServerSettingsProvider serverSettingsProvider;
    @Mock ServerSettingsWrapper serverSettings;
    @Mock ServerEntitiesWrapper serverEntities;
    @Mock ErrorLoggerManager loggerManager;
    @Mock ProcessingOptions processingOptions;
    @Mock TopicProvider topicProvider;
    @Mock TopicSourceURLProvider topicSourceURLProvider;
    @Mock PropertyTagProvider propertyTagProvider;
    @Mock TagProvider tagProvider;
    @Mock ContentSpecProvider contentSpecProvider;
    @Mock CSNodeProvider contentSpecNodeProvider;

    protected ErrorLogger logger;
    protected ContentSpecProcessor processor;

    @Before
    public void setUp() {
        this.logger = new ErrorLogger("testLogger");
        when(loggerManager.getLogger(ContentSpecProcessor.class)).thenReturn(logger);

        when(providerFactory.getProvider(TopicProvider.class)).thenReturn(topicProvider);
        when(providerFactory.getProvider(TopicSourceURLProvider.class)).thenReturn(topicSourceURLProvider);
        when(providerFactory.getProvider(PropertyTagProvider.class)).thenReturn(propertyTagProvider);
        when(providerFactory.getProvider(TagProvider.class)).thenReturn(tagProvider);
        when(providerFactory.getProvider(ContentSpecProvider.class)).thenReturn(contentSpecProvider);
        when(providerFactory.getProvider(CSNodeProvider.class)).thenReturn(contentSpecNodeProvider);

        when(providerFactory.getProvider(ServerSettingsProvider.class)).thenReturn(serverSettingsProvider);
        when(serverSettingsProvider.getServerSettings()).thenReturn(serverSettings);
        when(serverSettings.getEntities()).thenReturn(serverEntities);
        when(serverSettings.getDefaultLocale()).thenReturn(DEFAULT_LOCALE);
        when(serverEntities.getCspIdPropertyTagId()).thenReturn(CSP_PROPERTY_ID);
        when(serverEntities.getAddedByPropertyTagId()).thenReturn(ADDED_BY_PROPERTY_TAG_ID);
        when(serverEntities.getWriterCategoryId()).thenReturn(WRITER_CATEGORY_ID);

        this.processor = new ContentSpecProcessor(providerFactory, loggerManager, processingOptions);
    }

    protected CollectionWrapper<TagWrapper> makeTagCollection(String tagName) {
        final TagWrapper tagWrapper = mock(TagWrapper.class);
        return makeTagCollection(tagName, tagWrapper);
    }

    protected TagWrapper makeTag(String tagName) {
        final TagWrapper tagWrapper = mock(TagWrapper.class);
        when(tagWrapper.getName()).thenReturn(tagName);
        return tagWrapper;
    }

    protected CollectionWrapper<TagWrapper> makeTagCollection(String tagName, TagWrapper tagWrapper) {
        final CollectionWrapper<TagWrapper> tagCollection = mock(CollectionWrapper.class);
        final List<TagWrapper> tagList = Arrays.asList(tagWrapper);

        when(tagCollection.getItems()).thenReturn(tagList);
        when(tagCollection.size()).thenReturn(tagList.size());

        when(tagWrapper.getName()).thenReturn(tagName);

        return tagCollection;
    }

    protected void setUpNodeToReturnNulls(final CSNodeWrapper nodeMock) {
        when(nodeMock.getRevision()).thenReturn(null);
        when(nodeMock.getAdditionalText()).thenReturn(null);
        when(nodeMock.getEntityRevision()).thenReturn(null);
        when(nodeMock.getEntityId()).thenReturn(null);
        when(nodeMock.getTitle()).thenReturn(null);
        when(nodeMock.getTargetId()).thenReturn(null);
        when(nodeMock.getNextNode()).thenReturn(null);
    }
}
