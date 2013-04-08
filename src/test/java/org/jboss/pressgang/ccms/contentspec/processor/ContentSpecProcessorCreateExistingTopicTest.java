package org.jboss.pressgang.ccms.contentspec.processor;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.constants.CSConstants;
import org.jboss.pressgang.ccms.contentspec.test.makers.shared.SpecTopicMaker;
import org.jboss.pressgang.ccms.wrapper.PropertyTagInTopicWrapper;
import org.jboss.pressgang.ccms.wrapper.PropertyTagWrapper;
import org.jboss.pressgang.ccms.wrapper.TagWrapper;
import org.jboss.pressgang.ccms.wrapper.TopicSourceURLWrapper;
import org.jboss.pressgang.ccms.wrapper.TopicWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.CollectionWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.UpdateableCollectionWrapper;
import org.jboss.pressgang.ccms.wrapper.mocks.CollectionWrapperMock;
import org.jboss.pressgang.ccms.wrapper.mocks.UpdateableCollectionWrapperMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ContentSpecProcessorCreateExistingTopicTest extends ContentSpecProcessorTest {
    @Arbitrary Integer id;
    @Arbitrary Integer revision;
    @Arbitrary String title;
    @Arbitrary String type;
    @Arbitrary String randomString;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String username;

    @Mock TopicWrapper topicWrapper;
    @Mock PropertyTagWrapper cspIdPropertyTag;
    @Mock PropertyTagWrapper addedByPropertyTag;
    @Mock PropertyTagInTopicWrapper cspIdPropertyTagInTopic;
    @Mock PropertyTagInTopicWrapper addedByPropertyTagInTopic;
    @Mock CollectionWrapper<TagWrapper> existingTagCollection;
    @Mock UpdateableCollectionWrapper<PropertyTagInTopicWrapper> existingTopicProperties;
    @Mock CollectionWrapper<TopicSourceURLWrapper> existingTopicSourceURLCollection;

    private CollectionWrapper<TagWrapper> tagCollection;
    private UpdateableCollectionWrapper<PropertyTagInTopicWrapper> propertyTagCollection;
    private CollectionWrapper<TopicSourceURLWrapper> topicSourceURLCollection;
    private List<PropertyTagInTopicWrapper> existingProperties;

    @Before
    public void setUpEntities() {
        tagCollection = new CollectionWrapperMock<TagWrapper>();
        propertyTagCollection = new UpdateableCollectionWrapperMock<PropertyTagInTopicWrapper>();
        topicSourceURLCollection = new CollectionWrapperMock<TopicSourceURLWrapper>();
        existingProperties = new ArrayList<PropertyTagInTopicWrapper>();
    }

    @Test
    public void shouldReturnNullWhenCreateSpecTopicAndUnchanged() {
        // Given a SpecTopic
        SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, id.toString()), with(SpecTopicMaker.uniqueId, "L-" + id),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.type, type), with(SpecTopicMaker.assignedWriter, username),
                        with(SpecTopicMaker.description, randomString)));
        // Setup the basic details
        setupBaseTopicMocks();
        // and setup the basic valid mocks
        setupValidBaseTopicMocks();

        TopicWrapper topic = null;
        try {
            topic = processor.createTopicEntity(providerFactory, specTopic);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Creating a topic should not have thrown an exception");
        }

        // Then the topic should be null
        assertNull(topic);
    }

    @Test
    public void shouldCreateSpecTopicAndUpdatePropertyWhenItAlreadyExists() {
        final String tagName = randomString;
        final TagWrapper tag1Wrapper = makeTag(tagName);
        // Given a SpecTopic and we add a tag just to make a change
        SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, id.toString()), with(SpecTopicMaker.uniqueId, "L-" + id),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.type, type), with(SpecTopicMaker.assignedWriter, username),
                        with(SpecTopicMaker.description, randomString), with(SpecTopicMaker.tags, Arrays.asList(randomString)),
                        with(SpecTopicMaker.revision, (Integer) null)));
        // Setup the basic details
        setupBaseTopicMocks();
        // and setup the basic valid mocks
        setupValidBaseTopicMocks();
        // and the csp id already exists
        existingProperties.add(cspIdPropertyTagInTopic);
        when(cspIdPropertyTagInTopic.getId()).thenReturn(CSConstants.CSP_PROPERTY_ID);
        // and the tag we are adding exist
        when(tagProvider.getTagByName(tagName)).thenReturn(tag1Wrapper);
        when(tag1Wrapper.getId()).thenReturn(1);

        TopicWrapper topic = null;
        try {
            topic = processor.createTopicEntity(providerFactory, specTopic);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Creating a topic should not have thrown an exception");
        }

        // Then the base topic should be valid
        verifyValidBaseTopic(topic);
        verifyUnchangedBaseTopicCollections();
        // and the property should have changed
        verifyValidBaseTopicUpdatedProperties();
    }

    @Test
    public void shouldCreateSpecTopicWithNewTags() {
        String tag1 = "Test";
        String tag2 = "Test2";
        final TagWrapper tag1Wrapper = makeTag(tag1);
        final TagWrapper tag2Wrapper = makeTag(tag2);
        // Given a list of tags
        final List<String> tags = Arrays.asList(tag1, tag2);
        // and a SpecTopic
        SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, id.toString()), with(SpecTopicMaker.uniqueId, "L-" + id),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.type, type), with(SpecTopicMaker.assignedWriter, username),
                        with(SpecTopicMaker.description, randomString), with(SpecTopicMaker.tags, tags),
                        with(SpecTopicMaker.revision, (Integer) null)));
        // Setup the basic details
        setupBaseTopicMocks();
        // and setup the basic valid mocks
        setupValidBaseTopicMocks();
        // and the tags exist
        when(tagProvider.getTagByName(tag1)).thenReturn(tag1Wrapper);
        when(tag1Wrapper.getId()).thenReturn(1);
        when(tagProvider.getTagByName(tag2)).thenReturn(tag2Wrapper);
        when(tag2Wrapper.getId()).thenReturn(2);

        TopicWrapper topic = null;
        try {
            topic = processor.createTopicEntity(providerFactory, specTopic);
        } catch (Exception e) {
            fail("Creating a topic should not have thrown an exception");
        }

        // Then the base topic should be valid
        verifyValidBaseTopic(topic);
        verifyValidBaseTopicNewProperties();
        verifyUnchangedBaseTopicCollections();
        // and the tags were added
        assertTrue(tagCollection.getAddItems().contains(tag1Wrapper));
        assertTrue(tagCollection.getAddItems().contains(tag2Wrapper));
    }

    @Test
    public void shouldCreateSpecTopicWithNewAndExistingTags() {
        String tag1 = "Test";
        String tag2 = "Test2";
        final TagWrapper tag1Wrapper = makeTag(tag1);
        final TagWrapper tag2Wrapper = makeTag(tag2);
        final TagWrapper existingTagWrapper = mock(TagWrapper.class);
        // Given a list of tags
        final List<String> tags = Arrays.asList(tag1, tag2);
        // and a SpecTopic
        SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, id.toString()), with(SpecTopicMaker.uniqueId, "L-" + id),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.type, type), with(SpecTopicMaker.assignedWriter, username),
                        with(SpecTopicMaker.description, randomString), with(SpecTopicMaker.tags, tags),
                        with(SpecTopicMaker.revision, (Integer) null)));
        // Setup the basic details
        setupBaseTopicMocks();
        // and setup the basic valid mocks
        setupValidBaseTopicMocks();
        // and the tags exist
        when(tagProvider.getTagByName(tag1)).thenReturn(tag1Wrapper);
        when(tag1Wrapper.getId()).thenReturn(1);
        when(tagProvider.getTagByName(tag2)).thenReturn(tag2Wrapper);
        when(tag2Wrapper.getId()).thenReturn(2);
        // and the topic already has some tags
        when(existingTagCollection.getItems()).thenReturn(Arrays.asList(existingTagWrapper));
        when(existingTagWrapper.getId()).thenReturn(3);

        TopicWrapper topic = null;
        try {
            topic = processor.createTopicEntity(providerFactory, specTopic);
        } catch (Exception e) {
            fail("Creating a topic should not have thrown an exception");
        }

        // Then the base topic should be valid
        verifyValidBaseTopic(topic);
        verifyValidBaseTopicNewProperties();
        verifyUnchangedBaseTopicCollections();
        // and the tags were added
        assertTrue(tagCollection.getAddItems().contains(tag1Wrapper));
        assertTrue(tagCollection.getAddItems().contains(tag2Wrapper));
        // and that the original tag exists
        assertTrue(tagCollection.getItems().contains(existingTagWrapper));
        assertThat(tagCollection.size(), is(3));
    }

    @Test
    public void shouldCreateSpecTopicWithExistingTagsAndAddTagThatAlreadyExists() {
        String tag1 = "Test";
        String tag2 = "Test2";
        final TagWrapper tag1Wrapper = makeTag(tag1);
        final TagWrapper tag2Wrapper = makeTag(tag2);
        // Given a list of tags
        final List<String> tags = Arrays.asList(tag1, tag2);
        // and a SpecTopic
        SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, id.toString()), with(SpecTopicMaker.uniqueId, "L-" + id),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.type, type), with(SpecTopicMaker.assignedWriter, username),
                        with(SpecTopicMaker.description, randomString), with(SpecTopicMaker.tags, tags),
                        with(SpecTopicMaker.revision, (Integer) null)));
        // Setup the basic details
        setupBaseTopicMocks();
        // and setup the basic valid mocks
        setupValidBaseTopicMocks();
        // and the tags exist
        when(tagProvider.getTagByName(tag1)).thenReturn(tag1Wrapper);
        when(tag1Wrapper.getId()).thenReturn(1);
        when(tagProvider.getTagByName(tag2)).thenReturn(tag2Wrapper);
        when(tag2Wrapper.getId()).thenReturn(2);
        // and the topic already has the tag1Wrapper in it's collection
        when(existingTagCollection.getItems()).thenReturn(Arrays.asList(tag1Wrapper));

        TopicWrapper topic = null;
        try {
            topic = processor.createTopicEntity(providerFactory, specTopic);
        } catch (Exception e) {
            fail("Creating a topic should not have thrown an exception");
        }

        // Then the base topic should be valid
        verifyValidBaseTopic(topic);
        verifyValidBaseTopicNewProperties();
        verifyUnchangedBaseTopicCollections();
        // and the new tags were added
        assertFalse(tagCollection.getAddItems().contains(tag1Wrapper));
        assertTrue(tagCollection.getAddItems().contains(tag2Wrapper));
        // and that the original tag exists
        assertTrue(tagCollection.getItems().contains(tag1Wrapper));
        assertThat(tagCollection.size(), is(2));

    }

    @Test
    public void shouldReturnNullWhenCreateSpecTopicWithExistingTagsAndAllAddTagsAlreadyExists() {
        String tag1 = "Test";
        String tag2 = "Test2";
        final TagWrapper tag1Wrapper = makeTag(tag1);
        final TagWrapper tag2Wrapper = makeTag(tag2);
        // Given a list of tags
        final List<String> tags = Arrays.asList(tag1, tag2);
        // and a SpecTopic
        SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, id.toString()), with(SpecTopicMaker.uniqueId, "L-" + id),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.type, type), with(SpecTopicMaker.assignedWriter, username),
                        with(SpecTopicMaker.description, randomString), with(SpecTopicMaker.tags, tags),
                        with(SpecTopicMaker.revision, (Integer) null)));
        // Setup the basic details
        setupBaseTopicMocks();
        // and setup the basic valid mocks
        setupValidBaseTopicMocks();
        // and the tags exist
        when(tagProvider.getTagByName(tag1)).thenReturn(tag1Wrapper);
        when(tag1Wrapper.getId()).thenReturn(1);
        when(tagProvider.getTagByName(tag2)).thenReturn(tag2Wrapper);
        when(tag2Wrapper.getId()).thenReturn(2);
        // and the topic already has the tag1Wrapper and tag2wrapper in it's collection
        when(existingTagCollection.getItems()).thenReturn(Arrays.asList(tag1Wrapper, tag2Wrapper));

        TopicWrapper topic = null;
        try {
            topic = processor.createTopicEntity(providerFactory, specTopic);
        } catch (Exception e) {
            fail("Creating a topic should not have thrown an exception");
        }

        // Then the topic should be null
        assertNull(topic);
        // and that the original tags weren't altered
        verify(topicWrapper, never()).setTags(any(CollectionWrapper.class));
        verify(existingTagCollection, never()).addItem(any(TagWrapper.class));
        verify(existingTagCollection, never()).addNewItem(any(TagWrapper.class));
        verify(existingTagCollection, never()).addRemoveItem(any(TagWrapper.class));
    }

    @Test
    public void shouldReturnNullWhenCreateSpecTopicWithTagsAndRevision() {
        String tag1 = "Test";
        String tag2 = "Test2";
        final TagWrapper tag1Wrapper = makeTag(tag1);
        final TagWrapper tag2Wrapper = makeTag(tag2);
        // Given a list of tags
        final List<String> tags = Arrays.asList(tag1, tag2);
        // and a SpecTopic
        SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, id.toString()), with(SpecTopicMaker.uniqueId, "L-" + id),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.type, type), with(SpecTopicMaker.assignedWriter, username),
                        with(SpecTopicMaker.description, randomString), with(SpecTopicMaker.tags, tags),
                        with(SpecTopicMaker.revision, revision)));
        // Setup the basic details
        setupBaseTopicMocks();
        // and setup the basic valid mocks
        setupValidBaseTopicMocks();
        // and the tags exist
        when(tagProvider.getTagByName(tag1)).thenReturn(tag1Wrapper);
        when(tagProvider.getTagByName(tag2)).thenReturn(tag2Wrapper);

        TopicWrapper topic = null;
        try {
            topic = processor.createTopicEntity(providerFactory, specTopic);
        } catch (Exception e) {
            fail("Creating a topic should not have thrown an exception");
        }

        // Then the topic should be unchanged and should be null
        assertNull(topic);
        verify(topicWrapper, never()).setTags(any(CollectionWrapper.class));
    }

    @Test
    public void shouldReturnNullWhenCreateSpecTopicWithAnySourceUrls() {
        String url1 = "http://www.example.com/";
        String url2 = "http://www.domain.com/";
        final TopicSourceURLWrapper urlWrapper = mock(TopicSourceURLWrapper.class);
        // Given a list of urls
        final List<String> urls = Arrays.asList(url1, url2);
        // and a SpecTopic with just an id and title
        SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, id.toString()), with(SpecTopicMaker.uniqueId, "L-" + id),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.type, type), with(SpecTopicMaker.assignedWriter, username),
                        with(SpecTopicMaker.description, randomString), with(SpecTopicMaker.urls, urls)));
        // Setup the basic details
        setupBaseTopicMocks();
        // and setup the basic valid mocks
        setupValidBaseTopicMocks();
        // and creating a new source url is setup
        when(topicSourceURLProvider.newTopicSourceURL(eq(topicWrapper))).thenReturn(urlWrapper);

        TopicWrapper topic = null;
        try {
            topic = processor.createTopicEntity(providerFactory, specTopic);
        } catch (Exception e) {
            fail("Creating a topic should not have thrown an exception");
        }

        // Then the topic should be unchanged and should be null
        assertNull(topic);
        verify(topicWrapper, never()).setSourceURLs(any(CollectionWrapper.class));
    }

    protected void setupBaseTopicMocks() {
        // and the topic provider will return an existing
        when(topicProvider.getTopic(anyInt(), anyInt())).thenReturn(topicWrapper);
        // and the tag provider will return a new tag collection
        when(tagProvider.newTagCollection()).thenReturn(tagCollection);
        when(topicWrapper.getTags()).thenReturn(existingTagCollection);
        // and the property tag provider will return a new property tag collection
        when(propertyTagProvider.newPropertyTagInTopicCollection(any(TopicWrapper.class))).thenReturn(propertyTagCollection);
        when(topicWrapper.getProperties()).thenReturn(existingTopicProperties);
        // and the property tag provider will return a property tag
        when(propertyTagProvider.getPropertyTag(CSConstants.CSP_PROPERTY_ID)).thenReturn(cspIdPropertyTag);
        // and the topic source url provider will return a new topic source url collection
        when(topicSourceURLProvider.newTopicSourceURLCollection(eq(topicWrapper))).thenReturn(topicSourceURLCollection);
        when(topicWrapper.getSourceURLs()).thenReturn(existingTopicSourceURLCollection);
        // and the property tag collection already has the added by tag
        existingProperties.add(addedByPropertyTagInTopic);
        when(existingTopicProperties.getItems()).thenReturn(existingProperties);
        when(addedByPropertyTagInTopic.getId()).thenReturn(CSConstants.ADDED_BY_PROPERTY_TAG_ID);
        when(addedByPropertyTagInTopic.getValue()).thenReturn(username);
    }

    protected void setupValidBaseTopicMocks() {
        final TagWrapper typeTag = makeTag(type);
        final TagWrapper writerTag = makeTag(username);
        // and the tag provider returns a type
        when(tagProvider.getTagByName(eq(type))).thenReturn(typeTag);
        // and the tag provider returns an assigned writer
        when(tagProvider.getTagByName(eq(username))).thenReturn(writerTag);
        // and the property tag provider creates a new property tag
        when(propertyTagProvider.newPropertyTagInTopic(eq(cspIdPropertyTag), any(TopicWrapper.class))).thenReturn(cspIdPropertyTagInTopic);
        when(propertyTagProvider.newPropertyTagInTopic(eq(addedByPropertyTag), any(TopicWrapper.class))).thenReturn(
                addedByPropertyTagInTopic);
    }

    protected void verifyValidBaseTopic(final TopicWrapper topic) {
        // Then check the topic isn't null
        assertNotNull(topic);
        // and the topic is the mocked topic
        assertThat(topic, is(topicWrapper));
        // and the topic title was not changed
        verify(topicWrapper, never()).setTitle(anyString());
        // and the id was not changed
        verify(topicWrapper, never()).setId(anyInt());
        // and the xml was not changed
        verify(topicWrapper, never()).setXml(anyString());
        // and the description was not changed
        verify(topicWrapper, never()).setDescription(anyString());
        // and the doctype was not changed
        verify(topicWrapper, never()).setXmlDoctype(anyInt());
        // and the locale was not changed
        verify(topicWrapper, never()).setLocale(anyString());
    }

    protected void verifyValidBaseTopicNewProperties() {
        // and the topic had the properties set
        verify(topicWrapper, times(1)).setProperties(eq(propertyTagCollection));
        // and the topic had the CSP property tag set
        assertTrue(propertyTagCollection.getAddItems().contains(cspIdPropertyTagInTopic));
        verify(cspIdPropertyTagInTopic, times(1)).setValue("L-" + id);
    }

    protected void verifyValidBaseTopicUpdatedProperties() {
        // and the topic had the properties set
        verify(topicWrapper, times(1)).setProperties(eq(propertyTagCollection));
        // and the topic had the CSP property tag set
        assertTrue(propertyTagCollection.getUpdateItems().contains(cspIdPropertyTagInTopic));
        verify(cspIdPropertyTagInTopic, times(1)).setValue("L-" + id);
    }

    protected void verifyUnchangedBaseTopicCollections() {
        assertTrue(propertyTagCollection.getItems().contains(cspIdPropertyTagInTopic));
        // and the topic still has the AddedBy property tag
        assertTrue(propertyTagCollection.getUnchangedItems().contains(addedByPropertyTagInTopic));
        // and the value hasn't changed
        verify(addedByPropertyTagInTopic, never()).setValue(anyString());
        // and the original property tag collection wasn't touched
        verify(existingTopicProperties, never()).addItem(any(PropertyTagInTopicWrapper.class));
        verify(existingTopicProperties, never()).addNewItem(any(PropertyTagInTopicWrapper.class));
        verify(existingTopicProperties, never()).addRemoveItem(any(PropertyTagInTopicWrapper.class));
        verify(existingTopicProperties, never()).addUpdateItem(any(PropertyTagInTopicWrapper.class));
        // and the original tag collection wasn't touched
        verify(existingTagCollection, never()).addItem(any(TagWrapper.class));
        verify(existingTagCollection, never()).addNewItem(any(TagWrapper.class));
        verify(existingTagCollection, never()).addRemoveItem(any(TagWrapper.class));
        // and the original topic source url collection wasn't touched
        verify(existingTopicSourceURLCollection, never()).addItem(any(TopicSourceURLWrapper.class));
        verify(existingTopicSourceURLCollection, never()).addNewItem(any(TopicSourceURLWrapper.class));
        verify(existingTopicSourceURLCollection, never()).addRemoveItem(any(TopicSourceURLWrapper.class));
    }
}
