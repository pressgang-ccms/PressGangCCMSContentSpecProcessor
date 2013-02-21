package org.jboss.pressgang.ccms.contentspec.processor;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.exceptions.ParsingException;
import org.jboss.pressgang.ccms.contentspec.processor.utils.ProcessorUtilities;
import org.jboss.pressgang.ccms.utils.structures.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
@PrepareForTest({ProcessorUtilities.class})
public class ContentSpecParserProcessMetaDataTest extends ContentSpecParserTest {

    private final String MISSING_PARSING_EXCEPTION = "ParsingException not thrown";

    @Arbitrary Integer lineNumber;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String line;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String line2;
    @Mock ContentSpec contentSpec;

    protected Pair<String, String> keyValuePair = Pair.newPair(null, null);

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(ProcessorUtilities.class);
        when(ProcessorUtilities.getAndValidateKeyValuePair(anyString())).thenReturn(keyValuePair);
        super.setUp();
    }

    @Test
    public void shouldThrowParsingExceptionWhenEmptyLine() throws Exception {
        // Given an empty metadata line
        String emptyLine = "";
        when(ProcessorUtilities.getAndValidateKeyValuePair(anyString())).thenCallRealMethod();

        // When the metadata line is processed
        try {
            parser.processMetaDataLine(contentSpec, emptyLine, lineNumber);

            // Then an exception is thrown
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            // And it contains an error about the attribute format
            assertThat(e.getMessage(), containsString("Invalid Content Specification!"));
            assertThat(e.getMessage(), containsString("Incorrect attribute format."));
            // And the error message contains the line number
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldThrowExceptionWhenSpacesValueNotANumber() {
        // Given a line produces a key-value pair with a spaces value that is not an integer
        keyValuePair.setFirst("spaces");
        keyValuePair.setSecond("foo");

        // When the metadata line is processed
        try {
            parser.processMetaDataLine(contentSpec, line, lineNumber);

            // Then an exception is thrown
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            // And it contains an error about the number
            assertThat(e.getMessage(), containsString("Number expected but the value specified is not a valid number."));
            // And the error message contains the line number
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldSetIndentationSizeToTwoWhenValueSuppliedZero() throws Exception {
        // Given a line produces a key-value pair with a spaces value that is zero
        keyValuePair.setFirst("spaces");
        keyValuePair.setSecond("0");

        // When the metadata line is processed
        parser.processMetaDataLine(contentSpec, line, lineNumber);

        // Then the identation size is set to two spaces
        assertThat(parser.getIndentationSize(), is(2));
    }

    @Test
    public void shouldSetIndentationSizeToTwoWhenValueSuppliedNegative() throws Exception {
        // Given a line produces a key-value pair with a spaces value that is a negative number
        keyValuePair.setFirst("spaces");
        keyValuePair.setSecond("-1");

        // When the metadata line is processed
        parser.processMetaDataLine(contentSpec, line, lineNumber);

        // Then the identation size is set to two spaces
        assertThat(parser.getIndentationSize(), is(2));
    }

    @Test
    public void shouldLogWarningWhenDebugModeOutsideRange() throws Exception {
        // Given a line produces a key-value pair with debug value that is not 0, 1 or 2
        keyValuePair.setFirst("debug");
        keyValuePair.setSecond("3");

        // When the metadata line is processed
        parser.processMetaDataLine(contentSpec, line, lineNumber);

        // Then a warning is added to the logs
        assertThat(logger.getLogMessages().toString(), containsString("WARN:  Invalid debug setting. Debug must be set to 0, 1 or 2! So debug will be off by default."));
    }

    @Test
    public void shouldSetDebugLevelWhenValueZero() throws Exception {
        // Given a line produces a key-value pair with a debug value of 0
        keyValuePair.setFirst("debug");
        keyValuePair.setSecond("0");

        // When the metadata line is processed
        parser.processMetaDataLine(contentSpec, line, lineNumber);

        // Then the log verbosity is set to 0
        assertThat(logger.getDebugLevel(), is(0));
    }

    @Test
    public void shouldThrowExceptionWhenPublicanConfigEmpty() {
        // Given a line produces a key-value pair with an empty publican.cfg value
        keyValuePair.setFirst("publican.cfg");
        keyValuePair.setSecond("");

        // When the metadata line is processed
        try {
            parser.processMetaDataLine(contentSpec, line, lineNumber);

            // Then an exception is thrown
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            // And it contains an error about the config
            assertThat(e.getMessage(), containsString("Invalid Content Specification! Incorrect publican.cfg input."));
            // And the error message contains the line number
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldSetPublicanConfig() throws Exception {
        // Given a line produces a key-value pair with a publican.cfg key
        keyValuePair.setFirst("publican.cfg");
        // And a value containing both an opening and closing bracket
        keyValuePair.setSecond("[" + line + "]");

        // When the metadata line is processed
        parser.processMetaDataLine(contentSpec, line, lineNumber);

        // Then the publican config should be set
        ArgumentCaptor<String> publicanConfig = ArgumentCaptor.forClass(String.class);
        Mockito.verify(contentSpec, times(1)).setPublicanCfg(publicanConfig.capture());
        assertThat(publicanConfig.getValue(), containsString(line));
    }

    @Test
    public void shouldSearchSubsequentLinesForRemainingPublicanConfig() throws Exception {
        // Given a line produces a key-value pair with a publican.cfg key
        keyValuePair.setFirst("publican.cfg");
        // And a value containing only an opening bracket
        keyValuePair.setSecond("[" + line);
        // And the next line contains the closing bracket
        parser.getLines().push(line2 + "]");
        // And the current line count
        int originalLineCount = parser.getLineCount();

        // When the metadata line is processed
        parser.processMetaDataLine(contentSpec, line, lineNumber);

        // Then the line count should be incremented
        assertThat(parser.getLineCount(), is(originalLineCount+1));
        // And the publican config should be set
        ArgumentCaptor<String> publicanConfig = ArgumentCaptor.forClass(String.class);
        Mockito.verify(contentSpec, times(1)).setPublicanCfg(publicanConfig.capture());
        assertThat(publicanConfig.getValue(), containsString(line));
        assertThat(publicanConfig.getValue(), containsString(line2));
    }
}
