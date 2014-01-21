package org.jboss.pressgang.ccms.contentspec.processor;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static net.sf.ipsedixit.core.StringType.ALPHANUMERIC;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.jboss.pressgang.ccms.contentspec.TestUtil.selectRandomListItem;
import static org.jboss.pressgang.ccms.contentspec.test.makers.shared.LevelMaker.Level;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import org.hamcrest.Matchers;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.enums.LevelType;
import org.jboss.pressgang.ccms.contentspec.enums.TopicType;
import org.jboss.pressgang.ccms.contentspec.exceptions.ParsingException;
import org.jboss.pressgang.ccms.contentspec.test.makers.shared.SpecTopicMaker;
import org.junit.Test;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
public class ContentSpecParserParseLevelTest extends ContentSpecParserTest {

    @Arbitrary Integer lineNumber;
    @Arbitrary Integer id;
    @Arbitrary LevelType levelType;
    @ArbitraryString(type = ALPHANUMERIC) String title;
    @ArbitraryString(type = ALPHANUMERIC) String url;
    @ArbitraryString(type = ALPHANUMERIC) String writer;
    @ArbitraryString(type = ALPHANUMERIC) String topicTag;

    @Test
    public void shouldCreateEmptyLevelOfType() throws Exception {
        // Given a line number, level type and an empty line
        String line = "";

        // When process level is called
        Level result = parser.parseLevel(parserData, lineNumber, levelType, line);

        // Then an empty level of the given type is created a returned
        assertThat(result.getLevelType(), is(levelType));
        assertThat(result.getChildLevels().size(), is(0));
        // And it title is null
        assertNull(result.getTitle());
    }

    @Test
    public void shouldCreateLevelWithTitle() throws Exception {
        // Given a line number, level type and a line with a title
        String line = "Chapter:" + title;

        // When process level is called
        Level result = parser.parseLevel(parserData, lineNumber, levelType, line);

        // Then a level of the given type is created a returned
        assertThat(result.getLevelType(), is(levelType));
        // And it title is as given
        assertThat(result.getTitle(), is(title));
    }

    @Test
    public void shouldReplaceEscapeCharactersInTitle() throws Exception {
        // Given a line number, level type and a line with
        // a title containing an escape character
        String line = "Chapter:" + title + "\\," + title;

        // When process level is called
        Level result = parser.parseLevel(parserData, lineNumber, levelType, line);

        // Then a level of the given type is created a returned
        assertThat(result.getLevelType(), is(levelType));
        // And it title is as given
        assertThat(result.getTitle(), is(title + "," + title));
    }

