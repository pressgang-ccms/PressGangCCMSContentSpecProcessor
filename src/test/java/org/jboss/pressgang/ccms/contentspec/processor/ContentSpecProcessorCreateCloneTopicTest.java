package org.jboss.pressgang.ccms.contentspec.processor;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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
import org.jboss.pressgang.ccms.contentspec.test.makers.shared.SpecTopicMaker;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;
import org.jboss.pressgang.ccms.wrapper.PropertyTagInTopicWrapper;
import org.jboss.pressgang.ccms.wrapper.PropertyTagWrapper;
import org.jboss.pressgang.ccms.wrapper.TagWrapper;
import org.jboss.pressgang.ccms.wrapper.TopicSourceURLWrapper;
import org.jboss.pressgang.ccms.wrapper.TopicWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.CollectionWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.UpdateableCollectionWrapper;
import org.jboss.pressgang.ccms.wrapper.mocks.CollectionWrapperMock;
import org.jboss.pressgang.ccms.wrapper.mocks.TopicWrapperMock;
import org.jboss.pressgang.ccms.wrapper.mocks.UpdateableCollectionWrapperMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ContentSpecProcessorCreateCloneTopicTest extends ContentSpecProcessorTest {
    @Arbitrary Integer id;
    @Arbitrary Integer revision;
    @Arbitrary String title;
    @Arbitrary String type;
    @Arbitrary String randomString;
    @Arbitrary String locale;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String username;

    @Mock PropertyTagWrapper cspIdPropertyTag;
    @Mock PropertyTagWrapper addedByPropertyTag;
    @Mock PropertyTagInTopicWrapper cspIdPropertyTagInTopic;
    @Mock PropertyTagInTopicWrapper addedByPropertyTagInTopic;
    @Mock PropertyTagInTopicWrapper propertyTagInTopic;
    @Mock TagWrapper writerTag;

    // Existing topic mocks
    @Mock TopicWrapper topicWrapper;
    @Mock PropertyTagInTopicWrapper existingCspIdPropertyTagInTopic;
    @Mock PropertyTagInTopicWrapper existingAddedByPropertyTagInTopic;
    @Mock UpdateableCollectionWrapper<PropertyTagInTopicWrapper> existingTopicProperties;
    @Mock UpdateableCollectionWrapper<TopicSourceURLWrapper> existingTopicSourceURLCollection;
    @Mock CollectionWrapper<TagWrapper> existingTagCollection;

    private TopicWrapper newTopicWrapper;
    private CollectionWrapper<TagWrapper> tagCollection;
    private UpdateableCollectionWrapper<PropertyTagInTopicWrapper> propertyTagCollection;
    private UpdateableCollectionWrapper<TopicSourceURLWrapper> topicSourceURLCollection;
    private List<PropertyTagInTopicWrapper> existingProperties;

    @Before
    public void setUpEntities() {
        newTopicWrapper = new TopicWrapperMock();
        tagCollection = new CollectionWrapperMock<TagWrapper>();
        propertyTagCollection = new UpdateableCollectionWrapperMock<PropertyTagInTopicWrapper>();
        topicSourceURLCollection = new UpdateableCollectionWrapperMock<TopicSourceURLWrapper>();
        existingProperties = new ArrayList<PropertyTagInTopicWrapper>();
    }

    @Test
    public void shouldCreateSpecTopic() {
        // Given a SpecTopic with just an id and title
        SpecTopic specTopic = make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, "C1"), with(SpecTopicMaker.uniqueId, "L-C1"),
                with(SpecTopicMaker.title, title), with(SpecTopicMaker.type, type), with(SpecTopicMaker.assignedWriter, username),
                with(SpecTopicMaker.description, randomString)));
        // Setup the basic details
        setupBaseTopicMocks();
        // Setup the existing topic mocks
        setupExistingTopicMocks();
        // and setup the basic valid mocks
        setupValidBaseTopicMocks();

        TopicWrapper topic = null;
        try {
            topic = processor.createTopicEntity(providerFactory, specTopic, DOCBOOK_45);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Creating a topic should not have thrown an exception");
        }

        // Then the base topic should be valid
        verifyValidBaseTopic(topic);
        verifyUnchangedOriginalTopic();
        // and the property should have added
        verifyValidBaseTopicNewProperties(topic);
    }

    @Test
    public void shouldCreateSpecTopicAndSetNewPropertyWhenItAlreadyExistsInOriginalTopic() {
        // Given a SpecTopic and we add a tag just to make a change
        SpecTopic specTopic = make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, "C1"), with(SpecTopicMaker.uniqueId, "L-C1"),
                with(SpecTopicMaker.title, title), with(SpecTopicMaker.type, type), with(SpecTopicMaker.assignedWriter, username),
                with(SpecTopicMaker.description, randomString), with(SpecTopicMaker.revision, (Integer) null)));
        // Setup the basic details
        setupBaseTopicMocks();
        // Setup the existing topic mocks
        setupExistingTopicMocks();
        // and setup the basic valid mocks
        setupValidBaseTopicMocks();
        // and the csp id already exists
        existingProperties.add(existingCspIdPropertyTagInTopic);
        when(existingCspIdPropertyTagInTopic.getId()).thenReturn(CSP_PROPERTY_ID);

        TopicWrapper topic = null;
        try {
            topic = processor.createTopicEntity(providerFactory, specTopic, DOCBOOK_45);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Creating a topic should not have thrown an exception");
        }

        // Then the base topic should be valid
        verifyValidBaseTopic(topic);
        verifyUnchangedOriginalTopic();
        // and the property should have added
        verifyValidBaseTopicNewProperties(topic);
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
        SpecTopic specTopic = make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, "C1"), with(SpecTopicMaker.uniqueId, "L-C1"),
                with(SpecTopicMaker.title, title), with(SpecTopicMaker.type, type), with(SpecTopicMaker.assignedWriter, username),
                with(SpecTopicMaker.description, randomString), with(SpecTopicMaker.tags, tags),
                with(SpecTopicMaker.revision, (Integer) null)));
        // Setup the basic details
        setupBaseTopicMocks();
        // Setup the existing topic mocks
        setupExistingTopicMocks();
        // and setup the basic valid mocks
        setupValidBaseTopicMocks();
        // and the tags exist
        when(tagProvider.getTagByName(tag1)).thenReturn(tag1Wrapper);
        when(tag1Wrapper.getId()).thenReturn(1);
        when(tagProvider.getTagByName(tag2)).thenReturn(tag2Wrapper);
        when(tag2Wrapper.getId()).thenReturn(2);

        TopicWrapper topic = null;
        try {
            topic = processor.createTopicEntity(providerFactory, specTopic, DOCBOOK_45);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Creating a topic should not have thrown an exception");
        }

        // Then the base topic should be valid
        verifyValidBaseTopic(topic);
        verifyValidBaseTopicNewProperties(topic);
        verifyUnchangedOriginalTopic();
        // and the tags were added
        assertTrue(tagCollection.getAddItems().contains(tag1Wrapper));
        assertTrue(tagCollection.getAddItems().contains(tag2Wrapper));
        // and only new tags were set, as clones shouldn't have existing tags
        assertThat(tagCollection.getItems().size(), is(3));
        assertThat(tagCollection.getUnchangedItems().size(), is(0));
        assertThat(tagCollection.getRemoveItems().size(), is(0));
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
        SpecTopic specTopic = make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, "C1"), with(SpecTopicMaker.uniqueId, "L-C1"),
                with(SpecTopicMaker.title, title), with(SpecTopicMaker.type, type), with(SpecTopicMaker.assignedWriter, username),
                with(SpecTopicMaker.description, randomString), with(SpecTopicMaker.tags, tags),
                with(SpecTopicMaker.revision, (Integer) null)));
        // Setup the basic details
        setupBaseTopicMocks();
        // Setup the existing topic mocks
        setupExistingTopicMocks();
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
            topic = processor.createTopicEntity(providerFactory, specTopic, DOCBOOK_45);
        } catch (Exception e) {
            fail("Creating a topic should not have thrown an exception");
        }

        // Then the base topic should be valid
        verifyValidBaseTopic(topic);
        verifyValidBaseTopicNewProperties(topic);
        verifyUnchangedOriginalTopic();
        // and the tags were added
        assertTrue(tagCollection.getAddItems().contains(tag1Wrapper));
        assertTrue(tagCollection.getAddItems().contains(tag2Wrapper));
        // and that the original tag was added as well
        assertTrue(tagCollection.getAddItems().contains(existingTagWrapper));
        // and only new tags were set, as clones shouldn't have existing tags
        assertThat(tagCollection.getItems().size(), is(4));
        assertThat(tagCollection.getUnchangedItems().size(), is(0));
        assertThat(tagCollection.getRemoveItems().size(), is(0));
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
        SpecTopic specTopic = make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, "C1"), with(SpecTopicMaker.uniqueId, "L-C1"),
                with(SpecTopicMaker.title, title), with(SpecTopicMaker.type, type), with(SpecTopicMaker.assignedWriter, username),
                with(SpecTopicMaker.description, randomString), with(SpecTopicMaker.tags, tags),
                with(SpecTopicMaker.revision, (Integer) null)));
        // Setup the basic details
        setupBaseTopicMocks();
        // Setup the existing topic mocks
        setupExistingTopicMocks();
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
            topic = processor.createTopicEntity(providerFactory, specTopic, DOCBOOK_45);
        } catch (Exception e) {
            fail("Creating a topic should not have thrown an exception");
        }

        // Then the base topic should be valid
        verifyValidBaseTopic(topic);
        verifyValidBaseTopicNewProperties(topic);
        verifyUnchangedOriginalTopic();
        // and the new tags were added
        assertTrue(tagCollection.getAddItems().contains(tag2Wrapper));
        // and that the original tag was added as well
        assertTrue(tagCollection.getAddItems().contains(tag1Wrapper));
        // and only new tags were set, as clones shouldn't have existing tags
        assertThat(tagCollection.getItems().size(), is(3));
        assertThat(tagCollection.getUnchangedItems().size(), is(0));
        assertThat(tagCollection.getRemoveItems().size(), is(0));
    }

    @Test
    public void shouldCreateSpecTopicWithExistingTagsAndAllAddTagsAlreadyExists() {
        String tag1 = "Test";
        String tag2 = "Test2";
        final TagWrapper tag1Wrapper = makeTag(tag1);
        final TagWrapper tag2Wrapper = makeTag(tag2);
        // Given a list of tags
        final List<String> tags = Arrays.asList(tag1, tag2);
        // and a SpecTopic
        SpecTopic specTopic = make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, "C1"), with(SpecTopicMaker.uniqueId, "L-C1"),
                with(SpecTopicMaker.title, title), with(SpecTopicMaker.type, type), with(SpecTopicMaker.assignedWriter, username),
                with(SpecTopicMaker.description, randomString), with(SpecTopicMaker.tags, tags),
                with(SpecTopicMaker.revision, (Integer) null)));
        // Setup the basic details
        setupBaseTopicMocks();
        // Setup the existing topic mocks
        setupExistingTopicMocks();
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
            topic = processor.createTopicEntity(providerFactory, specTopic, DOCBOOK_45);
        } catch (Exception e) {
            fail("Creating a topic should not have thrown an exception");
        }

        // Then the base topic should be valid
        verifyValidBaseTopic(topic);
        verifyValidBaseTopicNewProperties(topic);
        verifyUnchangedOriginalTopic();
        // and the tags should be the same as the original
        assertThat(tagCollection.size(), is(3));
        // and the tags exist in the new state in the collection
        assertTrue(tagCollection.getAddItems().contains(tag1Wrapper));
        assertTrue(tagCollection.getAddItems().contains(tag2Wrapper));
        // and only new tags were set, as clones shouldn't have existing tags
        assertThat(tagCollection.getUnchangedItems().size(), is(0));
        assertThat(tagCollection.getRemoveItems().size(), is(0));
    }

    @Test
    public void shouldCreateSpecTopicWithTagsRemoved() {
        String removeTag = "Test";
        final TagWrapper tag1Wrapper = makeTag(removeTag);
        final TagWrapper existingTagWrapper = mock(TagWrapper.class);
        final TagWrapper existingTagWrapper2 = mock(TagWrapper.class);
        // Given a list of tags to be removed
        final List<String> tags = Arrays.asList(removeTag);
        // and a SpecTopic
        SpecTopic specTopic = make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, "C1"), with(SpecTopicMaker.uniqueId, "L-C1"),
                with(SpecTopicMaker.title, title), with(SpecTopicMaker.type, type), with(SpecTopicMaker.assignedWriter, username),
                with(SpecTopicMaker.description, randomString), with(SpecTopicMaker.removeTags, tags),
                with(SpecTopicMaker.revision, (Integer) null)));
        // Setup the basic details
        setupBaseTopicMocks();
        // Setup the existing topic mocks
        setupExistingTopicMocks();
        // and setup the basic valid mocks
        setupValidBaseTopicMocks();
        // and the tags exist
        when(tagProvider.getTagByName(removeTag)).thenReturn(tag1Wrapper);
        when(tag1Wrapper.getId()).thenReturn(1);
        // and the existing tags have an id
        when(existingTagWrapper.getId()).thenReturn(1);
        when(existingTagWrapper2.getId()).thenReturn(2);
        // and the topic already has the existing wrappers in it's collection
        when(existingTagCollection.getItems()).thenReturn(Arrays.asList(existingTagWrapper, existingTagWrapper2));

        TopicWrapper topic = null;
        try {
            topic = processor.createTopicEntity(providerFactory, specTopic, DOCBOOK_45);
        } catch (Exception e) {
            fail("Creating a topic should not have thrown an exception");
        }

        // Then the base topic should be valid
        verifyValidBaseTopic(topic);
        verifyValidBaseTopicNewProperties(topic);
        verifyUnchangedOriginalTopic();
        // and the tag was removed
        assertTrue(tagCollection.getRemoveItems().contains(existingTagWrapper));
        // and that the other tag was added as well
        assertTrue(tagCollection.getAddItems().contains(existingTagWrapper2));
        // and a tag was removed and the other two tags were added
        assertThat(tagCollection.getItems().size(), is(3));
        assertThat(tagCollection.getUnchangedItems().size(), is(0));
        assertThat(tagCollection.getAddItems().size(), is(2));
        assertThat(tagCollection.getRemoveItems().size(), is(1));
    }

    @Test
    public void shouldCreateSpecTopicWithWriterThatAlreadyExists() {
        String writerName = randomString;
        final TagWrapper writerTagWrapper = mock(TagWrapper.class);

        // and a SpecTopic
        SpecTopic specTopic = make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, "C1"), with(SpecTopicMaker.uniqueId, "L-C1"),
                with(SpecTopicMaker.title, title), with(SpecTopicMaker.type, type), with(SpecTopicMaker.assignedWriter, username),
                with(SpecTopicMaker.description, randomString), with(SpecTopicMaker.revision, (Integer) null)));
        // Setup the basic details
        setupBaseTopicMocks();
        // Setup the existing topic mocks
        setupExistingTopicMocks();
        // and setup the basic valid mocks
        setupValidBaseTopicMocks();
        // and a writer tag already exists
        when(writerTagWrapper.getId()).thenReturn(id);
        when(writerTag.getId()).thenReturn(id);
        // and the existing writer tag is in the writer category
        when(writerTagWrapper.containedInCategory(WRITER_CATEGORY_ID)).thenReturn(true);
        // and the topic already has the writerTag in it's collection
        when(existingTagCollection.getItems()).thenReturn(Arrays.asList(writerTagWrapper));

        TopicWrapper topic = null;
        try {
            topic = processor.createTopicEntity(providerFactory, specTopic, DOCBOOK_45);
        } catch (Exception e) {
            fail("Creating a topic should not have thrown an exception");
        }

        // Then the base topic should be valid
        verifyValidBaseTopic(topic);
        verifyValidBaseTopicNewProperties(topic);
        verifyUnchangedOriginalTopic();
        // and the old writer tag isn't in the collection
        assertFalse(tagCollection.getItems().contains(writerTagWrapper));
        // and that the new writer tag was added
        assertTrue(tagCollection.getAddItems().contains(writerTag));
        // and only new tags were set, as clones shouldn't have existing tags
        assertThat(tagCollection.getItems().size(), is(1));
        assertThat(tagCollection.getUnchangedItems().size(), is(0));
        assertThat(tagCollection.getRemoveItems().size(), is(0));
    }

    @Test
    public void shouldCreateSpecTopicWithSourceUrls() {
        String url1 = "http://www.example.com/";
        String url2 = "http://www.domain.com/";
        final TopicSourceURLWrapper urlWrapper = mock(TopicSourceURLWrapper.class);
        final TopicSourceURLWrapper urlWrapper2 = mock(TopicSourceURLWrapper.class);
        // Given a list of urls
        final List<String> urls = Arrays.asList(url1, url2);
        // and a SpecTopic with just an id and title
        SpecTopic specTopic = make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, "C1"), with(SpecTopicMaker.uniqueId, "L-C1"),
                with(SpecTopicMaker.title, title), with(SpecTopicMaker.type, type), with(SpecTopicMaker.assignedWriter, username),
                with(SpecTopicMaker.description, randomString), with(SpecTopicMaker.urls, urls)));
        // Setup the basic details
        setupBaseTopicMocks();
        // Setup the existing topic mocks
        setupExistingTopicMocks();
        // and setup the basic valid mocks
        setupValidBaseTopicMocks();
        // and creating a new source url is setup
        when(topicSourceURLProvider.newTopicSourceURL(eq(newTopicWrapper))).thenReturn(urlWrapper, urlWrapper2);

        TopicWrapper topic = null;
        try {
            topic = processor.createTopicEntity(providerFactory, specTopic, DOCBOOK_45);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Creating a topic should not have thrown an exception");
        }

        // Then the base topic should be valid
        verifyValidBaseTopic(topic);
        verifyValidBaseTopicNewProperties(topic);
        verifyUnchangedOriginalTopic();
        // and the source url collection will have the two source url objects.
        assertThat(topicSourceURLCollection.size(), is(2));
        // and the urls exist in the new state in the collection
        assertTrue(topicSourceURLCollection.getAddItems().contains(urlWrapper));
        assertTrue(topicSourceURLCollection.getAddItems().contains(urlWrapper2));
        // check that a url was set for each url
        verify(urlWrapper, times(1)).setUrl(eq(url1));
        verify(urlWrapper2, times(1)).setUrl(eq(url2));
        // and only new source urls were set, as clones shouldn't have existing tags
        assertThat(topicSourceURLCollection.getRemoveItems().size(), is(0));
        assertThat(topicSourceURLCollection.getUnchangedItems().size(), is(0));
    }

    protected void setupExistingTopicMocks() {
        // and the topic has an id
        when(topicWrapper.getId()).thenReturn(id);
        // and the topic has a title
        when(topicWrapper.getTitle()).thenReturn(title);
        // and the topic has xml
        when(topicWrapper.getXml()).thenReturn(randomString);
        // and the topic has a doctype
        when(topicWrapper.getXmlFormat()).thenReturn(CommonConstants.DOCBOOK_45);
        // and the topic has a locale
        when(topicWrapper.getLocale()).thenReturn(locale);
        // and the topic provider will return an existing
        when(topicProvider.getTopic(anyInt(), anyInt())).thenReturn(topicWrapper);
        // and the tag provider will return a new tag collection
        when(topicWrapper.getTags()).thenReturn(existingTagCollection);
        // and the property tag provider will return a new property tag collection
        when(topicWrapper.getProperties()).thenReturn(existingTopicProperties);
        // and the topic source url provider will return a new topic source url collection
        when(topicWrapper.getSourceURLs()).thenReturn(existingTopicSourceURLCollection);
        // and the property tag collection already has the added by tag
        existingProperties.add(existingAddedByPropertyTagInTopic);
        when(existingTopicProperties.getItems()).thenReturn(existingProperties);
        when(existingAddedByPropertyTagInTopic.getId()).thenReturn(ADDED_BY_PROPERTY_TAG_ID);
        when(existingAddedByPropertyTagInTopic.getValue()).thenReturn(username);
    }

    protected void setupBaseTopicMocks() {
        // and the topic provider will return a new topic
        when(topicProvider.newTopic()).thenReturn(newTopicWrapper);
        // and the tag provider will return a new tag collection
        when(tagProvider.newTagCollection()).thenReturn(tagCollection);
        newTopicWrapper.setTags(null);
        // and the property tag provider will return a new property tag collection
        when(propertyTagProvider.newPropertyTagInTopicCollection(any(TopicWrapper.class))).thenReturn(propertyTagCollection);
        newTopicWrapper.setProperties(null);
        // and the property tag provider will return a property tag
        when(propertyTagProvider.getPropertyTag(CSP_PROPERTY_ID)).thenReturn(cspIdPropertyTag);
        when(propertyTagProvider.getPropertyTag(ADDED_BY_PROPERTY_TAG_ID)).thenReturn(addedByPropertyTag);
        // and the topic source url provider will return a new topic source url collection
        when(topicSourceURLProvider.newTopicSourceURLCollection(eq(newTopicWrapper))).thenReturn(topicSourceURLCollection);
        newTopicWrapper.setSourceURLs(null);
    }

    protected void setupValidBaseTopicMocks() {
        final TagWrapper typeTag = makeTag(type);
        // and the tag provider returns a type
        when(tagProvider.getTagByName(eq(type))).thenReturn(typeTag);
        // and the tag provider returns an assigned writer
        when(tagProvider.getTagByName(eq(username))).thenReturn(writerTag);
        when(writerTag.getName()).thenReturn(username);
        // and the property tag provider creates a new property tag
        when(propertyTagProvider.newPropertyTagInTopic(eq(cspIdPropertyTag), any(TopicWrapper.class))).thenReturn(cspIdPropertyTagInTopic);
        when(propertyTagProvider.newPropertyTagInTopic(eq(addedByPropertyTag), any(TopicWrapper.class))).thenReturn(
                addedByPropertyTagInTopic);
    }

    protected void verifyValidBaseTopic(final TopicWrapper topic) {
        // Then check the topic isn't null
        assertNotNull(topic);
        // and the topic is the mocked topic
        assertNotSame(topic, is(topicWrapper));
        // and the id was set to null
        assertNull(topic.getId());
        // and the topic title was cloned
        assertThat(topic.getTitle(), is(title));
        // and the topic xml was cloned
        assertThat(topic.getXml(), is(randomString));
        // and the topic doctype was cloned
        assertThat(topic.getXmlFormat(), is(CommonConstants.DOCBOOK_45));
        // and the locale was cloned
        assertThat(topic.getLocale(), is(locale));
        // check that the added by property was not copied across
        assertFalse(propertyTagCollection.getAddItems().contains(existingAddedByPropertyTagInTopic));
        // check that the new added by property was set
        assertTrue(propertyTagCollection.getAddItems().contains(addedByPropertyTagInTopic));
        // and that the topic has the assigned writer set
        assertTrue(tagCollection.getAddItems().contains(writerTag));
    }

    protected void verifyValidBaseTopicNewProperties(final TopicWrapper topic) {
        // and the topic had the properties set
        assertSame(topic.getProperties(), propertyTagCollection);
        // and the topic had the CSP property tag set
        verify(cspIdPropertyTagInTopic, times(1)).setValue("L-C1");
        assertTrue(propertyTagCollection.getAddItems().contains(cspIdPropertyTagInTopic));
    }

    protected void verifyUnchangedOriginalTopic() {
        // and the topic title was not changed
        verify(topicWrapper, never()).setTitle(anyString());
        // and the id was not changed
        verify(topicWrapper, never()).setId(anyInt());
        // and the xml was not changed
        verify(topicWrapper, never()).setXml(anyString());
        // and the description was not changed
        verify(topicWrapper, never()).setDescription(anyString());
        // and the doctype was not changed
        verify(topicWrapper, never()).setXmlFormat(anyInt());
        // and the locale was not changed
        verify(topicWrapper, never()).setLocale(anyString());
        // and the added by value hasn't changed
        verify(existingAddedByPropertyTagInTopic, never()).setValue(anyString());
        // and the csp id value hasn't changed
        verify(existingCspIdPropertyTagInTopic, never()).setValue(anyString());
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