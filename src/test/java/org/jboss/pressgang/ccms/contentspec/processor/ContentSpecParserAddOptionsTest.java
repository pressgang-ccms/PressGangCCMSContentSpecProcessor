package org.jboss.pressgang.ccms.contentspec.processor;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.exceptions.IndentationException;
import org.junit.Test;

public class ContentSpecParserAddOptionsTest extends ContentSpecParserTest {

    @Arbitrary Integer id;
    @Arbitrary Integer lineNumber;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String title;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String randomString;

    @Test
    public void shouldAddUrlToNode() {
        String url = "http://www.example.com/";
        // Given a string that represents a global option to define the options
        String options = "[URL = " + url + "]";

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        final ContentSpec contentSpec = parserData.getContentSpec();
        assertTrue(result);
        assertThat(contentSpec.getSourceUrls().size(), is(1));
        assertThat(contentSpec.getSourceUrls().get(0), is(url));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddMultipleUrlsToNode() {
        String url = "http://www.example.com/";
        String url2 = "http://www.domain.com/";
        // Given a string that represents a global option to define the options
        String options = "[URL = " + url + ", URL = " + url2 + "]";

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        final ContentSpec contentSpec = parserData.getContentSpec();
        assertTrue(result);
        assertThat(contentSpec.getSourceUrls().size(), is(2));
        assertThat(contentSpec.getSourceUrls().get(0), is(url));
        assertThat(contentSpec.getSourceUrls().get(1), is(url2));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddDescriptionToNode() {
        // Given a string that represents a global option to define the options
        String options = "[Description = " + title + "]";

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        final ContentSpec contentSpec = parserData.getContentSpec();
        assertTrue(result);
        assertThat(contentSpec.getDescription(), is(title));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddConditionToNode() {
        // Given a string that represents a global option to define the options
        String options = "[condition = " + title + "]";

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        final ContentSpec contentSpec = parserData.getContentSpec();
        assertTrue(result);
        assertThat(contentSpec.getBaseLevel().getConditionStatement(), is(title));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddGroupedConditionToNode() {
        // Given a string that represents a global option to define the options
        String options = "[condition = " + title + "[A-Z]]";

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        final ContentSpec contentSpec = parserData.getContentSpec();
        assertTrue(result);
        assertThat(contentSpec.getBaseLevel().getConditionStatement(), is(title + "[A-Z]"));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddWriterToNode() {
        // Given a string that represents a global option to define the options
        String options = "[Writer = " + title + "]";

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        final ContentSpec contentSpec = parserData.getContentSpec();
        assertTrue(result);
        assertThat(contentSpec.getAssignedWriter(), is(title));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddTagToNode() {
        // Given a string that represents a global option to define the options
        String options = "[" + title + "]";

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        final ContentSpec contentSpec = parserData.getContentSpec();
        assertTrue(result);
        assertThat(contentSpec.getTags().size(), is(1));
        assertThat(contentSpec.getTags().get(0), is(title));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddTagWrappedInCategoryToNode() {
        // Given a string that represents a global option to define the options
        String options = "[Test : " + title + "]";

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        final ContentSpec contentSpec = parserData.getContentSpec();
        assertTrue(result);
        assertThat(contentSpec.getTags().size(), is(1));
        assertThat(contentSpec.getTags().get(0), is(title));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddRemoveTagToNode() {
        // Given a string that represents a global option to define the options
        String options = "[-" + title + "]";

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        final ContentSpec contentSpec = parserData.getContentSpec();
        assertTrue(result);
        assertThat(contentSpec.getTags().size(), is(0));
        assertThat(contentSpec.getRemoveTags().size(), is(1));
        assertThat(contentSpec.getRemoveTags().get(0), is(title));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddMultipleTagsToNode() {
        // Given a string that represents a global option to define the options
        String options = "[" + title + ", +" + randomString + ", Test]";

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        final ContentSpec contentSpec = parserData.getContentSpec();
        assertTrue(result);
        assertThat(contentSpec.getTags().size(), is(3));
        assertThat(contentSpec.getTags().get(0), is(title));
        assertThat(contentSpec.getTags().get(1), is(randomString));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldAddMultipleTagsWrappedInCategoryToNode() {
        // Given a string that represents a global option to define the options
        String options = "[Test : (" + title + ", +" + randomString + ", Test)]";

        // When parsing the a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then check that the content spec has the right data set
        final ContentSpec contentSpec = parserData.getContentSpec();
        assertTrue(result);
        assertThat(contentSpec.getTags().size(), is(3));
        assertThat(contentSpec.getTags().get(0), is(title));
        assertThat(contentSpec.getTags().get(1), is(randomString));
        assertThat(contentSpec.getNodes().size(), is(0));
    }

    @Test
    public void shouldPrintErrorAndReturnFalseWhenConditionIsInvalid() {
        // Given a line that is a global option
        String options = "[condition = (" + title + "]";

        // When processing a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
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
        // Given a line that is a global option
        String options = "[blah = " + title + "]";

        // When processing a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then the result should be false
        assertFalse(result);
        // and an error message should exist
        assertThat(logger.getLogMessages().size(), is(1));
        assertThat(logger.getLogMessages().get(0).toString(),
                containsString("Line " + lineNumber + ": Invalid Content Specification! Unknown attribute found. " +
                        "\"condition\", \"Description\", \"URL\" and \"Writer\" are currently the only supported attributes."));
    }

    @Test
    public void shouldPrintErrorAndReturnFalseWhenOptionIsBlank() {
        // Given a line that is a global option
        String options = "[condition = ]";

        // When processing a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
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
        // Given a line that is a global option
        String options = "[Test : () ]";

        // When processing a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
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

    @Test
    public void shouldPrintErrorAndReturnFalseWhenTagIsId() {
        // Given a line that is a global option
        String options = "[N1]";

        // When processing a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then the result should be false
        assertFalse(result);
        // and an error message should exist
        assertThat(logger.getLogMessages().size(), is(1));
        assertThat(logger.getLogMessages().get(0).toString(),
                containsString("Line " + lineNumber + ": Invalid Content Specification! Topic ID specified in the wrong location."));
    }

    @Test
    public void shouldPrintErrorAndReturnFalseWhenTagIsDuplicated() {
        // Given a line that is a global option
        String options = "[" + title + "," + title + "]";

        // When processing a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then the result should be false
        assertFalse(result);
        // and an error message should exist
        assertThat(logger.getLogMessages().size(), is(1));
        assertThat(logger.getLogMessages().get(0).toString(),
                containsString("Line " + lineNumber + ": Invalid Content Specification! Tag is duplicated."));
    }

    @Test
    public void shouldPrintErrorAndReturnFalseWhenTagIsDuplicatedWhenWrappedInCategory() {
        // Given a line that is a global option
        String options = "[Test : (" + title + ", " + title + ")]";

        // When processing a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then the result should be false
        assertFalse(result);
        // and an error message should exist
        assertThat(logger.getLogMessages().size(), is(1));
        assertThat(logger.getLogMessages().get(0).toString(),
                containsString("Line " + lineNumber + ": Invalid Content Specification! One or more tags are duplicated."));
    }

    @Test
    public void shouldPrintErrorAndReturnFalseWhenDuplicateAttributesSet() {
        // Given a line that is a global option with two conditions set
        String options = "[condition=a, condition=b]";

        // When processing a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then the result should be false
        assertFalse(result);
        // and an error message should exist
        assertThat(logger.getLogMessages().size(), is(1));
        assertThat(logger.getLogMessages().get(0).toString(),
                containsString("Line " + lineNumber + ": Invalid attribute, \"condition\" has already been defined."));
    }

    @Test
    public void shouldPrintErrorAndReturnFalseWhenGlobalRelationshipsUsed() {
        // Given a line that is a relationship on the base level
        String options = "[R: 10]";

        // When processing a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then the result should be false
        assertFalse(result);
        // and an error message should exist
        assertThat(logger.getLogMessages().size(), is(1));
        assertThat(logger.getLogMessages().get(0).toString(), containsString("Line " + lineNumber + ": Invalid Content Specification! Relationships can't be at the base level"));
    }

    @Test
    public void shouldPrintWarningWhenEmptyOptionsUsed() {
        // Given a line that has no data
        String options = "[]";

        // When processing a line
        try {
            parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then a warning message should exist
        assertThat(logger.getLogMessages().size(), is(1));
        assertThat(logger.getLogMessages().get(0).toString(), containsString("Line " + lineNumber + ": Empty brackets found."));
    }

    @Test
    public void shouldPrintErrorAndReturnFalseWhenDuplicateDescriptionsUsed() {
        // Given a line that has duplicate description options
        String options = "[description = blah, description = blah2]";

        // When processing a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then the result should be false
        assertFalse(result);
        // and an error message should exist
        assertThat(logger.getLogMessages().size(), is(1));
        assertThat(logger.getLogMessages().get(0).toString(), containsString("Line " + lineNumber + ": Invalid attribute, " +
                "\"description\" has already been defined."));
    }

    @Test
    public void shouldPrintErrorAndReturnFalseWhenDuplicateWritersUsed() {
        // Given a line that has duplicate writer options
        String options = "[writer = blah, writer = blah2]";

        // When processing a line
        Boolean result = null;
        try {
            result = parser.parseLine(parserData, options, lineNumber);
        } catch (IndentationException e) {
            fail("Indentation Exception should not have been thrown.");
        }

        // Then the result should be false
        assertFalse(result);
        // and an error message should exist
        assertThat(logger.getLogMessages().size(), is(1));
        assertThat(logger.getLogMessages().get(0).toString(), containsString("Line " + lineNumber + ": Invalid attribute, \"writer\"" +
                " has already been defined."));
    }
}
