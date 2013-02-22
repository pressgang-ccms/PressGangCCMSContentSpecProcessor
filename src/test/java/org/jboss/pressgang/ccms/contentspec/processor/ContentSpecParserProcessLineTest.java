package org.jboss.pressgang.ccms.contentspec.processor;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.Chapter;
import org.jboss.pressgang.ccms.contentspec.Comment;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.KeyValueNode;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.TextNode;
import org.jboss.pressgang.ccms.contentspec.enums.LevelType;
import org.jboss.pressgang.ccms.contentspec.exceptions.IndentationException;
import org.jboss.pressgang.ccms.contentspec.test.makers.parser.LevelStringMaker;
import org.jboss.pressgang.ccms.contentspec.test.makers.parser.TopicStringMaker;
import org.junit.Test;

public class ContentSpecParserProcessLineTest extends ContentSpecParserTest {

    @Arbitrary Integer id;
    @Arbitrary Integer lineNumber;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String title;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String randomString;

    @Test
    public void shouldAddBlankLineToContentSpec() {
        // Given a content spec object
        final ContentSpec contentSpec = new ContentSpec();
        // and a blank line
        String blankLine = "    ";
        // and the current level is the content spec level
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When processing a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, blankLine, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then verify that the content spec has a text node for the line
        assertThat(contentSpec.getNodes().size(), is(1));
        final TextNode textNode = (TextNode) contentSpec.getNodes().get(0);
        assertThat(textNode.getText(), is("\n"));
        // and the line processed successfully
        assertTrue(result);
    }

    @Test
    public void shouldAddBlankLineToLevel() {
        // Given a content spec object
        final ContentSpec contentSpec = new ContentSpec();
        // and a blank line
        String blankLine = "    ";
        // and the current level is a level in the content spec
        final Level level = new Chapter(title);
        parser.setCurrentLevel(level);

        // When processing a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, blankLine, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then verify that the content spec has a text node for the line
        assertThat(level.getChildNodes().size(), is(1));
        final TextNode textNode = (TextNode) level.getChildNodes().get(0);
        assertThat(textNode.getText(), is("\n"));
        // and the line processed successfully
        assertTrue(result);
    }

    @Test
    public void shouldAddCommentLineToContentSpec() {
        // Given a content spec object
        final ContentSpec contentSpec = new ContentSpec();
        // and a comment line
        String commentLine = "    # Test Comment";
        // and the current level is the content spec level
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When processing a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, commentLine, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then verify that the content spec has a text node for the line
        assertThat(contentSpec.getNodes().size(), is(1));
        final Comment commentNode = (Comment) contentSpec.getNodes().get(0);
        assertThat(commentNode.getText(), is(commentLine));
        // and the line processed successfully
        assertTrue(result);
    }

    @Test
    public void shouldAddCommentLineToLevel() {
        // Given a content spec object
        final ContentSpec contentSpec = new ContentSpec();
        // and a comment line
        String commentLine = "    # Test Comment";
        // and the current level is the some random level
        final Level level = new Chapter(title);
        parser.setCurrentLevel(level);

        // When processing a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, commentLine, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then verify that the content spec has a text node for the line
        assertThat(level.getChildNodes().size(), is(1));
        final Comment commentNode = (Comment) level.getChildNodes().get(0);
        assertThat(commentNode.getText(), is(commentLine));
        // and the line processed successfully
        assertTrue(result);
    }

