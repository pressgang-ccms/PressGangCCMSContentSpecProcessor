package org.jboss.pressgang.ccms.contentspec.processor;

import net.sf.ipsedixit.annotation.Arbitrary;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.exceptions.ParsingException;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
public class ContentSpecParserProcessMetaDataTest extends ContentSpecParserTest {

    @Arbitrary Integer lineNumber;
    @Mock ContentSpec contentSpec;

    @Test
    public void shouldThrowParsingExceptionWhenEmptyLine() throws Exception {
        // Given an empty metadata line
        String line = "";

        // When the metadata line is processed
        try {
            parser.processMetaDataLine(contentSpec, line, lineNumber);

            // Then an exception is thrown
            fail("ParsingException not thrown");
        } catch (ParsingException e) {
            // And it contains an error about the attribute format
            assertThat(e.getMessage(), containsString("Invalid Content Specification!"));
            assertThat(e.getMessage(), containsString("Incorrect attribute format."));
            // And the error message contains the line number
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

}
