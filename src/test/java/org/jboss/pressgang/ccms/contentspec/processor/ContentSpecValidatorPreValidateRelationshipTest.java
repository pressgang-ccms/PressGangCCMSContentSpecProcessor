package org.jboss.pressgang.ccms.contentspec.processor;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.SpecNodeWithRelationships;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.entities.Relationship;
import org.jboss.pressgang.ccms.contentspec.entities.TargetRelationship;
import org.jboss.pressgang.ccms.contentspec.entities.TopicRelationship;
import org.jboss.pressgang.ccms.contentspec.enums.RelationshipType;
import org.jboss.pressgang.ccms.contentspec.enums.TopicType;
import org.jboss.pressgang.ccms.contentspec.test.makers.shared.SpecTopicMaker;
import org.jboss.pressgang.ccms.contentspec.utils.ContentSpecUtilities;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest({ContentSpecUtilities.class})
public class ContentSpecValidatorPreValidateRelationshipTest extends ContentSpecValidatorTest {
    @Arbitrary Integer id;
    @Arbitrary Integer secondId;
    @Arbitrary Integer thirdId;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String title;

    @Mock ContentSpec contentSpec;
    @Mock SpecTopic specTopic;

    SpecTopic topic1;
    SpecTopic topic2;
    SpecTopic duplicateTopic;

