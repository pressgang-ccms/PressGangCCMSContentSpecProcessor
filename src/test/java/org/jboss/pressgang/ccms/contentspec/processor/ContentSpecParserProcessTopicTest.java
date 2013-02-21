package org.jboss.pressgang.ccms.contentspec.processor;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.exceptions.ParsingException;
import org.jboss.pressgang.ccms.contentspec.test.makers.parser.TopicRelationshipStringMaker;
import org.jboss.pressgang.ccms.contentspec.test.makers.parser.TopicStringMaker;
import org.junit.Test;

public class ContentSpecParserProcessTopicTest extends ContentSpecParserTest {
    @Arbitrary Integer id;
    @Arbitrary Integer randomNumber;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String title;

    @Test
    public void shouldReturnTopicWithTitleAndId() {
        // Given a string that represents a topic with a title and id
        String topicString = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString())));
        // and a line number
        int lineNumber = randomNumber;

        // When parsing the topic string
        SpecTopic topic = null;
        try {
            topic = parser.processTopic(topicString, lineNumber);
        } catch (ParsingException e) {
            fail("Parsing should not have failed.");
        }

        // Then check that the topic has the right data set
        assertNotNull(topic);
        assertThat(topic.getId(), is(id.toString()));
        assertThat(topic.getTitle(), is(title));
        assertThat(topic.getUniqueId(), is("L" + randomNumber + "-" + id));
    }

    @Test
    public void shouldReturnTopicWithTitleIdAndUrl() {
        String url = "http://www.example.com/";
        // Given a string that represents a topic with a title and id
        String topicString = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString()),
                        with(TopicStringMaker.url, url)));
        // and a line number
        int lineNumber = randomNumber;

        // When parsing the topic string
        SpecTopic topic = null;
        try {
            topic = parser.processTopic(topicString, lineNumber);
        } catch (ParsingException e) {
            fail("Parsing should not have failed.");
        }

        // Then check that the topic has the right data set
        assertNotNull(topic);
        assertThat(topic.getId(), is(id.toString()));
        assertThat(topic.getTitle(), is(title));
        assertThat(topic.getUniqueId(), is("L" + randomNumber + "-" + id));
        assertThat(topic.getSourceUrls(), contains(url));
    }

    @Test
    public void shouldThrowExceptionWithMissingOpeningBracket() {
        // Given a string that represents a topic with a title, id and a missing bracket
        String topicString = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString()),
                        with(TopicStringMaker.missingOpeningBracket, true)));
        // and a line number
        int lineNumber = randomNumber;

        // When parsing the topic string
        SpecTopic topic = null;
        try {
            topic = parser.processTopic(topicString, lineNumber);
            fail("Parsing the topic should have thrown an exception.");
        } catch (ParsingException e) {
            assertThat(e.getMessage(),
                    containsString("Line " + lineNumber + ": Invalid Content Specification! Missing opening bracket ([) detected."));
        }
    }

    @Test
    public void shouldThrowExceptionWithMissingClosingBracket() {
        // Given a string that represents a topic with a title, id and a missing bracket
        String topicString = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString()),
                        with(TopicStringMaker.missingClosingBracket, true)));
        // and a line number
        int lineNumber = randomNumber;

        // When parsing the topic string
        SpecTopic topic = null;
        try {
            topic = parser.processTopic(topicString, lineNumber);
            fail("Parsing the topic should have thrown an exception.");
        } catch (ParsingException e) {
            assertThat(e.getMessage(),
                    containsString("Line " + lineNumber + ": Invalid Content Specification! Missing ending bracket (]) detected."));
        }
    }

    @Test
    public void shouldThrowExceptionWithNoId() {
        // Given a string that represents a topic with a title
        String topicString = make(a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title)));
        // and a line number
        int lineNumber = randomNumber;

        // When parsing the topic string
        SpecTopic topic = null;
        try {
            topic = parser.processTopic(topicString, lineNumber);
            fail("Parsing the topic should have thrown an exception.");
        } catch (ParsingException e) {
            assertThat(e.getMessage(), containsString("Line " + lineNumber + ": Invalid Topic! Title and ID must be specified."));
        }
    }

    @Test
    public void shouldThrowExceptionWithMissingVariable() {
        // Given a string that represents a topic title, id and a missing variable
        String topicString = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString()),
                        with(TopicStringMaker.missingVariable, true)));
        // and a line number
        int lineNumber = randomNumber;

        // When parsing the topic string
        SpecTopic topic = null;
        try {
            topic = parser.processTopic(topicString, lineNumber);
            fail("Parsing the topic should have thrown an exception.");
        } catch (ParsingException e) {
            assertThat(e.getMessage(),
                    containsString("Line " + lineNumber + ": Invalid Content Specification! Missing attribute detected."));
        }
    }

    @Test
    public void shouldParseTopicWithTargetNumber() {
        // Given a string that represents a topic title, id and a missing variable
        String topicString = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString()),
                        with(TopicStringMaker.targetId, "[T1]")));
        // and a line number
        int lineNumber = randomNumber;

        // When parsing the topic string
        SpecTopic topic = null;
        try {
            topic = parser.processTopic(topicString, lineNumber);
        } catch (ParsingException e) {
            fail("Parsing should not have failed.");
        }

        // Then check that the topic has the right data set
        assertNotNull(topic);
        assertThat(topic.getId(), is(id.toString()));
        assertThat(topic.getTitle(), is(title));
        assertThat(topic.getUniqueId(), is("L" + randomNumber + "-" + id));
        assertThat(topic.getTargetId(), is("T1"));
    }

    @Test
    public void shouldParseTopicWithTargetString() {
        // Given a string that represents a topic title, id and a missing variable
        String topicString = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString()),
                        with(TopicStringMaker.targetId, "[T-Test-String]")));
        // and a line number
        int lineNumber = randomNumber;

        // When parsing the topic string
        SpecTopic topic = null;
        try {
            topic = parser.processTopic(topicString, lineNumber);
        } catch (ParsingException e) {
            fail("Parsing should not have failed.");
        }

        // Then check that the topic has the right data set
        assertNotNull(topic);
        assertThat(topic.getId(), is(id.toString()));
        assertThat(topic.getTitle(), is(title));
        assertThat(topic.getUniqueId(), is("L" + randomNumber + "-" + id));
        assertThat(topic.getTargetId(), is("T-Test-String"));
    }

    @Test
    public void shouldThrowExceptionWithInvalidTarget() {
        // Given a string that represents a topic title, id and a missing variable
        String topicString = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString()),
                        with(TopicStringMaker.targetId, "[Test]")));
        // and a line number
        int lineNumber = randomNumber;

        // When parsing the topic string
        SpecTopic topic = null;
        try {
            topic = parser.processTopic(topicString, lineNumber);
            fail("Parsing the topic should have thrown an exception.");
        } catch (ParsingException e) {
            assertThat(e.getMessage(), containsString("Line " + lineNumber + ": Duplicated bracket types found."));
        }
    }

    @Test
    public void shouldParseTopicWithShortRefersToRelationship() {
        // Given a valid refers to relationship
        List<String> topics = Arrays.asList(id.toString(), "T0");
        String refersToRelationship = make(
                a(TopicRelationshipStringMaker.TopicRelationshipString, with(TopicRelationshipStringMaker.relationshipType, "R"),
                        with(TopicRelationshipStringMaker.relationships, topics)));
        // and a string that represents a topic title, id and a missing variable
        String topicString = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString()),
                        with(TopicStringMaker.relationship, refersToRelationship)));
        // and a line number
        int lineNumber = randomNumber;

        // When parsing the topic string
        SpecTopic topic = null;
        try {
            topic = parser.processTopic(topicString, lineNumber);
        } catch (ParsingException e) {
            fail("Parsing should not have failed.");
        }

        // Then check that the topic has the right data set
        String uniqueId = "L" + randomNumber + "-" + id;
        assertNotNull(topic);
        assertThat(topic.getId(), is(id.toString()));
        assertThat(topic.getTitle(), is(title));
        assertThat(topic.getUniqueId(), is(uniqueId));
        // and the right number of relationships exist
        assertThat(parser.getRelationships().size(), is(1));
        assertThat(parser.getRelationships().get(uniqueId).size(), is(2));
        // and the relationships have the right main topic id
        assertThat(parser.getRelationships().get(uniqueId).get(0).getMainRelationshipTopicId(), is(uniqueId));
        assertThat(parser.getRelationships().get(uniqueId).get(1).getMainRelationshipTopicId(), is(uniqueId));
    }

    @Test
    public void shouldThrowExceptionWithShortRelationshipWithMissingSeparator() {
        // Given an invalid refers to relationship
        List<String> topics = Arrays.asList(id.toString(), randomNumber.toString());
        String refersToRelationship = make(
                a(TopicRelationshipStringMaker.TopicRelationshipString, with(TopicRelationshipStringMaker.relationshipType, "R"),
                        with(TopicRelationshipStringMaker.missingVariable, true),
                        with(TopicRelationshipStringMaker.relationships, topics)));
        // and a string that represents a topic title, id and a missing variable
        String topicString = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString()),
                        with(TopicStringMaker.relationship, refersToRelationship)));
        // and a line number
        int lineNumber = randomNumber;

        // When parsing the topic string
        SpecTopic topic = null;
        try {
            topic = parser.processTopic(topicString, lineNumber);
            fail("Parsing the topic should have thrown an exception.");
        } catch (ParsingException e) {
            assertThat(e.getMessage(),
                    containsString("Line " + lineNumber + ": Invalid Content Specification! Missing attribute detected."));
        }
    }

    @Test
    public void shouldThrowExceptionWithShortRelationshipWithMissingOpeningBracket() {
        // Given an invalid refers to relationship
        List<String> topics = Arrays.asList(id.toString(), randomNumber.toString());
        String refersToRelationship = make(
                a(TopicRelationshipStringMaker.TopicRelationshipString, with(TopicRelationshipStringMaker.relationshipType, "R"),
                        with(TopicRelationshipStringMaker.missingOpeningBracket, true),
                        with(TopicRelationshipStringMaker.relationships, topics)));
        // and a string that represents a topic title, id and a missing variable
        String topicString = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString()),
                        with(TopicStringMaker.relationship, refersToRelationship)));
        // and a line number
        int lineNumber = randomNumber;

        // When parsing the topic string
        SpecTopic topic = null;
        try {
            topic = parser.processTopic(topicString, lineNumber);
            fail("Parsing the topic should have thrown an exception.");
        } catch (ParsingException e) {
            assertThat(e.getMessage(),
                    containsString("Line " + lineNumber + ": Invalid Content Specification! Missing opening bracket ([) detected."));
        }
    }

    @Test
    public void shouldThrowExceptionWithShortRelationshipWithMissingClosingBracket() {
        // Given an invalid refers to relationship
        List<String> topics = Arrays.asList(id.toString(), randomNumber.toString());
        String refersToRelationship = make(
                a(TopicRelationshipStringMaker.TopicRelationshipString, with(TopicRelationshipStringMaker.relationshipType, "R"),
                        with(TopicRelationshipStringMaker.missingClosingBracket, true),
                        with(TopicRelationshipStringMaker.relationships, topics)));
        // and a string that represents a topic title, id and a missing variable
        String topicString = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString()),
                        with(TopicStringMaker.relationship, refersToRelationship)));
        // and a line number
        int lineNumber = randomNumber;

        // When parsing the topic string
        SpecTopic topic = null;
        try {
            topic = parser.processTopic(topicString, lineNumber);
            fail("Parsing the topic should have thrown an exception.");
        } catch (ParsingException e) {
            assertThat(e.getMessage(),
                    containsString("Line " + lineNumber + ": Invalid Content Specification! Missing ending bracket (]) detected."));
        }
    }

    @Test
    public void shouldThrowExceptionWithLongRelationshipWithMissingSeparator() {
        // Given an invalid refers to relationship
        List<String> topics = Arrays.asList(id.toString(), randomNumber.toString());
        String refersToRelationship = make(
                a(TopicRelationshipStringMaker.TopicRelationshipString, with(TopicRelationshipStringMaker.relationshipType, "R"),
                        with(TopicRelationshipStringMaker.longRelationship, true), with(TopicRelationshipStringMaker.missingVariable, true),
                        with(TopicRelationshipStringMaker.relationships, topics)));
        // and the relationship is setup in the lines
        parser.getLines().addAll(Arrays.asList(refersToRelationship.split("\n")));
        // and a string that represents a topic title, id and a missing variable
        String topicString = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString())));
        // and a line number
        int lineNumber = randomNumber;

        // When parsing the topic string
        SpecTopic topic = null;
        try {
            topic = parser.processTopic(topicString, lineNumber);
            fail("Parsing the topic should have thrown an exception.");
        } catch (ParsingException e) {
            assertThat(e.getMessage(),
                    containsString("Line " + lineNumber + ": Invalid Content Specification! Missing attribute detected."));
        }
    }

    @Test
    public void shouldThrowExceptionWithLongRelationshipWithMissingOpeningBracket() {
        // Given an invalid refers to relationship
        List<String> topics = Arrays.asList(id.toString(), randomNumber.toString());
        String refersToRelationship = make(
                a(TopicRelationshipStringMaker.TopicRelationshipString, with(TopicRelationshipStringMaker.relationshipType, "R"),
                        with(TopicRelationshipStringMaker.longRelationship, true),
                        with(TopicRelationshipStringMaker.missingOpeningBracket, true),
                        with(TopicRelationshipStringMaker.relationships, topics)));
        // and the relationship is setup in the lines
        String[] lines = refersToRelationship.split("\n");
        //parser.getLines().addAll(Arrays.asList(lines).subList(1, lines.length));
        // and a string that represents a topic title, id and a missing variable
        String topicString = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString()),
                        with(TopicStringMaker.relationship, "[R: 6] P: 6 [T1]")));
        // and a line number
        int lineNumber = randomNumber;

        // When parsing the topic string
        SpecTopic topic = null;
        try {
            topic = parser.processTopic(topicString, lineNumber);
            fail("Parsing the topic should have thrown an exception.");
        } catch (ParsingException e) {
            assertThat(e.getMessage(),
                    containsString("Line " + lineNumber + ": Invalid Content Specification! Missing opening bracket ([) detected."));
        }
    }

    @Test
    public void shouldThrowExceptionWithLongRelationshipWithMissingClosingBracket() {
        // Given an invalid refers to relationship
        List<String> topics = Arrays.asList(id.toString(), randomNumber.toString());
        String refersToRelationship = make(
                a(TopicRelationshipStringMaker.TopicRelationshipString, with(TopicRelationshipStringMaker.relationshipType, "R"),
                        with(TopicRelationshipStringMaker.longRelationship, true),
                        with(TopicRelationshipStringMaker.missingClosingBracket, true),
                        with(TopicRelationshipStringMaker.relationships, topics)));
        // and the relationship is setup in the lines
        parser.getLines().addAll(Arrays.asList(refersToRelationship.split("\n")));
        // and a string that represents a topic title, id and a missing variable
        String topicString = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString())));
        // and a line number
        int lineNumber = randomNumber;

        // When parsing the topic string
        SpecTopic topic = null;
        try {
            topic = parser.processTopic(topicString, lineNumber);
            fail("Parsing the topic should have thrown an exception.");
        } catch (ParsingException e) {
            assertThat(e.getMessage(), containsString("Line " + lineNumber + ": Invalid Content Specification! Missing ending bracket (])" +
                    " detected."));
        }
    }
}
