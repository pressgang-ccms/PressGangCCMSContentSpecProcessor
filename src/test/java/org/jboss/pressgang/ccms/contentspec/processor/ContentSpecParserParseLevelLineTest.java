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

import static java.util.Arrays.asList;
import static net.sf.ipsedixit.core.StringType.ALPHANUMERIC;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.jboss.pressgang.ccms.contentspec.TestUtil.selectRandomLevelType;
import static org.jboss.pressgang.ccms.contentspec.TestUtil.selectRandomListItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.enums.LevelType;
import org.jboss.pressgang.ccms.contentspec.exceptions.ParsingException;
import org.junit.Test;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
public class ContentSpecParserParseLevelLineTest extends ContentSpecParserTest {

    @Arbitrary Integer lineNumber;
    @ArbitraryString(type = ALPHANUMERIC) String title;

    @Test
    public void shouldThrowExceptionIfLevelFormatInvalid() throws Exception {
        // Given a line with an invalid level format
        String line = "";

        // When the level line is processed
        try {
            parser.parseLevelLine(parserData, line, lineNumber);

            // Then an exception is thrown with an appropriate error
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            assertThat(e.getMessage(), containsString("Invalid Chapter/Section/Appendix/Part/Preface! Incorrect format."));
            // And the line number is included
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldReturnEmptyLevelForType() throws Exception {
        // Given a valid line with a valid level type
        LevelType levelType = selectRandomLevelType();
        String line = levelType.getTitle() + ": " + title;

        // When the level line is processed
        Level result = parser.parseLevelLine(parserData, line, lineNumber);

        // Then the result should be a level of that type
        assertThat(result.getLevelType(), is(levelType));
    }

    @Test
    public void shouldReturnEmptyLevelOfTypeIfParsingProblem() throws Exception {
        // Given a line with a valid level type but invalid other contents
        ArrayList<LevelType> levelTypes = new ArrayList<LevelType>(asList(LevelType.values()));
        levelTypes.remove(LevelType.BASE);
        LevelType levelType = selectRandomListItem(levelTypes);
        String line = levelType.getTitle() + ": " + "[";

        // When the level line is processed
        Level result = parser.parseLevelLine(parserData, line, lineNumber);

        // Then an empty level of that type is returned
        assertThat(result.getLevelType(), is(levelType));
        assertThat(result.getChildLevels().size(), is(0));
    }
}
