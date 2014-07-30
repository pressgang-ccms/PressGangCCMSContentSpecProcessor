/*
  Copyright 2011-2014 Red Hat, Inc

  This file is part of PressGang CCMS.

  PressGang CCMS is free software: you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  PressGang CCMS is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with PressGang CCMS.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.jboss.pressgang.ccms.contentspec.processor;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static junit.framework.TestCase.assertTrue;
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
import org.jboss.pressgang.ccms.contentspec.InfoTopic;
import org.jboss.pressgang.ccms.contentspec.InitialContent;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.enums.LevelType;
import org.jboss.pressgang.ccms.contentspec.enums.TopicType;
import org.jboss.pressgang.ccms.contentspec.exceptions.ParsingException;
import org.jboss.pressgang.ccms.contentspec.test.makers.shared.SpecTopicMaker;
import org.junit.Before;
import org.junit.Test;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
public class ContentSpecParserParseLevelTest extends ContentSpecParserTest {

    @Arbitrary Integer lineNumber;
    @Arbitrary Integer id;
    @Arbitrary Integer revision;
    @Arbitrary LevelType levelType;
    @ArbitraryString(type = ALPHANUMERIC) String title;
    @ArbitraryString(type = ALPHANUMERIC) String url;
    @ArbitraryString(type = ALPHANUMERIC) String writer;
    @ArbitraryString(type = ALPHANUMERIC) String topicTag;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (levelType == LevelType.BASE || levelType == LevelType.PROCESS || levelType == LevelType.INITIAL_CONTENT) {
            levelType = LevelType.SECTION;
        }
    }

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
    public void shouldReplaceXMLCharReferencesInTitle() throws Exception {
        // Given a line number, level type and a line with
        // a title containing an escape character
        String line = "Chapter:" + title + "&#x002C;" + title;

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
        List<String> relationshipTypes = Arrays.asList("R", "P", "L", "NEXT", "PREV");
        String relationship = selectRandomListItem(relationshipTypes);
        String line = levelType.getTitle() + ":" + title + "[T" + id + "] [" + relationship + ": " + title + "[" + id + "]]";

        // When process level is called
        try {
            parser.parseLevel(parserData, lineNumber, levelType, line);

            // Then a parsing exception should be thrown containing an appropriate error
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            assertThat(e.getMessage(), containsString("Invalid " + levelType.getTitle() + "! Relationships can't be used for a " +
                    levelType.getTitle() + "."));
            // And the line number should be included
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldSetInitialContentTopic() throws Exception {
        // Given a line number, level type and a line specifying an id
        String line = "Section:" + title + "[" + id + "]";

        // When process level is called
        Level result = parser.parseLevel(parserData, lineNumber, levelType, line);

        // Then the inner topic is set
        final InitialContent initialContent = (InitialContent) result.getChildLevels().get(0);
        final SpecTopic initialContentTopic = initialContent.getSpecTopics().get(0);
        assertNotNull(initialContentTopic);
        assertEquals(initialContentTopic.getId(), id.toString());
        assertEquals(initialContentTopic.getTopicType(), TopicType.INITIAL_CONTENT);
    }

    @Test
    public void shouldSetInitialContentTopicAndGlobalOptions() throws Exception {
        // Given a line number, level type, a line specifying an id and a tag
        String line = "Section:" + title + "[" + id + "] [" + topicTag + "]";

        // When process level is called
        Level result = parser.parseLevel(parserData, lineNumber, levelType, line);

        // Then the inner topic and tag is set
        final InitialContent initialContent = (InitialContent) result.getChildLevels().get(0);
        final SpecTopic initialContentTopic = initialContent.getSpecTopics().get(0);
        assertNotNull(initialContentTopic);
        assertEquals(initialContentTopic.getId(), id.toString());
        assertThat(result.getTags(false), Matchers.contains(topicTag));
    }

    @Test
    public void shouldSetShorthandInitialContentTopicAndRelationships() throws Exception {
        // Given a line number, level type, a line specifying an id and a relationship
        List<String> relationshipTypes = Arrays.asList("R", "P", "L");
        String relationship = selectRandomListItem(relationshipTypes);
        String line = "Section:" + title + "[" + id + "] [" + relationship + ": T1]";
        // and the relationship exists
        parserData.getTargetTopics().put("T1", new SpecTopic(id, title));
        // and the generated uniqueid will be "L<LINE>-1"
        String uniqueId = "L" + lineNumber + "-1";

        // When process level is called
        Level result = parser.parseLevel(parserData, lineNumber, levelType, line);

        // Then the inner topic and relationship is set
        final InitialContent initialContent = (InitialContent) result.getChildLevels().get(0);
        final SpecTopic initialContentTopic = initialContent.getSpecTopics().get(0);
        assertNotNull(initialContentTopic);
        assertEquals(initialContentTopic.getId(), id.toString());
        assertThat(initialContent.getUniqueId(), is(uniqueId));
        assertTrue(parserData.getLevelRelationships().containsKey(uniqueId));
        assertThat(parserData.getLevelRelationships().get(uniqueId).size(), is(1));
        assertThat(parserData.getLevelRelationships().get(uniqueId).get(0).getSecondaryRelationshipId(), is("T1"));
    }

    @Test
    public void shouldParseInitialContentWithRelationships() throws Exception {
        // Given a line number, level type, a line specifying an id and a relationship
        List<String> relationshipTypes = Arrays.asList("R", "P", "L");
        String relationship = selectRandomListItem(relationshipTypes);
        String line = "Initial Text: [" + relationship + ": T1]";
        // and the relationship exists
        parserData.getTargetTopics().put("T1", new SpecTopic(id, title));
        // and the generated uniqueid will be "L<LINE>"
        String uniqueId = "L" + lineNumber;

        // When process level is called
        Level result = parser.parseLevel(parserData, lineNumber, LevelType.INITIAL_CONTENT, line);

        // Then the inner content container should exist and the relationship should exist
        assertNotNull(result);
        assertThat(result.getUniqueId(), is(uniqueId));
        assertTrue(parserData.getLevelRelationships().containsKey(uniqueId));
        assertThat(parserData.getLevelRelationships().get(uniqueId).size(), is(1));
        assertThat(parserData.getLevelRelationships().get(uniqueId).get(0).getSecondaryRelationshipId(), is("T1"));
    }

    @Test
    public void shouldSetInfoTopic() throws Exception {
        // Given a line number, level type and a line specifying an id
        String line = "Section:" + title + "[Info: " + id + "]";

        // When process level is called
        Level result = parser.parseLevel(parserData, lineNumber, levelType, line);

        // Then the info topic is set
        final InfoTopic infoTopic = result.getInfoTopic();
        assertNotNull(infoTopic);
        assertEquals(id.toString(), infoTopic.getId());
        assertEquals(TopicType.INFO, infoTopic.getTopicType());
    }

    @Test
    public void shouldSetInfoTopicWithRevision() throws Exception {
        // Given a line number, level type and a line specifying an id
        String line = "Chapter:" + title + "[Info: " + id + ", rev: " + revision + "]";

        // When process level is called
        Level result = parser.parseLevel(parserData, lineNumber, levelType, line);

        // Then the info topic is set
        final InfoTopic infoTopic = result.getInfoTopic();
        assertNotNull(infoTopic);
        assertEquals(id.toString(), infoTopic.getId());
        assertEquals(TopicType.INFO, infoTopic.getTopicType());
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