    @Before
    public void setUp() {
        topic1 = make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, id.toString()), with(SpecTopicMaker.title, title)));
        topic2 = make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, secondId.toString()), with(SpecTopicMaker.title, title)));
        duplicateTopic = make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, "X1"), with(SpecTopicMaker.title, title)));
        super.setUp();
    }

    @Test
    public void shouldFailAndLogErrorWhenTopicRelationshipToDuplicateTopic() {
        // Given a map of topics is returned
        PowerMockito.mockStatic(ContentSpecUtilities.class);
        when(ContentSpecUtilities.getIdSpecTopicMap(contentSpec)).thenReturn(createDummyTopicMap());
        // and some relationships
        final Map<SpecNodeWithRelationships, List<Relationship>> dummyMap = new HashMap<SpecNodeWithRelationships, List<Relationship>>();
        dummyMap.put(topic2, Arrays.asList((Relationship) new TopicRelationship(topic2, duplicateTopic, RelationshipType.PREREQUISITE)));
        given(contentSpec.getRelationships()).willReturn(dummyMap);

        // When prevalidating the relationships
        final boolean result = validator.preValidateRelationships(contentSpec);

        // Then the result should be false
        assertFalse(result);
        // and an error should have been printed
        assertThat(logger.getLogMessages().toString(), containsString(
                "Ambiguous Relationship! The link target is ambiguous, please use an explicit link target ID. Add [T<uniqueID>] to the "
                        + "instance you want to relate to, and use that as the link target."));
    }

    @Test
    public void shouldFailAndLogErrorWhenTopicRelationshipDoesntExist() {
        // Given a map of topics is returned
        PowerMockito.mockStatic(ContentSpecUtilities.class);
        when(ContentSpecUtilities.getIdSpecTopicMap(contentSpec)).thenReturn(createDummyTopicMap());
        // and a topic that isn't in the map
        SpecTopic topic = make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, thirdId.toString()), with(SpecTopicMaker.title, title)));
        // and some relationships
        final Map<SpecNodeWithRelationships, List<Relationship>> dummyMap = new HashMap<SpecNodeWithRelationships, List<Relationship>>();
        dummyMap.put(topic1, Arrays.asList((Relationship) new TopicRelationship(topic1, topic, RelationshipType.REFER_TO)));
        given(contentSpec.getRelationships()).willReturn(dummyMap);

        // When prevalidating the relationships
        final boolean result = validator.preValidateRelationships(contentSpec);

        // Then the result should be false
        assertFalse(result);
        // and an error should have been printed
        assertThat(logger.getLogMessages().toString(), containsString(
                "Invalid Relationship! The related topic specified (" + thirdId + ") doesn't exist in the content specification."));
    }

    @Test
    public void shouldFailAndLogErrorWhenTargetRelationshipDoesntExist() {
        // Given a map of topics is returned
        PowerMockito.mockStatic(ContentSpecUtilities.class);
        when(ContentSpecUtilities.getIdSpecTopicMap(contentSpec)).thenReturn(createDummyTopicMap());
        // and a dummy topic that can be used in the relationship
        SpecTopic topic = make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, "-1"), with(SpecTopicMaker.title, title),
                with(SpecTopicMaker.targetId, "T1")));
        // and some relationships
        final Map<SpecNodeWithRelationships, List<Relationship>> dummyMap = new HashMap<SpecNodeWithRelationships, List<Relationship>>();
        dummyMap.put(topic1, Arrays.asList((Relationship) new TargetRelationship(topic1, topic, RelationshipType.REFER_TO)));
        given(contentSpec.getRelationships()).willReturn(dummyMap);

        // When prevalidating the relationships
        final boolean result = validator.preValidateRelationships(contentSpec);

        // Then the result should be false
        assertFalse(result);
        // and an error should have been printed
        assertThat(logger.getLogMessages().toString(), containsString(
                "Invalid Relationship! The related topic specified (T1) doesn't exist in the content specification."));
    }

    @Test
    public void shouldFailAndLogErrorWhenTopicRelationshipHasMultipleReferences() {
        // Given a map of topics is returned
        PowerMockito.mockStatic(ContentSpecUtilities.class);
        Map<String, List<SpecTopic>> topicMap = createDummyTopicMap();
        when(ContentSpecUtilities.getIdSpecTopicMap(contentSpec)).thenReturn(topicMap);
        // and a topic with the same id
        SpecTopic topic = make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, id.toString()), with(SpecTopicMaker.title, title)));
        topicMap.get(id.toString()).add(topic);
        // and some relationships
        final Map<SpecNodeWithRelationships, List<Relationship>> dummyMap = new HashMap<SpecNodeWithRelationships, List<Relationship>>();
        dummyMap.put(topic2, Arrays.asList((Relationship) new TopicRelationship(topic2, topic, RelationshipType.LINKLIST)));
        given(contentSpec.getRelationships()).willReturn(dummyMap);

        // When prevalidating the relationships
        final boolean result = validator.preValidateRelationships(contentSpec);

        // Then the result should be false
        assertFalse(result);
        // and an error should have been printed
        assertThat(logger.getLogMessages().toString(), containsString(
                "Ambiguous Relationship! Topic " + id + " is included on lines " + topic1.getLineNumber() + " and " + topic.getLineNumber
                        () + " of the Content Specification. To relate to one of these topics please use a Target."));
    }

    @Test
    public void shouldFailAndLogErrorWhenTopicRelationshipRelatesToItself() {
        // Given a map of topics is returned
        PowerMockito.mockStatic(ContentSpecUtilities.class);
        when(ContentSpecUtilities.getIdSpecTopicMap(contentSpec)).thenReturn(createDummyTopicMap());
        // and a relationship to itself
        final Map<SpecNodeWithRelationships, List<Relationship>> dummyMap = new HashMap<SpecNodeWithRelationships, List<Relationship>>();
        dummyMap.put(topic1, Arrays.asList((Relationship) new TopicRelationship(topic1, topic1, RelationshipType.REFER_TO)));
        given(contentSpec.getRelationships()).willReturn(dummyMap);

        // When prevalidating the relationships
        final boolean result = validator.preValidateRelationships(contentSpec);

        // Then the result should be false
        assertFalse(result);
        // and an error should have been printed
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Relationship! You can't relate a topic to itself."));
    }

    @Test
    public void shouldFailAndLogErrorWhenTopicRelationshipExistsOnInitialContentTopic() {
        // Given a map of topics is returned
        PowerMockito.mockStatic(ContentSpecUtilities.class);
        Map<String, List<SpecTopic>> topicMap = createDummyTopicMap();
        when(ContentSpecUtilities.getIdSpecTopicMap(contentSpec)).thenReturn(topicMap);
        // and an initial content topic
        given(specTopic.getTopicType()).willReturn(TopicType.INITIAL_CONTENT);
        given(specTopic.getId()).willReturn(thirdId.toString());
        given(specTopic.getDBId()).willReturn(thirdId);
        topicMap.put(thirdId.toString(), Arrays.asList(specTopic));
        // and a relationship to itself
        final Map<SpecNodeWithRelationships, List<Relationship>> dummyMap = new HashMap<SpecNodeWithRelationships, List<Relationship>>();
        dummyMap.put(specTopic, Arrays.asList((Relationship) new TopicRelationship(specTopic, topic1, RelationshipType.REFER_TO)));
        given(contentSpec.getRelationships()).willReturn(dummyMap);

        // When prevalidating the relationships
        final boolean result = validator.preValidateRelationships(contentSpec);

        // Then the result should be false
        assertFalse(result);
        // and an error should have been printed
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Relationship! Initial Text topics cannot have " +
                "relationships applied directly. Instead they should be applied on the \"Initial Text\" container."));
    }

    protected Map<String, List<SpecTopic>> createDummyTopicMap() {
        final Map<String, List<SpecTopic>> dummyMap = new HashMap<String, List<SpecTopic>>();
        dummyMap.put(id.toString(), new ArrayList<SpecTopic>() {{
            add(topic1);
        }});
        dummyMap.put(secondId.toString(), new ArrayList<SpecTopic>() {{
            add(topic2);
        }});
        dummyMap.put("X1", Arrays.asList(duplicateTopic));
        return dummyMap;
    }
}