    @Test
    public void shouldThrowExceptionWhenMultipleBracketedValues() throws Exception {
        // Given a line number, level type and a line with multiple bracketed values
        String line = "Chapter:" + title + "[foo] [bar]";

        // When process level is called
        try {
            parser.parseLevel(parserData, lineNumber, levelType, line);

            // Then a parsing exception should be thrown containing an appropriate error
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            assertThat(e.getMessage(), containsString("Duplicated bracket types found."));
            // And the line number should be included
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldSetOptions() throws Exception {
        // Given a line number, level type and a line specifying some options
        String line = "Section:" + title + "[URL=" + url + ",Writer=" + writer + "]";

        // When process level is called
        Level result = parser.parseLevel(parserData, lineNumber, levelType, line);

        // Then the options are set
        assertThat(result.getAssignedWriter(false), is(writer));
        assertThat(result.getSourceUrls(true), Matchers.contains(url));
    }

    @Test
    public void shouldSetTag() throws Exception {
        // Given a line number, level type and a line specifying a tag
        String line = "Section:" + title + "[" + topicTag + "]";

        // When process level is called
        Level result = parser.parseLevel(parserData, lineNumber, levelType, line);

        // Then the options are set
        assertThat(result.getTags(false), Matchers.contains(topicTag));
    }

    @Test
    public void shouldThrowExceptionIfAttemptToSetDuplicateTargetTopic() throws Exception {
        // Given a line number, level type and a line with an already existing target topic id
        String line = "Chapter:" + title + "[T" + id + "]";
        parserData.getTargetTopics().put("T" + id, make(a(SpecTopicMaker.SpecTopic)));

        // When process level is called
        try {
            parser.parseLevel(parserData, lineNumber, levelType, line);

            // Then a parsing exception should be thrown containing an appropriate error
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            assertThat(e.getMessage(), containsString("Target ID is duplicated. Target ID's must be unique."));
            // And the line number should be included
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldThrowExceptionIfAttemptToSetDuplicateTargetLevel() throws Exception {
        // Given a line number, level type and a line with an already existing target level id
        String line = "Chapter:" + title + "[T" + id + "]";
        parserData.getTargetLevels().put("T" + id, make(a(Level)));

        // When process level is called
        try {
            parser.parseLevel(parserData, lineNumber, levelType, line);

            // Then a parsing exception should be thrown containing an appropriate error
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            assertThat(e.getMessage(), containsString("Target ID is duplicated. Target ID's must be unique."));
            // And the line number should be included
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldAddTarget() throws Exception {
        // Given a line number, level type and a line specifying a target id
        String line = "Section:" + title + "[T" + id + "]";

        // When process level is called
        Level result = parser.parseLevel(parserData, lineNumber, levelType, line);

        // Then the target id should be set
        assertThat(result.getTargetId(), is("T" + id));
    }

    @Test
    public void shouldThrowExceptionIfRelationshipSpecified() throws Exception {
        // Given a line number, level type and a line with an appendix relationship specified
        List<String> relationshipTypes = Arrays.asList("R", "P", "NEXT", "PREV");
        String relationship = selectRandomListItem(relationshipTypes);
        String line = "Chapter:" + title + "[T" + id + "] [" + relationship + ": " + title + "[" + id + "]]";

        // When process level is called
        try {
            parser.parseLevel(parserData, lineNumber, levelType, line);

            // Then a parsing exception should be thrown containing an appropriate error
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            assertThat(e.getMessage(), containsString("Invalid Chapter! Relationships can't be used for a Chapter."));
            // And the line number should be included
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldSetInnerTopic() throws Exception {
        // Given a line number, level type and a line specifying an id
        String line = "Section:" + title + "[" + id + "]";

        // When process level is called
        Level result = parser.parseLevel(parserData, lineNumber, levelType, line);

        // Then the inner topic is set
        final SpecTopic frontMatterTopic = result.getFrontMatterTopics().get(0);
        assertNotNull(frontMatterTopic);
        assertEquals(frontMatterTopic.getId(), id.toString());
        assertEquals(frontMatterTopic.getTopicType(), TopicType.LEVEL);
    }

    @Test
    public void shouldSetInnerTopicAndGlobalOptions() throws Exception {
        // Given a line number, level type, a line specifying an id and a tag
        String line = "Section:" + title + "[" + id + "] [" + topicTag + "]";

        // When process level is called
        Level result = parser.parseLevel(parserData, lineNumber, levelType, line);

        // Then the inner topic and tag is set
        final SpecTopic frontMatterTopic = result.getFrontMatterTopics().get(0);
        assertNotNull(frontMatterTopic);
        assertEquals(frontMatterTopic.getId(), id.toString());
        assertThat(result.getTags(false), Matchers.contains(topicTag));
    }

//    @Test
//    public void shouldAddExternalTarget() throws Exception {
//        // Given a line number, level type and a line specifying an external target id
//        String line = "Section:" + title + "[ET" + id + "]";
//
//        // When process level is called
//        Level result = parser.parseLevel(lineNumber, levelType, line);
//
//        // Then the target id should be set
//        assertThat(result.getExternalTargetId(), is("ET" + id));
//    }

}