    @Test
    public void shouldThrowExceptionWhenIndentationIsInvalid() {
        // Given a content spec object
        final ContentSpec contentSpec = new ContentSpec();
        // and a line that has invalid indentation
        String topicLine = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString()),
                        with(TopicStringMaker.indentation, 2)));
        // and the current level is the base content spec
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When processing a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, topicLine, lineNumber);
            fail("Indentation Exception should have been thrown.");
        } catch (IndentationException e) {
            assertThat(e.getMessage(), containsString("Line " + lineNumber + ": Invalid Content Specification! Indentation is invalid."));
        }
    }

    @Test
    public void shouldThrowExceptionWhenIndentationSpaceIsInvalid() {
        // Given a content spec object
        final ContentSpec contentSpec = new ContentSpec();
        // and a valid line
        String topicLine = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString()),
                        with(TopicStringMaker.indentation, 1)));
        // and the current level is the base content spec
        parser.setCurrentLevel(contentSpec.getBaseLevel());
        // and a changed indentation size to break content
        parser.setIndentationSize(4);

        // When processing a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, topicLine, lineNumber);
            fail("Indentation Exception should have been thrown.");
        } catch (IndentationException e) {
            assertThat(e.getMessage(), containsString("Line " + lineNumber + ": Invalid Content Specification! Indentation is invalid."));
        }
    }

    @Test
    public void shouldChangeLevelWhenIndentationDecreases() {
        // Given a content spec object
        final ContentSpec contentSpec = new ContentSpec();
        // and a sub level for the content spec
        final Level level = new Chapter(title);
        contentSpec.getBaseLevel().appendChild(level);
        // and a valid line that exists on the base level
        String topicLine = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString()),
                        with(TopicStringMaker.indentation, 0)));
        // and the current level is the sublevel
        parser.setCurrentLevel(level);
        // and the indentation level matches the sublevel
        parser.setIndentationLevel(1);

        // When processing a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, topicLine, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then the current level should be the base level
        assertThat(parser.getCurrentLevel(), is(contentSpec.getBaseLevel()));
        assertTrue(result);
        // and the base level contains the topic
        assertThat(contentSpec.getBaseLevel().getChildNodes().size(), is(2));
        assertThat(level.getChildNodes().size(), is(0));
    }

    @Test
    public void shouldChangeLevelWhenIndentationDecreasesMultipleTimes() {
        // Given a content spec object
        final ContentSpec contentSpec = new ContentSpec();
        // and a sub level for the content spec
        final Level level = new Chapter(title);
        contentSpec.getBaseLevel().appendChild(level);
        // and another sub level for the level
        final Level subLevel = new Chapter(title);
        level.appendChild(subLevel);
        // and a valid line that exists on the base level
        String topicLine = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString()),
                        with(TopicStringMaker.indentation, 0)));
        // and the current level is the lowest sublevel
        parser.setCurrentLevel(subLevel);
        // and the indentation level matches the sublevel
        parser.setIndentationLevel(2);

        // When processing a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, topicLine, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then the current level should be the base level
        assertThat(parser.getCurrentLevel(), is(contentSpec.getBaseLevel()));
        assertTrue(result);
        // and the base level contains the topic
        assertThat(contentSpec.getBaseLevel().getChildNodes().size(), is(2));
        assertThat(level.getChildNodes().size(), is(1));
        assertThat(subLevel.getChildNodes().size(), is(0));
    }

    @Test
    public void shouldAddMetaDataToContentSpec() {
        // Given a content spec
        final ContentSpec contentSpec = new ContentSpec();
        // and a meta data line
        final String metaData = "Title = " + title;
        // and the current level is the content spec level
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When processing a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, metaData, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the meta data assigned
        assertThat(contentSpec.getNodes().size(), is(1));
        final KeyValueNode<String> metaDataNode = (KeyValueNode<String>) contentSpec.getNodes().get(0);
        assertThat(metaDataNode.getKey(), is("Title"));
        assertThat(metaDataNode.getValue(), is(title));
        assertThat(contentSpec.getTitle(), is(title));
        // and the line processed successfully
        assertTrue(result);
    }

    @Test
    public void shouldAddTopicToContentSpec() {
        // Given a content spec object
        final ContentSpec contentSpec = new ContentSpec();
        // and a valid line that exists on the base level
        String topicLine = make(
                a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title), with(TopicStringMaker.id, id.toString()),
                        with(TopicStringMaker.indentation, 0)));
        // and the current level is the content spec level
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When processing a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, topicLine, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the topic assigned
        assertThat(contentSpec.getBaseLevel().getChildNodes().size(), is(1));
        final SpecTopic specTopic = (SpecTopic) contentSpec.getBaseLevel().getChildNodes().get(0);
        assertThat(specTopic.getTitle(), is(title));
        assertThat(specTopic.getId(), is(id.toString()));
        // and the line processed successfully
        assertTrue(result);
    }

    @Test
    public void shouldAddLevelToContentSpec() {
        // Given a content spec object
        final ContentSpec contentSpec = new ContentSpec();
        // and a valid line that exists on the base level
        String levelLine = make(
                a(LevelStringMaker.LevelString, with(LevelStringMaker.levelType, LevelType.CHAPTER), with(LevelStringMaker.title, title)));
        // and the current level is the content spec level
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When processing a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, levelLine, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the topic assigned
        assertThat(contentSpec.getBaseLevel().getChildNodes().size(), is(1));
        final Level level = (Level) contentSpec.getBaseLevel().getChildNodes().get(0);
        assertThat(level.getTitle(), is(title));
        assertThat(level.getType(), is(LevelType.CHAPTER));
        // and the line processed successfully
        assertTrue(result);
    }

    @Test
    public void shouldAddProcessToContentSpec() {
        // Given a content spec object
        final ContentSpec contentSpec = new ContentSpec();
        // and a valid line that exists on the base level
        String levelLine = make(
                a(LevelStringMaker.LevelString, with(LevelStringMaker.levelType, LevelType.PROCESS), with(LevelStringMaker.title, title)));
        // and the current level is the content spec level
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When processing a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, levelLine, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the topic assigned
        assertThat(contentSpec.getBaseLevel().getChildNodes().size(), is(1));
        final Level level = (Level) contentSpec.getBaseLevel().getChildNodes().get(0);
        assertThat(level.getTitle(), is(title));
        assertThat(level.getType(), is(LevelType.PROCESS));
        // and the processes list in the parser has the process
        assertThat(parser.getProcesses().size(), is(1));
        // and the line processed successfully
        assertTrue(result);
    }

    @Test
    public void shouldPrintErrorAndReturnFalseWhenLineIsInvalid() {
        // Given a content spec object
        final ContentSpec contentSpec = new ContentSpec();
        // and a line that is invalid
        String topicLine = make(a(TopicStringMaker.TopicString, with(TopicStringMaker.title, title)));
        // and the current level is the base content spec
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When processing a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, topicLine, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then the result should be false
        assertFalse(result);
        // and an error message should exist
        assertThat(logger.getLogMessages().size(), is(1));
    }

    @Test
    public void shouldAddUrlToContentSpec() {
        String url = "http://www.example.com/";
        // Given a string that represents a global option to define the options
        String options = "[URL = " + url + "]";
        // and a content spec
        final ContentSpec contentSpec = new ContentSpec();
        // and the current level is the base content spec
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        assertTrue(result);
        assertThat(contentSpec.getSourceUrls().size(), is(1));
        assertThat(contentSpec.getSourceUrls().get(0), is(url));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddMultipleUrlsToContentSpec() {
        String url = "http://www.example.com/";
        String url2 = "http://www.domain.com/";
        // Given a string that represents a global option to define the options
        String options = "[URL = " + url + ", URL = " + url2 + "]";
        // and a content spec
        final ContentSpec contentSpec = new ContentSpec();
        // and the current level is the base content spec
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        assertTrue(result);
        assertThat(contentSpec.getSourceUrls().size(), is(2));
        assertThat(contentSpec.getSourceUrls().get(0), is(url));
        assertThat(contentSpec.getSourceUrls().get(1), is(url2));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddDescriptionToContentSpec() {
        // Given a string that represents a global option to define the options
        String options = "[Description = " + title + "]";
        // and a content spec
        final ContentSpec contentSpec = new ContentSpec();
        // and the current level is the base content spec
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        assertTrue(result);
        assertThat(contentSpec.getDescription(), is(title));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddConditionToContentSpec() {
        // Given a string that represents a global option to define the options
        String options = "[condition = " + title + "]";
        // and a content spec
        final ContentSpec contentSpec = new ContentSpec();
        // and the current level is the base content spec
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        assertTrue(result);
        assertThat(contentSpec.getBaseLevel().getConditionStatement(), is(title));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddWriterToContentSpec() {
        // Given a string that represents a global option to define the options
        String options = "[Writer = " + title + "]";
        // and a content spec
        final ContentSpec contentSpec = new ContentSpec();
        // and the current level is the base content spec
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        assertTrue(result);
        assertThat(contentSpec.getAssignedWriter(), is(title));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddTagToContentSpec() {
        // Given a string that represents a global option to define the options
        String options = "[" + title + "]";
        // and a content spec
        final ContentSpec contentSpec = new ContentSpec();
        // and the current level is the base content spec
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        assertTrue(result);
        assertThat(contentSpec.getTags().size(), is(1));
        assertThat(contentSpec.getTags().get(0), is(title));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddTagWrappedInCategoryToContentSpec() {
        // Given a string that represents a global option to define the options
        String options = "[Test : " + title + "]";
        // and a content spec
        final ContentSpec contentSpec = new ContentSpec();
        // and the current level is the base content spec
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        assertTrue(result);
        assertThat(contentSpec.getTags().size(), is(1));
        assertThat(contentSpec.getTags().get(0), is(title));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddRemoveTagToContentSpec() {
        // Given a string that represents a global option to define the options
        String options = "[-" + title + "]";
        // and a content spec
        final ContentSpec contentSpec = new ContentSpec();
        // and the current level is the base content spec
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        assertTrue(result);
        assertThat(contentSpec.getTags().size(), is(0));
        assertThat(contentSpec.getRemoveTags().size(), is(1));
        assertThat(contentSpec.getRemoveTags().get(0), is(title));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddMultipleTagsToContentSpec() {
        // Given a string that represents a global option to define the options
        String options = "[" + title + ", +" + randomString + ", Test]";
        // and a content spec
        final ContentSpec contentSpec = new ContentSpec();
        // and the current level is the base content spec
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        assertTrue(result);
        assertThat(contentSpec.getTags().size(), is(3));
        assertThat(contentSpec.getTags().get(0), is(title));
        assertThat(contentSpec.getTags().get(1), is(randomString));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddMultipleTagsWrappedInCategoryToContentSpec() {
        // Given a string that represents a global option to define the options
        String options = "[Test : (" + title + ", +" + randomString + ", Test)]";
        // and a content spec
        final ContentSpec contentSpec = new ContentSpec();
        // and the current level is the base content spec
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        assertTrue(result);
        assertThat(contentSpec.getTags().size(), is(3));
        assertThat(contentSpec.getTags().get(0), is(title));
        assertThat(contentSpec.getTags().get(1), is(randomString));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldPrintErrorAndReturnFalseWhenConditionIsInvalid() {
        // Given a content spec object
        final ContentSpec contentSpec = new ContentSpec();
        // and a line that is a global option
        String options = "[condition = (" + title + "]";
        // and the current level is the base content spec
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When processing a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then the result should be false
        assertFalse(result);
        // and an error message should exist
        assertThat(logger.getLogMessages().size(), is(1));
        assertThat(logger.getLogMessages().get(0).toString(), containsString("Line " + lineNumber + ": Invalid Content Specification! The" +
                " condition statement must be a valid regular expression string."));
    }

    @Test
    public void shouldPrintErrorAndReturnFalseWhenOptionIsInvalid() {
        // Given a content spec object
        final ContentSpec contentSpec = new ContentSpec();
        // and a line that is a global option
        String options = "[blah = " + title + "]";
        // and the current level is the base content spec
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When processing a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then the result should be false
        assertFalse(result);
        // and an error message should exist
        assertThat(logger.getLogMessages().size(), is(1));
        assertThat(logger.getLogMessages().get(0).toString(),
                containsString("Line " + lineNumber + ": Invalid Content Specification! Unknown metadata tag found. " +
                        "\"condition\", \"Description\", \"URL\" and \"Writer\" are currently the only supported metadata."));
    }

    @Test
    public void shouldPrintErrorAndReturnFalseWhenOptionIsBlank() {
        // Given a content spec object
        final ContentSpec contentSpec = new ContentSpec();
        // and a line that is a global option
        String options = "[condition = ]";
        // and the current level is the base content spec
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When processing a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then the result should be false
        assertFalse(result);
        // and an error message should exist
        assertThat(logger.getLogMessages().size(), is(1));
        assertThat(logger.getLogMessages().get(0).toString(),
                containsString("Line " + lineNumber + ": Invalid Content Specification! Incorrect attribute format."));
    }

    @Test
    public void shouldPrintErrorAndReturnFalseWhenTagListIsBlankWhenWrappedInCategory() {
        // Given a content spec object
        final ContentSpec contentSpec = new ContentSpec();
        // and a line that is a global option
        String options = "[Test : () ]";
        // and the current level is the base content spec
        parser.setCurrentLevel(contentSpec.getBaseLevel());

        // When processing a line
        Boolean result = null;
        try {
            result = parser.processLine(contentSpec, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then the result should be false
        assertFalse(result);
        // and an error message should exist
        assertThat(logger.getLogMessages().size(), is(1));
        assertThat(logger.getLogMessages().get(0).toString(),
                containsString("Line " + lineNumber + ": Invalid Content Specification! Incorrect tag attribute format."));
    }
}