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

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.exceptions.ParsingException;
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
            assertThat(e.getMessage(), containsString("Invalid Topic! Incorrect topic format."));
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
            assertThat(e.getMessage(), containsString("Invalid Content Specification! Missing ending bracket (]) detected."));
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
            assertThat(e.getMessage(), containsString("Invalid Topic! Title and ID must be specified."));
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
            assertThat(e.getMessage(), containsString("Invalid Content Specification! Missing attribute detected."));
        }
    }
}
