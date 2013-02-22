package org.jboss.pressgang.ccms.contentspec.processor;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.enums.LevelType;
import org.jboss.pressgang.ccms.contentspec.exceptions.ParsingException;
import org.junit.Test;

import java.util.ArrayList;

import static java.util.Arrays.asList;
import static net.sf.ipsedixit.core.StringType.ALPHANUMERIC;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.jboss.pressgang.ccms.contentspec.TestUtil.selectRandomListItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
public class ContentSpecParserProcessLevelLineTest extends ContentSpecParserTest {

    @Arbitrary Integer lineNumber;
    @ArbitraryString(type = ALPHANUMERIC) String title;

    @Test
    public void shouldThrowExceptionIfLevelFormatInvalid() throws Exception {
        // Given a line with an invalid level format
        String line = "";

        // When the level line is processed
        try {
            parser.processLevelLine(line, lineNumber);

            // Then an exception is thrown with an appropriate error
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            assertThat(e.getMessage(), containsString("Invalid Chapter/Section/Appendix! Incorrect format."));
            // And the line number is included
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldReturnEmptyLevelForType() throws Exception {
        // Given a valid line with a valid level type
        ArrayList<LevelType> levelTypes = new ArrayList<LevelType>(asList(LevelType.values()));
        levelTypes.remove(LevelType.BASE);
        LevelType levelType = selectRandomListItem(levelTypes);
        String line = levelType.name() + ": " + title;

        // When the level line is processed
        Level result = parser.processLevelLine(line, lineNumber);

        // Then the result should be a level of that type
        assertThat(result.getType(), is(levelType));
    }

    @Test
    public void shouldReturnEmptyLevelOfTypeIfParsingProblem() throws Exception {
        // Given a line with a valid level type but invalid other contents
        ArrayList<LevelType> levelTypes = new ArrayList<LevelType>(asList(LevelType.values()));
        levelTypes.remove(LevelType.BASE);
        LevelType levelType = selectRandomListItem(levelTypes);
        String line = levelType.name() + ": " + "[";

        // When the level line is processed
        Level result = parser.processLevelLine(line, lineNumber);

        // Then an empty level of that type is returned
        assertThat(result.getType(), is(levelType));
        assertThat(result.getChildLevels().size(), is(0));
    }
}
