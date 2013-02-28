package org.jboss.pressgang.ccms.contentspec.processor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.jboss.pressgang.ccms.contentspec.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.processor.structures.ProcessingOptions;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.provider.PropertyTagProvider;
import org.jboss.pressgang.ccms.contentspec.provider.TagProvider;
import org.jboss.pressgang.ccms.contentspec.provider.TopicProvider;
import org.jboss.pressgang.ccms.contentspec.provider.TopicSourceURLProvider;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLogger;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.contentspec.wrapper.TagWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.collection.CollectionWrapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.mockito.Mock;
import org.powermock.modules.junit4.rule.PowerMockRule;

@Ignore
public class ContentSpecProcessorTest extends BaseUnitTest {
    @Rule public PowerMockRule rule = new PowerMockRule();

    @Mock DataProviderFactory providerFactory;
    @Mock ErrorLoggerManager loggerManager;
    @Mock ProcessingOptions processingOptions;
    @Mock TopicProvider topicProvider;
    @Mock TopicSourceURLProvider topicSourceURLProvider;
    @Mock PropertyTagProvider propertyTagProvider;
    @Mock TagProvider tagProvider;

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

        this.processor = new ContentSpecProcessor(providerFactory, loggerManager, processingOptions);
    }

    protected CollectionWrapper<TagWrapper> makeTagCollection(String tagName) {
        final TagWrapper tagWrapper = mock(TagWrapper.class);
        return makeTagCollection(tagName, tagWrapper);
    }

    protected CollectionWrapper<TagWrapper> makeTagCollection(String tagName, TagWrapper tagWrapper) {
        final CollectionWrapper<TagWrapper> tagCollection = mock(CollectionWrapper.class);
        final List<TagWrapper> tagList = Arrays.asList(tagWrapper);

        when(tagCollection.getItems()).thenReturn(tagList);
        when(tagCollection.size()).thenReturn(tagList.size());

        when(tagWrapper.getName()).thenReturn(tagName);

        return tagCollection;
    }
}
