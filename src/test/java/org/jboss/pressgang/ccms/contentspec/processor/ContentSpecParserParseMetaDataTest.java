package org.jboss.pressgang.ccms.contentspec.processor;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static net.sf.ipsedixit.core.StringType.ALPHANUMERIC;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyChar;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import org.hamcrest.Matchers;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.FileList;
import org.jboss.pressgang.ccms.contentspec.KeyValueNode;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.entities.InjectionOptions;
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

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
@PrepareForTest({ProcessorUtilities.class})
public class ContentSpecParserParseMetaDataTest extends ContentSpecParserTest {

    @Arbitrary Integer lineNumber;
    @Arbitrary Integer lineNumber2;
    @Arbitrary Integer id;
    @Arbitrary Integer revision;
    @ArbitraryString(type = ALPHANUMERIC) String line;
    @ArbitraryString(type = ALPHANUMERIC) String line2;
    @ArbitraryString(type = ALPHANUMERIC) String type;
    @ArbitraryString(type = ALPHANUMERIC) String type2;

    @Mock ContentSpec contentSpec;
    protected Pair<String, String> keyValuePair = Pair.newPair(null, null);
    protected Pair<String, String> keyValuePair2 = Pair.newPair(null, null);

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(ProcessorUtilities.class);
        when(ProcessorUtilities.getAndValidateKeyValuePair(anyString())).thenReturn(keyValuePair, keyValuePair2);
        when(ProcessorUtilities.replaceEscapeChars(anyString())).thenCallRealMethod();
        when(ProcessorUtilities.findVariableSet(anyString(), anyChar(), anyChar(), anyInt())).thenCallRealMethod();
        super.setUp();
        line = line.replace("[", "\\[");
        parserData.setContentSpec(contentSpec);
    }

    @Test
    public void shouldThrowParsingExceptionWhenEmptyLine() throws Exception {
        // Given an empty metadata line
        String emptyLine = "";
        when(ProcessorUtilities.getAndValidateKeyValuePair(anyString())).thenCallRealMethod();

        // When the metadata line is processed
        try {
            parser.parseMetaDataLine(parserData, emptyLine, lineNumber);

            // Then an exception is thrown
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            // And it contains an error about the attribute format
            assertThat(e.getMessage(), containsString("Invalid Content Specification!"));
            assertThat(e.getMessage(), containsString("Incorrect metadata format."));
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
            parser.parseMetaDataLine(parserData, line, lineNumber);

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
        parser.parseMetaDataLine(parserData, line, lineNumber);

        // Then the identation size is set to two spaces
        assertThat(parserData.getIndentationSize(), is(2));
    }

    @Test
    public void shouldSetIndentationSizeToTwoWhenValueSuppliedNegative() throws Exception {
        // Given a line produces a key-value pair with a spaces value that is a negative number
        keyValuePair.setFirst("spaces");
        keyValuePair.setSecond("-1");

        // When the metadata line is processed
        parser.parseMetaDataLine(parserData, line, lineNumber);

        // Then the identation size is set to two spaces
        assertThat(parserData.getIndentationSize(), is(2));
    }

    @Test
    public void shouldLogWarningWhenDebugModeOutsideRange() throws Exception {
        // Given a line produces a key-value pair with debug value that is not 0, 1 or 2
        keyValuePair.setFirst("debug");
        keyValuePair.setSecond("3");

        // When the metadata line is processed
        parser.parseMetaDataLine(parserData, line, lineNumber);

        // Then a warning is added to the logs
        assertThat(logger.getLogMessages().toString(), containsString("WARN:  Invalid debug setting. Debug must be set to 0, 1 or 2! So debug will be off by default."));
    }

    @Test
    public void shouldSetDebugLevelWhenValueZero() throws Exception {
        // Given a line produces a key-value pair with a debug value of 0
        keyValuePair.setFirst("debug");
        keyValuePair.setSecond("0");

        // When the metadata line is processed
        parser.parseMetaDataLine(parserData, line, lineNumber);

        // Then the log verbosity is set to 0
        assertThat(logger.getDebugLevel(), is(0));
    }

    @Test
    public void shouldSetPublicanConfig() throws Exception {
        // Given a line produces a key-value pair with a publican.cfg key
        keyValuePair.setFirst("publican.cfg");
        // And a value containing both an opening and closing bracket
        keyValuePair.setSecond("[" + line + "]");

        // When the metadata line is processed
        parser.parseMetaDataLine(parserData, line, lineNumber);

        // Then the publican config should be set
        ArgumentCaptor<KeyValueNode> publicanConfig = ArgumentCaptor.forClass(KeyValueNode.class);
        Mockito.verify(contentSpec, times(1)).appendKeyValueNode(publicanConfig.capture());
        assertThat((String) publicanConfig.getValue().getValue(), is(line));
    }

    @Test
    public void shouldSetPublicanConfigWithEmptyData() throws Exception {
        // Given a line produces a key-value pair with a publican.cfg key
        keyValuePair.setFirst("publican.cfg");
        // And a value containing both an opening and closing bracket
        keyValuePair.setSecond("[]");

        // When the metadata line is processed
        parser.parseMetaDataLine(parserData, line, lineNumber);

        // Then the publican config should be set
        ArgumentCaptor<KeyValueNode> publicanConfig = ArgumentCaptor.forClass(KeyValueNode.class);
        Mockito.verify(contentSpec, times(1)).appendKeyValueNode(publicanConfig.capture());
        assertThat((String) publicanConfig.getValue().getValue(), is(""));
    }

    @Test
    public void shouldSearchSubsequentLinesForRemainingPublicanConfig() throws Exception {
        // Given a line produces a key-value pair with a publican.cfg key
        keyValuePair.setFirst("publican.cfg");
        // And a value containing only an opening bracket
        keyValuePair.setSecond("[" + line);
        // And the next line contains the closing bracket
        parserData.getLines().push(line2 + "]\n");
        // And the current line count
        int originalLineCount = parserData.getLineCount();

        // When the metadata line is processed
        parser.parseMetaDataLine(parserData, line, lineNumber);

        // Then the line count should be incremented
        assertThat(parserData.getLineCount(), is(originalLineCount + 1));
        // And the publican config should be set
        ArgumentCaptor<KeyValueNode> publicanConfig = ArgumentCaptor.forClass(KeyValueNode.class);
        Mockito.verify(contentSpec, times(1)).appendKeyValueNode(publicanConfig.capture());
        assertThat((String) publicanConfig.getValue().getValue(), is(line + "\n" + line2));
    }

    @Test
    public void shouldThrowExceptionIfPublicanConfigEndNotFound() throws Exception {
        // Given a line produces a key-value pair with a publican.cfg key
        keyValuePair.setFirst("publican.cfg");
        // And a value containing only an opening bracket
        keyValuePair.setSecond("[" + line);

        // When the metadata line is processed
        try {
            parser.parseMetaDataLine(parserData, line, lineNumber);

            // Then an exception is thrown
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            // And it contains an error about the config
            assertThat(e.getMessage(), containsString("Invalid Content Specification! Incorrect multiple line metadata format."));
            // And the error message contains the line number
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldThrowExceptionIfPublicanDelimitersDoNotMatch() throws Exception {
        // Given a line produces a key-value pair with a publican.cfg key
        keyValuePair.setFirst("publican.cfg");
        // And a value containing mismatched brackets
        keyValuePair.setSecond("[" + line + "[" + line + "]");

        // When the metadata line is processed
        try {
            parser.parseMetaDataLine(parserData, line, lineNumber);

            // Then an exception is thrown
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            // And it contains an error about the config
            assertThat(e.getMessage(), containsString("Invalid Content Specification! Incorrect multiple line metadata format."));
            // And the error message contains the line number
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldThrowExceptionIfInjectionOptionDelimitersMismatched() throws Exception {
        // Given a line produces a key-value pair with an inline injection key
        keyValuePair.setFirst("inline injection");
        // And a value containing an opening bracket but no closing bracket
        keyValuePair.setSecond("[" + type);

        try {
            parser.parseMetaDataLine(parserData, line, lineNumber);

            // Then an exception is thrown
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            // And it contains an error about the bracket
            assertThat(e.getMessage(), containsString("Invalid Content Specification! Missing ending bracket (]) detected."));
            // And the error message contains the line number
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldThrowExceptionIfNoInlineInjectionFlag() throws Exception {
        // Given a line produces a key-value pair for an inline injection key
        keyValuePair.setFirst("inline injection");
        // And a type with matching brackets
        keyValuePair.setSecond("[" + type + "]");

        try {
            parser.parseMetaDataLine(parserData, line, lineNumber);

            // Then an exception is thrown
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            // And it contains an error about the bracket
            assertThat(e.getMessage(), containsString("Invalid Content Specification! The setting for inline injection must be On or Off."));
            // And the error message contains the line number
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldSetInjectionOptionsContentSpecTypeToOff() throws Exception {
        // Given a line produces a key-value pair with an inline injection key
        keyValuePair.setFirst("inline injection");
        // And a value with matching brackets that contains the off flag
        keyValuePair.setSecond("off[" + type + "]");

        // When the metadata line is processed
        parser.parseMetaDataLine(parserData, line, lineNumber);

        // Then the injection options content spec type is set to off
        ArgumentCaptor<InjectionOptions> injectionOptions = ArgumentCaptor.forClass(InjectionOptions.class);
        Mockito.verify(contentSpec, times(1)).setInjectionOptions(injectionOptions.capture());
        assertThat(injectionOptions.getValue().getContentSpecType(), is(InjectionOptions.UserType.OFF));
    }

    @Test
    public void shouldSetInjectionOptionsContentSpecTypeToOnWhenNoTypes() throws Exception {
        // Given a line produces a key-value pair with an inline injection key
        keyValuePair.setFirst("inline injection");
        // And a value that only contains on
        keyValuePair.setSecond("on");

        // When the metadata line is processed
        parser.parseMetaDataLine(parserData, line, lineNumber);

        // Then the injection options content spec type is set to on
        ArgumentCaptor<InjectionOptions> injectionOptions = ArgumentCaptor.forClass(InjectionOptions.class);
        Mockito.verify(contentSpec, times(1)).setInjectionOptions(injectionOptions.capture());
        assertThat(injectionOptions.getValue().getContentSpecType(), is(InjectionOptions.UserType.ON));
    }

    @Test
    public void shouldSetInjectionOptionsContentSpecTypeToStrictWhenOnWithTypes() throws Exception {
        // Given a line produces a key-value pair with an inline injection key
        keyValuePair.setFirst("inline injection");
        // And a value that contains the on flag and a type
        keyValuePair.setSecond("on[" + type + "]");

        // When the metadata line is processed
        parser.parseMetaDataLine(parserData, line, lineNumber);

        // Then the injection options content spec type is set to strict
        ArgumentCaptor<InjectionOptions> injectionOptions = ArgumentCaptor.forClass(InjectionOptions.class);
        Mockito.verify(contentSpec, times(1)).setInjectionOptions(injectionOptions.capture());
        assertThat(injectionOptions.getValue().getContentSpecType(), is(InjectionOptions.UserType.STRICT));
    }

    @Test
    public void shouldSetInjectionOptionsWithTopicTypes() throws Exception {
        // Given a line produces a key-value pair with an inline injection key
        keyValuePair.setFirst("inline injection");
        // And a value with an injection flag and some valid topic types
        keyValuePair.setSecond("off[" + type + "," + type2 + "]");

        // When the metadata line is processed
        parser.parseMetaDataLine(parserData, line, lineNumber);

        // Then the injection option topic types should be set
        ArgumentCaptor<InjectionOptions> injectionOptions = ArgumentCaptor.forClass(InjectionOptions.class);
        Mockito.verify(contentSpec, times(1)).setInjectionOptions(injectionOptions.capture());
        assertThat(injectionOptions.getValue().getStrictTopicTypes(), Matchers.contains(type, type2));
        // And the injection options content spec type is set to off
        assertThat(injectionOptions.getValue().getContentSpecType(), is(InjectionOptions.UserType.OFF));
    }

    @Test
    public void shouldAddOtherKeyValueNode() throws Exception {
        // Given a line produces a key-value pair that doesn't have a known key
        keyValuePair.setFirst(type);
        // And some value
        keyValuePair.setSecond(line);

        // When the metadata line is processed
        parser.parseMetaDataLine(parserData, line, lineNumber);

        // Then this pair should be added to the spec
        ArgumentCaptor<KeyValueNode> keyValueNode = ArgumentCaptor.forClass(KeyValueNode.class);
        Mockito.verify(contentSpec, times(1)).appendKeyValueNode(keyValueNode.capture());
        assertThat(keyValueNode.getValue().getKey(), is(type));
        assertThat(keyValueNode.getValue().getValue().toString(), is(line));
    }

    @Test
    public void shouldSetSpecTopicMetaData() throws Exception {
        // Given a line produces a key-value pair with a Revision History key
        keyValuePair.setFirst("Revision History");
        // And a value containing both an opening and closing bracket and an id
        keyValuePair.setSecond("[" + id.toString() + "]");
        given(ProcessorUtilities.findVariableSet(anyString(), anyChar(), anyChar(), anyInt())).willCallRealMethod();

        // When the metadata line is processed
        parser.parseMetaDataLine(parserData, line, lineNumber);

        // Then the revision history should be set
        ArgumentCaptor<KeyValueNode> revisionHistory = ArgumentCaptor.forClass(KeyValueNode.class);
        Mockito.verify(contentSpec, times(1)).appendKeyValueNode(revisionHistory.capture());
        final SpecTopic specTopic = (SpecTopic) revisionHistory.getValue().getValue();
        assertNotNull(specTopic);
        assertEquals(specTopic.getId(), id.toString());
    }

    @Test
    public void shouldThrowExceptionIfMissingOpeningBracketForSpecTopicMetaData() throws Exception {
        // Given a line produces a key-value pair with a publican.cfg key
        keyValuePair.setFirst("Revision History");
        // And a value containing only a closing bracket and an id
        keyValuePair.setSecond(id.toString() + "]");
        given(ProcessorUtilities.findVariableSet(anyString(), anyChar(), anyChar(), anyInt())).willCallRealMethod();

        // When the metadata line is processed
        try {
            parser.parseMetaDataLine(parserData, line, lineNumber);

            // Then an exception is thrown
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            // And it contains an error about the bracket
            assertThat(e.getMessage(), containsString("Invalid Content Specification! Missing opening bracket ([) detected."));
            // And the error message contains the line number
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldThrowExceptionIfMissingClosingBracketForSpecTopicMetaData() throws Exception {
        // Given a line produces a key-value pair with a publican.cfg key
        keyValuePair.setFirst("Revision History");
        // And a value containing only an opening bracket and an id
        keyValuePair.setSecond("[" + id.toString());
        given(ProcessorUtilities.findVariableSet(anyString(), anyChar(), anyChar(), anyInt())).willCallRealMethod();

        // When the metadata line is processed
        try {
            parser.parseMetaDataLine(parserData, line, lineNumber);

            // Then an exception is thrown
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            // And it contains an error about the bracket
            assertThat(e.getMessage(), containsString("Invalid Content Specification! Missing ending bracket (]) detected."));
            // And the error message contains the line number
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldThrowExceptionIfMissingBracketsForSpecTopicMetaData() throws Exception {
        // Given a line produces a key-value pair with a publican.cfg key
        keyValuePair.setFirst("Revision History");
        // And a value with an id and missing an opening and closing bracket
        keyValuePair.setSecond(id.toString());
        given(ProcessorUtilities.findVariableSet(anyString(), anyChar(), anyChar(), anyInt())).willCallRealMethod();

        // When the metadata line is processed
        try {
            parser.parseMetaDataLine(parserData, line, lineNumber);

            // Then an exception is thrown
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            // And it contains an error about the bracket
            assertThat(e.getMessage(), containsString("Invalid Content Specification! Missing brackets [] detected."));
            // And the error message contains the line number
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldParseShortFiles() throws Exception {
        // Given a line produces a key-value pair with a additional files key
        keyValuePair.setFirst("Additional Files");
        // And a value containing both an opening and closing bracket
        keyValuePair.setSecond("[" + id + "]");

        // When the metadata line is processed
        parser.parseMetaDataLine(parserData, line, lineNumber);

        // Then the files should be set
        ArgumentCaptor<FileList> files = ArgumentCaptor.forClass(FileList.class);
        Mockito.verify(contentSpec, times(1)).appendKeyValueNode(files.capture());
        assertThat(files.getValue().getValue().size(), is(1));
        final FileList fileList = files.getValue();
        assertThat(fileList.getValue().get(0).getId(), is(id));
    }

    @Test
    public void shouldParseLongFiles() throws Exception {
        // Given a line produces a key-value pair with a additional files key
        keyValuePair.setFirst("Additional Files");
        // And a value containing both an opening and closing bracket
        keyValuePair.setSecond("[" + line  + " [" + id + ", rev: " + revision + "]]");

        // When the metadata line is processed
        parser.parseMetaDataLine(parserData, line, lineNumber);

        // Then the files should be set
        ArgumentCaptor<FileList> files = ArgumentCaptor.forClass(FileList.class);
        Mockito.verify(contentSpec, times(1)).appendKeyValueNode(files.capture());
        assertThat(files.getValue().getValue().size(), is(1));
        final FileList fileList = files.getValue();
        assertThat(fileList.getValue().get(0).getId(), is(id));
        assertThat(fileList.getValue().get(0).getTitle(), is(line));
        assertThat(fileList.getValue().get(0).getRevision(), is(revision));
    }

    @Test
    public void shouldThrowExceptionIfFilesEndNotFound() throws Exception {
        // Given a line produces a key-value pair with a additional files key
        keyValuePair.setFirst("Additional Files");
        // And a value containing only an opening bracket
        keyValuePair.setSecond("[" + id);

        // When the metadata line is processed
        try {
            parser.parseMetaDataLine(parserData, line, lineNumber);

            // Then an exception is thrown
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            // And it contains an error about the config
            assertThat(e.getMessage(), containsString("Invalid Content Specification! Missing ending bracket (]) detected."));
            // And the error message contains the line number
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldThrowExceptionIfSingleLongFileEndNotFound() throws Exception {
        // Given a line produces a key-value pair with a additional files key
        keyValuePair.setFirst("Additional Files");
        // And a value containing only an opening bracket
        keyValuePair.setSecond("[" + line + " [" + id + "]");

        // When the metadata line is processed
        try {
            parser.parseMetaDataLine(parserData, line, lineNumber);

            // Then an exception is thrown
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            // And it contains an error about the config
            assertThat(e.getMessage(), containsString("Invalid Content Specification! Missing ending bracket (]) detected."));
            // And the error message contains the line number
            assertThat(e.getMessage(), containsString(lineNumber.toString()));
        }
    }

    @Test
    public void shouldSearchSubsequentLinesForRemainingFiles() throws Exception {
        // Given a line produces a key-value pair with an additional files key
        keyValuePair.setFirst("Additional Files");
        // And a value containing only an opening bracket
        keyValuePair.setSecond("[" + line + " [" + id + "],");
        // And the next line contains the closing bracket
        parserData.getLines().push(line2 + " [" + revision + "]]\n");
        // And the current line count
        int originalLineCount = parserData.getLineCount();

        // When the metadata line is processed
        parser.parseMetaDataLine(parserData, line, lineNumber);

        // Then the line count should be incremented
        assertThat(parserData.getLineCount(), is(originalLineCount + 1));
        // And the files should be set
        ArgumentCaptor<FileList> files = ArgumentCaptor.forClass(FileList.class);
        Mockito.verify(contentSpec, times(1)).appendKeyValueNode(files.capture());
        assertThat(files.getValue().getValue().size(), is(2));
        final FileList fileList = files.getValue();
        assertThat(fileList.getValue().get(0).getId(), is(id));
        assertThat(fileList.getValue().get(0).getTitle(), is(line));
        assertThat(fileList.getValue().get(1).getId(), is(revision));
        assertThat(fileList.getValue().get(1).getTitle(), is(line2));
    }

    @Test
    public void shouldThrowExceptionWhenDuplicateMetaData() {
        // Given twos lines that are duplicate metadata
        keyValuePair.setFirst("Title");
        keyValuePair.setSecond(line);
        keyValuePair2.setFirst("TITLE");
        keyValuePair2.setSecond(line);

        try {
            parser.parseMetaDataLine(parserData, line, lineNumber);
            parser.parseMetaDataLine(parserData, line, lineNumber2);

            // Then an exception is thrown
            fail(MISSING_PARSING_EXCEPTION);
        } catch (ParsingException e) {
            // And it contains an error about the duplicate entry
            assertThat(e.getMessage(), containsString("Invalid metadata, \"TITLE\" has already been defined."));
            // And the error message contains the line number
            assertThat(e.getMessage(), containsString(lineNumber2.toString()));
        }
    }
}
