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

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.jboss.pressgang.ccms.utils.common.StringUtilities.isStringNullOrEmpty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;
import org.jboss.pressgang.ccms.contentspec.Appendix;
import org.jboss.pressgang.ccms.contentspec.Chapter;
import org.jboss.pressgang.ccms.contentspec.CommonContent;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.File;
import org.jboss.pressgang.ccms.contentspec.FileList;
import org.jboss.pressgang.ccms.contentspec.IOptionsNode;
import org.jboss.pressgang.ccms.contentspec.ITopicNode;
import org.jboss.pressgang.ccms.contentspec.InfoTopic;
import org.jboss.pressgang.ccms.contentspec.InitialContent;
import org.jboss.pressgang.ccms.contentspec.KeyValueNode;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.Part;
import org.jboss.pressgang.ccms.contentspec.Process;
import org.jboss.pressgang.ccms.contentspec.Section;
import org.jboss.pressgang.ccms.contentspec.SpecNode;
import org.jboss.pressgang.ccms.contentspec.SpecNodeWithRelationships;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.TextNode;
import org.jboss.pressgang.ccms.contentspec.constants.CSConstants;
import org.jboss.pressgang.ccms.contentspec.entities.InjectionOptions;
import org.jboss.pressgang.ccms.contentspec.entities.Relationship;
import org.jboss.pressgang.ccms.contentspec.enums.LevelType;
import org.jboss.pressgang.ccms.contentspec.enums.RelationshipType;
import org.jboss.pressgang.ccms.contentspec.exceptions.IndentationException;
import org.jboss.pressgang.ccms.contentspec.exceptions.ParsingException;
import org.jboss.pressgang.ccms.contentspec.processor.constants.ProcessorConstants;
import org.jboss.pressgang.ccms.contentspec.processor.enums.ParserType;
import org.jboss.pressgang.ccms.contentspec.processor.exceptions.InvalidKeyValueException;
import org.jboss.pressgang.ccms.contentspec.processor.structures.ParserResults;
import org.jboss.pressgang.ccms.contentspec.processor.structures.VariableSet;
import org.jboss.pressgang.ccms.contentspec.processor.utils.ProcessorUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.ContentSpecUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLogger;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.provider.ServerSettingsProvider;
import org.jboss.pressgang.ccms.provider.TopicProvider;
import org.jboss.pressgang.ccms.utils.common.CollectionUtilities;
import org.jboss.pressgang.ccms.utils.common.StringUtilities;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;
import org.jboss.pressgang.ccms.utils.structures.Pair;

/**
 * A class that parses a Content Specification and stores the parsed data into a ContentSpec Object. The Object then
 * contains all of the
 * Levels, Topics and relationships to be passed for validation or saving.
 *
 * @author lnewson
 * @author alabbas
 */
public class ContentSpecParser {
    private static final Pattern LEVEL_PATTERN = Pattern.compile(ProcessorConstants.LEVEL_REGEX);
    private static final Pattern LEVEL_INITIAL_CONTENT_PATTERN = Pattern.compile(ProcessorConstants.LEVEL_INITIAL_CONTENT_REGEX);
    private static final Pattern SQUARE_BRACKET_PATTERN = Pattern.compile(format(ProcessorConstants.BRACKET_NAMED_PATTERN, '[', ']'));
    private static final Pattern RELATION_ID_LONG_PATTERN = Pattern.compile(ProcessorConstants.RELATION_ID_LONG_PATTERN);
    private static final Pattern FILE_ID_LONG_PATTERN = Pattern.compile(ProcessorConstants.FILE_ID_LONG_PATTERN);
    private static final Pattern COMMON_CONTENT_PATTERN = Pattern.compile("^.*\\[\\s*(?i)Common\\s+Content.*$");

    /**
     * An Enumerator used to specify the parsing mode of the Parser.
     */
    public static enum ParsingMode {
        /**
         * The Content Spec should be Parsed as a new Content Spec.
         */
        NEW,
        /**
         * The Content Spec should be Parsed as an edited Content Spec.
         */
        EDITED,
        /**
         * The Parser shouldn't care if the Content Spec is new or edited.
         */
        EITHER
    }

    private final DataProviderFactory providerFactory;
    private final TopicProvider topicProvider;
    private final ServerSettingsProvider serverSettingsProvider;
    private final ErrorLogger log;
    private final ErrorLoggerManager loggerManager;

    /**
     * Constructor
     *
     * @param providerFactory The Factory to produce various different Entity DataProviders.
     * @param loggerManager   The Logging Manager that contains any errors/warnings produced while parsing.
     */
    public ContentSpecParser(final DataProviderFactory providerFactory, final ErrorLoggerManager loggerManager) {
        this.providerFactory = providerFactory;
        topicProvider = providerFactory.getProvider(TopicProvider.class);
        serverSettingsProvider = providerFactory.getProvider(ServerSettingsProvider.class);
        this.loggerManager = loggerManager;
        log = loggerManager.getLogger(ContentSpecParser.class);
    }

    /**
     * Parse a Content Specification to put the string into usable objects that can then be validate.
     *
     * @param contentSpec A string representation of the Content Specification.
     * @return True if everything was parsed successfully otherwise false.
     */
    public ParserResults parse(final String contentSpec) {
        return parse(contentSpec, ParsingMode.EITHER);
    }

    /**
     * Parse a Content Specification to put the string into usable objects that can then be validate.
     *
     * @param contentSpec A string representation of the Content Specification.
     * @param mode        The mode in which the Content Specification should be parsed.
     * @return True if everything was parsed successfully otherwise false.
     */
    public ParserResults parse(final String contentSpec, final ParsingMode mode) {
        return parse(contentSpec, mode, false);
    }

    /**
     * Parse a Content Specification to put the string into usable objects that can then be validate.
     *
     * @param contentSpec      A string representation of the Content Specification.
     * @param mode             The mode in which the Content Specification should be parsed.
     * @param processProcesses Whether or not processes should call the data provider to be processed.
     * @return True if everything was parsed successfully otherwise false.
     */
    public ParserResults parse(final String contentSpec, final ParsingMode mode, final boolean processProcesses) {
        final ParserData parserData = new ParserData();

        // Read in the file contents
        final BufferedReader br = new BufferedReader(new StringReader(contentSpec));
        try {
            readFileData(parserData, br);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Process the spec contents.
        return processSpec(parserData, mode, processProcesses);
    }

    /**
     * Reset all of the variables used during parsing.
     */
    protected void reset() {
        // Clear the logs
        log.clearLogs();
    }

    /**
     * Reads the data from a file that is passed into a BufferedReader and processes it accordingly.
     *
     * @param parserData
     * @param br         A BufferedReader object that has been initialised with a file's data.
     * @throws IOException Thrown if an IO Error occurs while reading from the BufferedReader.
     */
    protected void readFileData(final ParserData parserData, final BufferedReader br) throws IOException {
        // Read in the entire file so we can peek ahead later on
        String line;
        while ((line = br.readLine()) != null) {
            parserData.addLine(line);
        }
    }

    /**
     * Starting method to process a Content Specification string into a ContentSpec object.
     *
     * @param parserData
     * @param mode             The mode to parse the string as.
     * @param processProcesses Whether or not processes should call the data provider to be processed.
     * @return True if the content spec was processed successfully otherwise false.
     */
    protected ParserResults processSpec(final ParserData parserData, final ParsingMode mode, final boolean processProcesses) {
        // Find the first line that isn't a blank line or a comment
        while (parserData.getLines().peek() != null) {
            final String input = parserData.getLines().peek();
            if (isCommentLine(input) || isBlankLine(input)) {
                parserData.setLineCount(parserData.getLineCount() + 1);

                if (isCommentLine(input)) {
                    parserData.getContentSpec().appendComment(input);
                } else if (isBlankLine(input)) {
                    parserData.getContentSpec().appendChild(new TextNode("\n"));
                }

                parserData.getLines().poll();
                continue;
            } else {
                // We've found the first line so break the loop
                break;
            }
        }

        // Process the content spec depending on the mode
        if (mode == ParsingMode.NEW) {
            return processNewSpec(parserData, processProcesses);
        } else if (mode == ParsingMode.EDITED) {
            return processEditedSpec(parserData, processProcesses);
        } else {
            return processEitherSpec(parserData, processProcesses);
        }
    }

    /**
     * Process a New Content Specification. That is that it should start with a Title, instead of a CHECKSUM and ID.
     *
     * @param parserData
     * @param processProcesses If processes should be processed to populate the relationships.
     * @return True if the content spec was processed successfully otherwise false.
     */
    protected ParserResults processNewSpec(final ParserData parserData, final boolean processProcesses) {
        final String input = parserData.getLines().poll();

        parserData.setLineCount(parserData.getLineCount() + 1);
        try {
            final Pair<String, String> keyValuePair = ProcessorUtilities.getAndValidateKeyValuePair(input);
            final String key = keyValuePair.getFirst();
            final String value = keyValuePair.getSecond();

            if (key.equalsIgnoreCase(CommonConstants.CS_TITLE_TITLE)) {
                parserData.getContentSpec().setTitle(value);

                // Process the rest of the spec now that we know the start is correct
                return processSpecContents(parserData, processProcesses);
            } else if (key.equalsIgnoreCase(CommonConstants.CS_CHECKSUM_TITLE)) {
                log.error(ProcessorConstants.ERROR_INCORRECT_NEW_MODE_MSG);
                return new ParserResults(false, null);
            } else {
                log.error(ProcessorConstants.ERROR_INCORRECT_FILE_FORMAT_MSG);
                return new ParserResults(false, null);
            }
        } catch (InvalidKeyValueException e) {
            log.error(ProcessorConstants.ERROR_INCORRECT_FILE_FORMAT_MSG, e);
            return new ParserResults(false, null);
        }
    }

    /**
     * Process an Edited Content Specification. That is that it should start with a CHECKSUM and ID, instead of a Title.
     *
     * @param parserData
     * @param processProcesses If processes should be processed to populate the relationships.
     * @return True if the content spec was processed successfully otherwise false.
     */
    protected ParserResults processEditedSpec(final ParserData parserData, final boolean processProcesses) {
        final String input = parserData.getLines().poll();

        parserData.setLineCount(parserData.getLineCount() + 1);
        try {
            final Pair<String, String> keyValuePair = ProcessorUtilities.getAndValidateKeyValuePair(input);
            final String key = keyValuePair.getFirst();
            final String value = keyValuePair.getSecond();

            if (key.equalsIgnoreCase(CommonConstants.CS_CHECKSUM_TITLE)) {
                parserData.getContentSpec().setChecksum(value);

                // Read in the Content Spec ID
                final String specIdLine = parserData.getLines().poll();
                parserData.setLineCount(parserData.getLineCount() + 1);
                if (specIdLine != null) {
                    final Pair<String, String> specIdPair = ProcessorUtilities.getAndValidateKeyValuePair(specIdLine);
                    final String specIdKey = specIdPair.getFirst();
                    final String specIdValue = specIdPair.getSecond();
                    if (specIdKey.equalsIgnoreCase(CommonConstants.CS_ID_TITLE)) {
                        int contentSpecId;
                        try {
                            contentSpecId = Integer.parseInt(specIdValue);
                        } catch (NumberFormatException e) {
                            log.error(format(ProcessorConstants.ERROR_INVALID_CS_ID_FORMAT_MSG, specIdLine.trim()));
                            return new ParserResults(false, null);
                        }
                        parserData.getContentSpec().setId(contentSpecId);

                        return processSpecContents(parserData, processProcesses);
                    } else {
                        log.error(ProcessorConstants.ERROR_CS_NO_CHECKSUM_MSG);
                        return new ParserResults(false, null);
                    }
                } else {
                    log.error(ProcessorConstants.ERROR_INCORRECT_FILE_FORMAT_MSG);
                    return new ParserResults(false, null);
                }
            } else if (key.equalsIgnoreCase(CommonConstants.CS_ID_TITLE)) {
                int contentSpecId;
                try {
                    contentSpecId = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    log.error(format(ProcessorConstants.ERROR_INVALID_CS_ID_FORMAT_MSG, input.trim()));
                    return new ParserResults(false, null);
                }
                parserData.getContentSpec().setId(contentSpecId);

                return processSpecContents(parserData, processProcesses);
            } else {
                log.error(ProcessorConstants.ERROR_INCORRECT_EDIT_MODE_MSG);
                return new ParserResults(false, null);
            }
        } catch (InvalidKeyValueException e) {
            log.error(ProcessorConstants.ERROR_INCORRECT_FILE_FORMAT_MSG, e);
            return new ParserResults(false, null);
        }
    }

    /**
     * Process Content Specification that is either NEW or EDITED. That is that it should start with a CHECKSUM and ID or a Title.
     *
     * @param parserData
     * @param processProcesses If processes should be processed to populate the relationships.
     * @return True if the content spec was processed successfully otherwise false.
     */
    protected ParserResults processEitherSpec(final ParserData parserData, final boolean processProcesses) {
        try {
            final Pair<String, String> keyValuePair = ProcessorUtilities.getAndValidateKeyValuePair(parserData.getLines().peek());
            final String key = keyValuePair.getFirst();

            if (key.equalsIgnoreCase(CommonConstants.CS_CHECKSUM_TITLE) || key.equalsIgnoreCase(CommonConstants.CS_ID_TITLE)) {
                return processEditedSpec(parserData, processProcesses);
            } else {
                return processNewSpec(parserData, processProcesses);
            }
        } catch (InvalidKeyValueException e) {
            log.error(ProcessorConstants.ERROR_INCORRECT_FILE_FORMAT_MSG, e);
            return new ParserResults(false, null);
        }
    }

    /**
     * Process the contents of a content specification and parse it into a ContentSpec object.
     *
     * @param parserData
     * @param processProcesses If processes should be processed to populate the relationships.
     * @return True if the contents were processed successfully otherwise false.
     */
    protected ParserResults processSpecContents(ParserData parserData, final boolean processProcesses) {
        parserData.setCurrentLevel(parserData.getContentSpec().getBaseLevel());
        boolean error = false;
        while (parserData.getLines().peek() != null) {
            parserData.setLineCount(parserData.getLineCount() + 1);
            // Process the content specification and print an error message if an error occurs
            try {
                if (!parseLine(parserData, parserData.getLines().poll(), parserData.getLineCount())) {
                    error = true;
                }
            } catch (IndentationException e) {
                log.error(e.getMessage());
                return new ParserResults(false, null);
            }
        }

        // Before validating the content specification, processes should be loaded first so that the
        // relationships and targets are created
        if (processProcesses) {
            for (final Process process : parserData.getProcesses()) {
                process.processTopics(parserData.getSpecTopics(), parserData.getTargetTopics(), topicProvider, serverSettingsProvider);
            }
        }

        // Setup the relationships
        processRelationships(parserData);

        return new ParserResults(!error, parserData.getContentSpec());
    }

    /**
     * Processes a line of the content specification and stores it in objects
     *
     * @param parserData
     * @param line       A line of input from the content specification
     * @return True if the line of input was processed successfully otherwise false.
     * @throws IndentationException Thrown if any invalid indentation occurs.
     */
    protected boolean parseLine(final ParserData parserData, final String line, int lineNumber) throws IndentationException {
        assert line != null;

        // Trim the whitespace
        final String trimmedLine = line.trim();

        // If the line is a blank or a comment, then nothing needs processing. So add the line and return
        if (isBlankLine(trimmedLine) || isCommentLine(trimmedLine)) {
            return parseEmptyOrCommentLine(parserData, line);
        } else {
            // Calculate the lines indentation level
            int lineIndentationLevel = calculateLineIndentationLevel(parserData, line, lineNumber);
            if (lineIndentationLevel > parserData.getIndentationLevel()) {
                // The line was indented without a new level so throw an error
                throw new IndentationException(format(ProcessorConstants.ERROR_INCORRECT_INDENTATION_MSG, lineNumber, trimmedLine));
            } else if (lineIndentationLevel < parserData.getIndentationLevel()) {
                // The line has left the previous level so move the current level up to the right level
                Level newCurrentLevel = parserData.getCurrentLevel();
                for (int i = (parserData.getIndentationLevel() - lineIndentationLevel); i > 0; i--) {
                    if (newCurrentLevel.getParent() != null) {
                        newCurrentLevel = newCurrentLevel.getParent();
                    }
                }
                changeCurrentLevel(parserData, newCurrentLevel, lineIndentationLevel);
            }

            // Process the line based on what type the line is
            try {
                if (isMetaDataLine(parserData, trimmedLine)) {
                    parseMetaDataLine(parserData, line, lineNumber);
                } else if (isCommonContentLine(trimmedLine)) {
                    final CommonContent commonContent = parseCommonContentLine(parserData, line, lineNumber);
                    parserData.getCurrentLevel().appendChild(commonContent);
                } else if (isLevelInitialContentLine(trimmedLine)) {
                    final Level initialContent = parseLevelLine(parserData, trimmedLine, lineNumber);
                    parserData.getCurrentLevel().appendChild(initialContent);

                    // Change the current level to use the new parsed level
                    changeCurrentLevel(parserData, initialContent, parserData.getIndentationLevel() + 1);
                } else if (isLevelLine(trimmedLine)) {
                    final Level level = parseLevelLine(parserData, trimmedLine, lineNumber);
                    if (level instanceof Process) {
                        parserData.getProcesses().add((Process) level);
                    }
                    parserData.getCurrentLevel().appendChild(level);

                    // Change the current level to use the new parsed level
                    changeCurrentLevel(parserData, level, parserData.getIndentationLevel() + 1);
//                } else if (trimmedLine.toUpperCase(Locale.ENGLISH).matches("^CS[ ]*:.*")) {
//                    processExternalLevelLine(getCurrentLevel(), line);
                } else if (StringUtilities.indexOf(trimmedLine,
                        '[') == 0 && parserData.getCurrentLevel().getLevelType() == LevelType.BASE) {
                    parseGlobalOptionsLine(parserData, line, lineNumber);
                } else {
                    // Process a new topic
                    final SpecTopic tempTopic = parseTopic(parserData, trimmedLine, lineNumber);

                    // Adds the topic to the current level
                    parserData.getCurrentLevel().appendSpecTopic(tempTopic);
                }
            } catch (ParsingException e) {
                log.error(e.getMessage());
                return false;
            }

            return true;
        }
    }

    /**
     * Calculates the indentation level of a line using the amount of whitespace and the parsers indentation size setting.
     *
     * @param parserData
     * @param line       The line to calculate the indentation for.
     * @return The lines indentation level.
     * @throws IndentationException Thrown if the indentation for the line isn't valid.
     */
    protected int calculateLineIndentationLevel(final ParserData parserData, final String line,
            int lineNumber) throws IndentationException {
        char[] lineCharArray = line.toCharArray();
        int indentationCount = 0;
        // Count the amount of whitespace characters before any text to determine the level
        if (Character.isWhitespace(lineCharArray[0])) {
            for (char c : lineCharArray) {
                if (Character.isWhitespace(c)) {
                    indentationCount++;
                } else {
                    break;
                }
            }
            if (indentationCount % parserData.getIndentationSize() != 0) {
                throw new IndentationException(format(ProcessorConstants.ERROR_INCORRECT_INDENTATION_MSG, lineNumber, line.trim()));
            }
        }

        return indentationCount / parserData.getIndentationSize();
    }

    /**
     * Checks to see if a line is represents a Content Specifications Meta Data.
     *
     * @param parserData
     * @param line       The line to be checked.
     * @return True if the line is meta data, otherwise false.
     */
    protected boolean isMetaDataLine(ParserData parserData, String line) {
        return parserData.getCurrentLevel().getLevelType() == LevelType.BASE && line.trim().matches("^\\w[\\w\\.\\s-]+=.*");
    }

    /**
     * Checks to see if a line is represents a Content Specifications Common Content node.
     *
     *
     * @param line       The line to be checked.
     * @return True if the line is a common content node, otherwise false.
     */
    protected boolean isCommonContentLine(String line) {
        return COMMON_CONTENT_PATTERN.matcher(line.trim()).matches();
    }

    /**
     * Checks to see if a line is a blank/empty line.
     *
     * @param line The line to be checked.
     * @return True if the line is an blank line, otherwise false.
     */
    protected boolean isBlankLine(String line) {
        return line.trim().isEmpty();
    }

    /**
     * Checks to see if a line is represents a Content Specifications comment.
     *
     * @param line The line to be checked.
     * @return True if the line is a comment, otherwise false.
     */
    protected boolean isCommentLine(String line) {
        return line.trim().startsWith("#");
    }

    /**
     * Checks to see if a line is represents a Content Specifications Level Front Matter.
     *
     * @param line The line to be checked.
     * @return True if the line is a front matter declaration, otherwise false.
     */
    protected boolean isLevelInitialContentLine(String line) {
        final Matcher matcher = LEVEL_INITIAL_CONTENT_PATTERN.matcher(line.trim().toUpperCase(Locale.ENGLISH));
        return matcher.find();
    }

    /**
     * Checks to see if a line is represents a Content Specifications Level.
     *
     * @param line The line to be checked.
     * @return True if the line is meta data, otherwise false.
     */
    protected boolean isLevelLine(String line) {
        final Matcher matcher = LEVEL_PATTERN.matcher(line.trim().toUpperCase(Locale.ENGLISH));
        return matcher.find();
    }

    /**
     * Processes a line that represents a comment or an empty line in a Content Specification.
     *
     * @param parserData
     * @param line       The line to be processed.
     * @return True if the line was processed without errors, otherwise false.
     */
    protected boolean parseEmptyOrCommentLine(final ParserData parserData, final String line) {
        if (isBlankLine(line)) {
            if (parserData.getCurrentLevel().getLevelType() == LevelType.BASE) {
                parserData.getContentSpec().appendChild(new TextNode("\n"));
            } else {
                parserData.getCurrentLevel().appendChild(new TextNode("\n"));
            }
            return true;
        } else {
            if (parserData.getCurrentLevel().getLevelType() == LevelType.BASE) {
                parserData.getContentSpec().appendComment(line);
            } else {
                parserData.getCurrentLevel().appendComment(line);
            }
            return true;
        }
    }

    /**
     * Processes a line that represents the start of a Content Specification Level. This method creates the level based on the data in
     * the line and then changes the current processing level to the new level.
     *
     * @param parserData
     * @param line       The line to be processed as a level.
     * @return True if the line was processed without errors, otherwise false.
     */
    protected Level parseLevelLine(final ParserData parserData, final String line, int lineNumber) throws ParsingException {
        String tempInput[] = StringUtilities.split(line, ':', 2);
        // Remove the whitespace from each value in the split array
        tempInput = CollectionUtilities.trimStringArray(tempInput);

        if (tempInput.length >= 1) {
            final LevelType levelType = LevelType.getLevelType(tempInput[0]);
            Level retValue = null;
            try {
                retValue = parseLevel(parserData, lineNumber, levelType, line);
            } catch (ParsingException e) {
                log.error(e.getMessage());
                // Create a basic level so the rest of the spec can be processed
                retValue = createEmptyLevelFromType(lineNumber, levelType, line);
                retValue.setUniqueId("L" + lineNumber);
            }

            parserData.getLevels().put(retValue.getUniqueId(), retValue);

            return retValue;
        } else {
            throw new ParsingException(format(ProcessorConstants.ERROR_LEVEL_FORMAT_MSG, lineNumber, line));
        }
    }

//    /**
//     * TODO The external level processing still needs to be implemented. DO NOT use this method at this time.
//     *
//     * @param line        The line to be processed.
//     */
//    protected void processExternalLevelLine(final Level currentLevel, final String line) throws ParsingException {
//        String splitVars[] = StringUtilities.split(line, ':', 2);
//        // Remove the whitespace from each value in the split array
//        splitVars = CollectionUtilities.trimStringArray(splitVars);
//
//        // Get the mapping of variables
//        HashMap<ParserType, String[]> variableMap = getLineVariables(splitVars[1], '[', ']', ',', false);
//        final String title = StringUtilities.replaceEscapeChars(getTitle(splitVars[1], '['));
//        processExternalLevel(currentLevel, variableMap.get(ParserType.EXTERNAL_CONTENT_SPEC)[0], title, line);
//    }

    /**
     * Processes a line that represents some Meta Data in a Content Specification.
     *
     * @param parserData
     * @param line       The line to be processed.
     * @throws ParsingException
     */
    protected void parseMetaDataLine(final ParserData parserData, final String line, int lineNumber) throws ParsingException {
        // Parse the line and break it up into the key/value pair
        Pair<String, String> keyValue = null;
        try {
            keyValue = ProcessorUtilities.getAndValidateKeyValuePair(line);
        } catch (InvalidKeyValueException e) {
            throw new ParsingException(format(ProcessorConstants.ERROR_INVALID_METADATA_FORMAT_MSG, lineNumber, line));
        }

        final String key = keyValue.getFirst();
        final String value = keyValue.getSecond();

        if (parserData.getParsedMetaDataKeys().contains(key.toLowerCase())) {
            throw new ParsingException(format(ProcessorConstants.ERROR_DUPLICATE_METADATA_FORMAT_MSG, lineNumber, key, line));
        } else {
            parserData.getParsedMetaDataKeys().add(key.toLowerCase());

            // first deal with metadata that is used by the parser or needs to be parsed further
            if (key.equalsIgnoreCase(CommonConstants.CS_SPACES_TITLE)) {
                // Read in the amount of spaces that were used for the content specification
                try {
                    parserData.setIndentationSize(Integer.parseInt(value));
                    if (parserData.getIndentationSize() <= 0) {
                        parserData.setIndentationSize(2);
                    }
                } catch (NumberFormatException e) {
                    throw new ParsingException(format(ProcessorConstants.ERROR_INVALID_NUMBER_MSG, lineNumber, line));
                }
            } else if (key.equalsIgnoreCase(CSConstants.DEBUG_TITLE)) {
                if (value.equals("1")) {
                    log.setVerboseDebug(1);
                } else if (value.equals("2")) {
                    log.setVerboseDebug(2);
                } else if (value.equals("0")) {
                    log.setVerboseDebug(0);
                } else {
                    log.warn(ProcessorConstants.WARN_DEBUG_IGNORE_MSG);
                }
            } else if (key.equalsIgnoreCase(CommonConstants.CS_INLINE_INJECTION_TITLE)) {
                final InjectionOptions injectionOptions = new InjectionOptions();
                String[] types = null;
                if (StringUtilities.indexOf(value, '[') != -1) {
                    if (StringUtilities.indexOf(value, ']') != -1) {
                        final Matcher matcher = SQUARE_BRACKET_PATTERN.matcher(value);

                        // Find all of the variables inside of the brackets defined by the regex
                        while (matcher.find()) {
                            final String topicTypes = matcher.group(ProcessorConstants.BRACKET_CONTENTS);
                            types = StringUtilities.split(topicTypes, ',');
                            for (final String type : types) {
                                injectionOptions.addStrictTopicType(type.trim());
                            }
                        }
                    } else {
                        throw new ParsingException(
                                format(ProcessorConstants.ERROR_NO_ENDING_BRACKET_MSG + ProcessorConstants.CSLINE_MSG, lineNumber, ']',
                                        line));
                    }
                }
                String injectionSetting = getTitle(value, '[');
                if (injectionSetting == null) {
                    throw new ParsingException(format(ProcessorConstants.ERROR_INVALID_INJECTION_MSG, lineNumber, line));
                } else if (injectionSetting.trim().equalsIgnoreCase("on")) {
                    if (types != null) {
                        injectionOptions.setContentSpecType(InjectionOptions.UserType.STRICT);
                    } else {
                        injectionOptions.setContentSpecType(InjectionOptions.UserType.ON);
                    }
                } else if (injectionSetting.trim().equalsIgnoreCase("off")) {
                    injectionOptions.setContentSpecType(InjectionOptions.UserType.OFF);
                } else {
                    throw new ParsingException(format(ProcessorConstants.ERROR_INVALID_INJECTION_MSG, lineNumber, line));
                }
                parserData.getContentSpec().setInjectionOptions(injectionOptions);
            } else if (key.equalsIgnoreCase(CommonConstants.CS_FILE_TITLE) || key.equalsIgnoreCase(CommonConstants.CS_FILE_SHORT_TITLE)) {
                final FileList files = parseFilesMetaData(parserData, value, lineNumber, line);
                parserData.getContentSpec().appendKeyValueNode(files);
            } else if (isSpecTopicMetaData(key, value)) {
                final SpecTopic specTopic = parseSpecTopicMetaData(parserData, value, key, lineNumber);
                parserData.getContentSpec().appendKeyValueNode(new KeyValueNode<SpecTopic>(key, specTopic, lineNumber));
            } else {
                try {
                    final KeyValueNode<String> node;
                    if (ContentSpecUtilities.isMetaDataMultiLine(key)) {
                        node = parseMultiLineMetaData(parserData, key, value, lineNumber);
                    } else {
                        node = new KeyValueNode<String>(key, value, lineNumber);
                    }
                    parserData.getContentSpec().appendKeyValueNode(node);
                } catch (NumberFormatException e) {
                    throw new ParsingException(format(ProcessorConstants.ERROR_INVALID_METADATA_FORMAT_MSG, lineNumber, line));
                }
            }
        }
    }

    /**
     * Checks if a metadata line is a spec topic.
     *
     * @param key   The metadata key.
     * @param value The metadata value.
     * @return True if the line is a spec topic metadata element, otherwise false.
     */
    private boolean isSpecTopicMetaData(final String key, final String value) {
        if (ContentSpecUtilities.isSpecTopicMetaData(key)) {
            // Abstracts can be plain text so check an opening bracket exists
            if (key.equalsIgnoreCase(CommonConstants.CS_ABSTRACT_TITLE) || key.equalsIgnoreCase(
                    CommonConstants.CS_ABSTRACT_ALTERNATE_TITLE)) {
                final String fixedValue = value.trim().replaceAll("(?i)^" + key + "\\s*", "");
                return fixedValue.trim().startsWith("[");
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Parses a multiple line metadata element.
     *
     * @param parserData
     * @param key        The metadata key.
     * @param value      The value on the metadata line.
     * @param lineNumber The initial line number.
     * @return The parsed multiple line value for the metadata element.
     * @throws ParsingException
     */
    private KeyValueNode<String> parseMultiLineMetaData(final ParserData parserData, final String key, final String value,
            final int lineNumber) throws ParsingException {
        // Check if the starting brace is found, if not then assume that there is only one line.
        int startingPos = StringUtilities.indexOf(value, '[');
        if (startingPos != -1) {
            final StringBuilder multiLineValue = new StringBuilder(value);
            // If the ']' character isn't on this line try the next line
            if (StringUtilities.indexOf(value, ']') == -1) {
                multiLineValue.append("\n");

                // Read the next line and increment counters
                String newLine = parserData.getLines().poll();
                while (newLine != null) {
                    multiLineValue.append(newLine).append("\n");
                    parserData.setLineCount(parserData.getLineCount() + 1);
                    // If the ']' character still isn't found keep trying
                    if (StringUtilities.lastIndexOf(multiLineValue.toString(), ']') == -1) {
                        newLine = parserData.getLines().poll();
                    } else {
                        break;
                    }
                }
            }

            // Check that the ']' character was found and that it was found before another '[' character
            final String finalMultiLineValue = multiLineValue.toString().trim();
            if (StringUtilities.lastIndexOf(finalMultiLineValue, ']') == -1 || StringUtilities.lastIndexOf(finalMultiLineValue,
                    '[') != startingPos) {
                throw new ParsingException(format(ProcessorConstants.ERROR_INVALID_MULTILINE_METADATA_MSG, lineNumber,
                        key + " = " + finalMultiLineValue.replaceAll("\n", "\n          ")));
            } else {
                final String finalValue = finalMultiLineValue.substring(1, finalMultiLineValue.length() - 1);
                return new KeyValueNode<String>(key, finalValue);
            }
        } else {
            return new KeyValueNode<String>(key, value, lineNumber);
        }
    }

    /**
     * TODO
     *
     * @param parserData
     * @param value
     * @param key
     * @return
     */
    private SpecTopic parseSpecTopicMetaData(final ParserData parserData, final String value, final String key,
            final int lineNumber) throws ParsingException {
        final String fixedValue = value.trim().replaceAll("(?i)^" + key + "\\s*", "");
        if (fixedValue.trim().startsWith("[") && fixedValue.trim().endsWith("]")) {
            final String topicString = key + " " + fixedValue.trim();
            return parseTopic(parserData, topicString, lineNumber);
        } else {
            if (fixedValue.trim().startsWith("[")) {
                throw new ParsingException(format(ProcessorConstants.ERROR_NO_ENDING_BRACKET_MSG, lineNumber, ']'));
            } else if (fixedValue.trim().endsWith("]")) {
                throw new ParsingException(format(ProcessorConstants.ERROR_NO_OPENING_BRACKET_MSG, lineNumber, '['));
            } else {
                throw new ParsingException(format(ProcessorConstants.ERROR_NO_BRACKET_MSG, lineNumber, '[', ']'));
            }
        }
    }

    /**
     * Parse an "Additional Files" metadata component into a List of {@link org.jboss.pressgang.ccms.contentspec.File} objects.
     *
     * @param parserData
     * @param value      The value of the key value pair
     * @param lineNumber The line number of the additional files key
     * @param line       The full line of the key value pair
     * @return A list of parsed File objects.
     * @throws ParsingException Thrown if an error occurs during parsing.
     */
    protected FileList parseFilesMetaData(final ParserData parserData, final String value, final int lineNumber,
            final String line) throws ParsingException {
        int startingPos = StringUtilities.indexOf(value, '[');
        if (startingPos != -1) {
            final List<File> files = new LinkedList<File>();
            final HashMap<ParserType, String[]> variables = getLineVariables(parserData, value, lineNumber, '[', ']', ',', true);
            final String[] vars = variables.get(ParserType.NONE);

            // Loop over each file found and parse it
            for (final String var : vars) {
                final File file = parseFileMetaData(var, lineNumber);
                if (file != null) {
                    files.add(file);
                }
            }

            return new FileList(CommonConstants.CS_FILE_TITLE, files, lineNumber);
        } else {
            throw new ParsingException(format(ProcessorConstants.ERROR_INVALID_FILES_MSG, lineNumber, line));
        }
    }

    /**
     * Parse a File MetaData component into a {@link org.jboss.pressgang.ccms.contentspec.File} object.
     *
     * @param line       The line to be parsed.
     * @param lineNumber The line number of the line being parsed.
     * @return A file object initialised with the data from the line.
     * @throws ParsingException Thrown if the line contains invalid content and couldn't be parsed.
     */
    protected File parseFileMetaData(final String line, final int lineNumber) throws ParsingException {
        final File file;
        if (line.matches(ProcessorConstants.FILE_ID_REGEX)) {
            file = new File(Integer.parseInt(line));
        } else if (FILE_ID_LONG_PATTERN.matcher(line).matches()) {
            final Matcher matcher = FILE_ID_LONG_PATTERN.matcher(line);

            matcher.find();
            final String id = matcher.group("ID");
            final String title = matcher.group("Title");
            final String rev = matcher.group("REV");
            file = new File(title.trim(), Integer.parseInt(id));

            if (rev != null) {
                file.setRevision(Integer.parseInt(rev));
            }
        } else {
            throw new ParsingException(format(ProcessorConstants.ERROR_INVALID_FILE_MSG, lineNumber));
        }

        return file;
    }

    /**
     * Processes a line that represents the Global Options for the Content Specification.
     *
     * @param parserData
     * @param line       The line to be processed.
     * @return True if the line was processed without errors, otherwise false.
     */
    protected boolean parseGlobalOptionsLine(final ParserData parserData, final String line, int lineNumber) throws ParsingException {
        // Read in the variables from the line
        final HashMap<ParserType, String[]> variableMap = getLineVariables(parserData, line, lineNumber, '[', ']', ',', false);

        // Check the read in values are valid
        if ((variableMap.size() > 1 && variableMap.containsKey(ParserType.NONE)) || (variableMap.size() > 0 && !variableMap.containsKey(
                ParserType.NONE))) {
            throw new ParsingException(format(ProcessorConstants.ERROR_RELATIONSHIP_BASE_LEVEL_MSG, lineNumber, line));
        }
        String[] variables = variableMap.get(ParserType.NONE);

        // Check that some options were found, if so then parse them
        if (variables.length > 0) {
            addOptions(parserData, parserData.getCurrentLevel(), variables, 0, line, lineNumber);
        } else {
            log.warn(format(ProcessorConstants.WARN_EMPTY_BRACKETS_MSG, lineNumber));
        }

        return true;
    }

    /**
     * Changes the current level that content is being processed for to a new level.
     *
     * @param parserData
     * @param newLevel            The new level to process for,
     * @param newIndentationLevel The new indentation level of the level in the Content Specification.
     */
    protected void changeCurrentLevel(ParserData parserData, final Level newLevel, int newIndentationLevel) {
        parserData.setIndentationLevel(newIndentationLevel);
        parserData.setCurrentLevel(newLevel);
    }

    /**
     * Processes the input to create a new common content node.
     *
     * @param parserData
     * @param line       The line of input to be processed
     * @return A common contents object initialised with the data from the input line.
     * @throws ParsingException Thrown if the line can't be parsed as a Common Content node, due to incorrect syntax.
     */
    protected CommonContent parseCommonContentLine(final ParserData parserData, final String line, int lineNumber) throws ParsingException {
        // Read in the variables inside of the brackets
        final HashMap<ParserType, String[]> variableMap = getLineVariables(parserData, line, lineNumber, '[', ']', ',', false);
        if (!variableMap.containsKey(ParserType.NONE)) {
            throw new ParsingException(format(ProcessorConstants.ERROR_INVALID_TOPIC_FORMAT_MSG, lineNumber, line));
        }

        // Get the title
        final String title = ProcessorUtilities.replaceEscapeChars(getTitle(line, '['));

        // Create the common content
        final CommonContent commonContent = new CommonContent(title, lineNumber, line);
        commonContent.setUniqueId("L" + lineNumber + "-CommonContent");

        // Check that no attributes were defined
        final String[] baseAttributes = variableMap.get(ParserType.NONE);
        if (baseAttributes.length > 1) {
            log.warn(format(ProcessorConstants.WARN_IGNORE_COMMON_CONTENT_ATTRIBUTES_MSG, lineNumber, line));
        }

        // Throw an error for relationships
        variableMap.remove(ParserType.NONE);
        if (variableMap.size() > 0) {
            throw new ParsingException(format(ProcessorConstants.ERROR_COMMON_CONTENT_CONTAINS_ILLEGAL_CONTENT, lineNumber, line));
        }

        return commonContent;
    }

    /**
     * Processes the input to create a new topic
     *
     * @param parserData
     * @param line       The line of input to be processed
     * @return A topics object initialised with the data from the input line.
     * @throws ParsingException Thrown if the line can't be parsed as a Topic, due to incorrect syntax.
     */
    protected SpecTopic parseTopic(final ParserData parserData, final String line, int lineNumber) throws ParsingException {
        // Read in the variables inside of the brackets
        final HashMap<ParserType, String[]> variableMap = getLineVariables(parserData, line, lineNumber, '[', ']', ',', false);
        if (!variableMap.containsKey(ParserType.NONE)) {
            throw new ParsingException(format(ProcessorConstants.ERROR_INVALID_TOPIC_FORMAT_MSG, lineNumber, line));
        }

        // Get the title
        final String title = ProcessorUtilities.replaceEscapeChars(getTitle(line, '['));

        // Create the spec topic
        final SpecTopic tempTopic = new SpecTopic(title, lineNumber, line, null);

        // Add in the topic attributes
        addTopicAttributes(tempTopic, parserData, variableMap.get(ParserType.NONE), lineNumber, line);
        parserData.getSpecTopics().put(tempTopic.getUniqueId(), tempTopic);

        // Process the Topic Relationships
        processTopicRelationships(parserData, tempTopic, variableMap, line, lineNumber);

        // Throw an error for info
        if (variableMap.containsKey(ParserType.INFO)) {
            throw new ParsingException(format(ProcessorConstants.ERROR_TOPIC_WITH_INFO_TOPIC, lineNumber, line));
        }

        return tempTopic;
    }

    /**
     * @param topic      The topic to add the attributes to.
     * @param parserData
     * @param variables  The variables that define the topic attributes.
     * @param lineNumber
     * @param line       The line of input to be processed
     * @throws ParsingException Thrown if the line can't be parsed as a Topic, due to incorrect syntax.
     */
    protected void addTopicAttributes(final ITopicNode topic, final ParserData parserData, final String[] variables, final int lineNumber,
            final String line) throws ParsingException {
        // Process a new topic
        int varStartPos = 2;

        // Process and validate the Types & ID
        if (variables.length >= 2) {
            // Check the type and the set it
            if (CSConstants.NEW_TOPIC_ID_PATTERN.matcher(variables[0]).matches()) {
                if (variables[1].matches("^C:[ ]*[0-9]+$")) {
                    variables[0] = "C" + variables[1].replaceAll("^C:[ ]*", "");
                } else if (topic instanceof SpecTopic) {
                    ((SpecTopic) topic).setType(ProcessorUtilities.replaceEscapeChars(variables[1]));
                } else {
                    varStartPos = 1;
                }
            }
            // If we have two variables for a existing topic then check to see if the second variable is the revision
            else if (variables[0].matches(CSConstants.EXISTING_TOPIC_ID_REGEX)) {
                // Check if the existing topic has a revision specified. If so parse it otherwise the var is a normal option
                if (variables[1].toLowerCase(Locale.ENGLISH).matches("^rev[ ]*:.*")) {
                    String[] vars = variables[1].split(":");
                    vars = CollectionUtilities.trimStringArray(vars);

                    try {
                        topic.setRevision(Integer.parseInt(vars[1]));
                    } catch (NumberFormatException ex) {
                        throw new ParsingException(format(ProcessorConstants.ERROR_TOPIC_INVALID_REVISION_FORMAT, lineNumber, line));
                    }
                } else {
                    varStartPos = 1;
                }
            } else {
                varStartPos = 1;
            }
        } else if (variables.length == 1) {
            if (!variables[0].matches("(" + CSConstants.DUPLICATE_TOPIC_ID_REGEX + ")|(" + CSConstants.CLONED_TOPIC_ID_REGEX + ")|(" +
                    CSConstants.EXISTING_TOPIC_ID_REGEX + ")|(" + CSConstants.NEW_TOPIC_ID_REGEX + ")|(" +
                    CSConstants.CLONED_DUPLICATE_TOPIC_ID_REGEX + ")")) {
                throw new ParsingException(format(ProcessorConstants.ERROR_TOPIC_INVALID_ID_MSG, lineNumber, line));
            } else if (CSConstants.NEW_TOPIC_ID_PATTERN.matcher(variables[0]).matches() && topic instanceof SpecTopic) {
                throw new ParsingException(format(ProcessorConstants.ERROR_INVALID_TYPE_TITLE_ID_MSG, lineNumber, line));
            }
            varStartPos = 1;
        } else {
            throw new ParsingException(format(ProcessorConstants.ERROR_INVALID_TITLE_ID_MSG, lineNumber, line));
        }

        // Set the topic ID
        topic.setId(variables[0]);

        /*
         * Set the Unique ID for the topic. If the ID is already unique and not
         * duplicated then just set the id (e.g. N1). Otherwise create the Unique ID
         * using the line number and topic ID.
         */
        String uniqueId = variables[0];
        if (variables[0].equals("N") || topic.isTopicADuplicateTopic() || topic.isTopicAClonedDuplicateTopic()
                || topic.isTopicAClonedTopic() || topic.isTopicAnExistingTopic()) {
            uniqueId = "L" + Integer.toString(lineNumber) + "-" + variables[0];
        } else if (CSConstants.NEW_TOPIC_ID_PATTERN.matcher(variables[0]).matches() && parserData.getSpecTopics().containsKey(variables[0])) {
            throw new ParsingException(format(ProcessorConstants.ERROR_DUPLICATE_ID_MSG, lineNumber, variables[0], line));
        }
        topic.setUniqueId(uniqueId);

        // Get the options if the topic is a new or cloned topic
        if (variables[0].matches("(" + CSConstants.NEW_TOPIC_ID_REGEX + ")|(" + CSConstants.CLONED_TOPIC_ID_REGEX +
                ")|(" + CSConstants.EXISTING_TOPIC_ID_REGEX + ")")) {
            addOptions(parserData, topic, variables, varStartPos, line, lineNumber);
        } else if (variables.length > varStartPos) {
            // Display warnings if options are specified for duplicated topics
            if (topic.isTopicADuplicateTopic() || topic.isTopicAClonedDuplicateTopic()) {
                log.warn(format(ProcessorConstants.WARN_IGNORE_DUP_INFO_MSG, lineNumber, line));
            }
        }
    }

    /**
     * Process the relationships parsed for a topic.
     *
     * @param parserData
     * @param tempTopic   The temporary topic that will be turned into a full topic once fully parsed.
     * @param variableMap The list of variables containing the parsed relationships.
     * @param input       The line representing the topic and it's relationships.
     * @param lineNumber  The number of the line the relationships are on.
     * @throws ParsingException Thrown if the variables can't be parsed due to incorrect syntax.
     */
    protected void processTopicRelationships(final ParserData parserData, final SpecTopic tempTopic,
            final HashMap<ParserType, String[]> variableMap, final String input, int lineNumber) throws ParsingException {
        // Process the relationships
        final String uniqueId = tempTopic.getUniqueId();
        final ArrayList<Relationship> topicRelationships = new ArrayList<Relationship>();

        // Refer-To relationships
        processRelationshipList(ParserType.REFER_TO, tempTopic, variableMap, topicRelationships, lineNumber);

        // Prerequisite relationships
        processRelationshipList(ParserType.PREREQUISITE, tempTopic, variableMap, topicRelationships, lineNumber);

        // Link-List relationships
        processRelationshipList(ParserType.LINKLIST, tempTopic, variableMap, topicRelationships, lineNumber);

        // Next and Previous relationships should only be created internally and shouldn't be specified by the user
        if (variableMap.containsKey(ParserType.NEXT) || variableMap.containsKey(ParserType.PREVIOUS)) {
            throw new ParsingException(format(ProcessorConstants.ERROR_TOPIC_NEXT_PREV_MSG, lineNumber, input));
        }

        // Add the relationships to the global list if any exist
        if (!topicRelationships.isEmpty()) {
            parserData.getTopicRelationships().put(uniqueId, topicRelationships);
        }

        // Process targets
        if (variableMap.containsKey(ParserType.TARGET)) {
            final String targetId = variableMap.get(ParserType.TARGET)[0];
            if (parserData.getTargetTopics().containsKey(targetId)) {
                throw new ParsingException(
                        format(ProcessorConstants.ERROR_DUPLICATE_TARGET_ID_MSG, parserData.getTargetTopics().get(targetId).getLineNumber(),
                                parserData.getTargetTopics().get(targetId).getText(), lineNumber, input));
            } else if (parserData.getTargetLevels().containsKey(targetId)) {
                throw new ParsingException(
                        format(ProcessorConstants.ERROR_DUPLICATE_TARGET_ID_MSG, parserData.getTargetLevels().get(targetId).getLineNumber(),
                                parserData.getTargetLevels().get(targetId).getText(), lineNumber, input));
            } else {
                parserData.getTargetTopics().put(targetId, tempTopic);
                tempTopic.setTargetId(targetId);
            }
        }

        // Throw an error for external targets
        if (variableMap.containsKey(ParserType.EXTERNAL_TARGET)) {
            // TODO Log an error properly using a constant
            throw new ParsingException("Unable to use external targets on topics.");
        }

        // Throw an error for external content spec injections
        if (variableMap.containsKey(ParserType.EXTERNAL_CONTENT_SPEC)) {
            // TODO Log an error properly using a constant
            throw new ParsingException("Unable to use external content specs as topics.");
        }
    }

    /**
     * Processes a list of relationships for a specific relationship type from some line processed variables.
     *
     * @param relationshipType The relationship type to be processed.
     * @param tempNode         The temporary node that will be turned into a full node once fully parsed.
     * @param variableMap      The list of variables containing the parsed relationships.
     * @param lineNumber       The number of the line the relationships are on.
     * @param relationships    The list of topic relationships.
     * @throws ParsingException Thrown if the variables can't be parsed due to incorrect syntax.
     */
    private void processRelationshipList(final ParserType parserType, final SpecNodeWithRelationships tempNode,
            final HashMap<ParserType, String[]> variableMap, final List<Relationship> relationships,
            int lineNumber) throws ParsingException {
        final String uniqueId = tempNode.getUniqueId();

        String errorMessageFormat = null;
        RelationshipType relationshipType;
        switch (parserType) {
            case REFER_TO:
                errorMessageFormat = ProcessorConstants.ERROR_INVALID_REFERS_TO_RELATIONSHIP;
                relationshipType = RelationshipType.REFER_TO;
                break;
            case LINKLIST:
                errorMessageFormat = ProcessorConstants.ERROR_INVALID_LINK_LIST_RELATIONSHIP;
                relationshipType = RelationshipType.LINKLIST;
                break;
            case PREREQUISITE:
                errorMessageFormat = ProcessorConstants.ERROR_INVALID_PREREQUISITE_RELATIONSHIP;
                relationshipType = RelationshipType.PREREQUISITE;
                break;
            default:
                return;
        }

        if (variableMap.containsKey(parserType)) {
            final String[] relationshipList = variableMap.get(parserType);
            for (final String relationshipId : relationshipList) {
                if (relationshipId.matches(ProcessorConstants.RELATION_ID_REGEX)) {
                    relationships.add(new Relationship(uniqueId, relationshipId, relationshipType));
                } else if (relationshipId.matches(ProcessorConstants.RELATION_ID_LONG_REGEX)) {
                    final Matcher matcher = RELATION_ID_LONG_PATTERN.matcher(relationshipId);

                    matcher.find();
                    final String id = matcher.group("TopicID");
                    final String relationshipTitle = matcher.group("TopicTitle");
                    final String cleanedTitle = ProcessorUtilities.cleanXMLCharacterReferences(relationshipTitle.trim());

                    relationships.add(new Relationship(uniqueId, id, relationshipType,
                            ProcessorUtilities.replaceEscapeChars(cleanedTitle)));
                } else {
                    if (relationshipId.matches("^(" + ProcessorConstants.TARGET_BASE_REGEX + "|[0-9]+).*?(" +
                            ProcessorConstants.TARGET_BASE_REGEX + "|[0-9]+).*")) {
                        throw new ParsingException(format(ProcessorConstants.ERROR_MISSING_SEPARATOR_MSG, lineNumber, ','));
                    } else {
                        throw new ParsingException(format(errorMessageFormat, lineNumber));
                    }
                }
            }
        }
    }

    /**
     * Creates an empty Level using the LevelType to determine which Level subclass to instantiate.
     *
     * @param lineNumber The line number of the level.
     * @param levelType  The Level Type.
     * @param input      The string that represents the level, if one exists,
     * @return The empty Level subclass object, or a plain Level object if no type matches a subclass.
     */
    protected Level createEmptyLevelFromType(final int lineNumber, final LevelType levelType, final String input) {
        // Create the level based on the type
        switch (levelType) {
            case APPENDIX:
                return new Appendix(null, lineNumber, input);
            case CHAPTER:
                return new Chapter(null, lineNumber, input);
            case SECTION:
                return new Section(null, lineNumber, input);
            case PART:
                return new Part(null, lineNumber, input);
            case PROCESS:
                return new Process(null, lineNumber, input);
            case INITIAL_CONTENT:
                return new InitialContent(lineNumber, input);
            default:
                return new Level(null, lineNumber, input, levelType);
        }
    }

    /**
     * Processes and creates a level based on the level type.
     *
     * @param parserData
     * @param lineNumber The line number the level is on.
     * @param levelType  The type the level will represent. ie. A Chapter or Appendix
     * @param line       The chapter string in the content specification.
     * @return The created level or null if an error occurred.
     * @throws ParsingException Thrown if the line can't be parsed as a Level, due to incorrect syntax.
     */
    protected Level parseLevel(final ParserData parserData, final int lineNumber, final LevelType levelType,
            final String line) throws ParsingException {
        String splitVars[] = StringUtilities.split(line, ':', 2);
        // Remove the whitespace from each value in the split array
        splitVars = CollectionUtilities.trimStringArray(splitVars);

        // Create the level based on the type
        final Level newLvl = createEmptyLevelFromType(lineNumber, levelType, line);
        newLvl.setUniqueId("L" + lineNumber);

        // Parse the input
        if (splitVars.length >= 2) {
            final String unescapedTitle = getTitle(splitVars[1], '[');
            final String title = ProcessorUtilities.replaceEscapeChars(unescapedTitle);
            if (!isNullOrEmpty(title)) {
                newLvl.setTitle(title);
            }
            // Get the mapping of variables
            final HashMap<ParserType, List<String[]>> variableMap = getLineVariables(parserData, splitVars[1], lineNumber, '[', ']', ',',
                    false, true);
            if (variableMap.containsKey(ParserType.NONE)) {
                boolean optionsProcessed = false;
                for (final String[] variables : variableMap.get(ParserType.NONE)) {
                    if (variables.length >= 1) {
                        if (variables[0].matches(CSConstants.ALL_TOPIC_ID_REGEX)) {
                            final InitialContent initialContent;
                            if (newLvl instanceof InitialContent) {
                                initialContent = (InitialContent) newLvl;
                            } else {
                                initialContent = new InitialContent(lineNumber, "");
                                initialContent.setUniqueId("L" + lineNumber + "-1");
                                parserData.getLevels().put(initialContent.getUniqueId(), initialContent);
                                newLvl.appendChild(initialContent);
                            }

                            final String topicString = unescapedTitle + " [" + StringUtilities.buildString(variables, ", ") + "]";
                            final SpecTopic initialContentTopic = parseTopic(parserData, topicString, lineNumber);
                            initialContent.appendSpecTopic(initialContentTopic);
                        } else {
                            // Process the options
                            if (!optionsProcessed) {
                                addOptions(parserData, newLvl, variables, 0, line, lineNumber);
                                optionsProcessed = true;
                            } else {
                                throw new ParsingException(
                                        format(ProcessorConstants.ERROR_DUPLICATED_RELATIONSHIP_TYPE_MSG, lineNumber, line));
                            }
                        }
                    }
                }
            }

            // Flatten the variable map since we've gotten the useful information
            final HashMap<ParserType, String[]> flattenedVariableMap = new HashMap<ParserType, String[]>();
            for (final Map.Entry<ParserType, List<String[]>> lineVariable : variableMap.entrySet()) {
                flattenedVariableMap.put(lineVariable.getKey(), lineVariable.getValue().get(0));
            }

            // Add targets for the level
            if (variableMap.containsKey(ParserType.TARGET)) {
                final String[] targets = flattenedVariableMap.get(ParserType.TARGET);
                final String targetId = targets[0];
                if (parserData.getTargetTopics().containsKey(targetId)) {
                    throw new ParsingException(format(ProcessorConstants.ERROR_DUPLICATE_TARGET_ID_MSG,
                            parserData.getTargetTopics().get(targetId).getLineNumber(),
                            parserData.getTargetTopics().get(targetId).getText(), lineNumber, line));
                } else if (parserData.getTargetLevels().containsKey(targetId)) {
                    throw new ParsingException(format(ProcessorConstants.ERROR_DUPLICATE_TARGET_ID_MSG,
                            parserData.getTargetLevels().get(targetId).getLineNumber(),
                            parserData.getTargetLevels().get(targetId).getText(), lineNumber, line));
                } else {
                    parserData.getTargetLevels().put(targetId, newLvl);
                    newLvl.setTargetId(targetId);
                }
            }

            // Add the info topic for the level
            if (variableMap.containsKey(ParserType.INFO)) {
                final String[] infoVariables = flattenedVariableMap.get(ParserType.INFO);
                final InfoTopic infoTopic = new InfoTopic(newLvl);

                // Add the attributes to the topic
                addTopicAttributes(infoTopic, parserData, infoVariables, lineNumber, line);

                newLvl.setInfoTopic(infoTopic);
            }

            // Check for external targets
//            if (variableMap.containsKey(ParserType.EXTERNAL_TARGET)) {
//                final String externalTargetId = variableMap.get(ParserType.EXTERNAL_TARGET)[0];
//                getExternalTargetLevels().put(externalTargetId, newLvl);
//                newLvl.setExternalTargetId(externalTargetId);
//            }

//            // Check if the level is injecting data from another content spec
//            if (variableMap.containsKey(ParserType.EXTERNAL_CONTENT_SPEC)) {
//                processExternalLevel(newLvl, variableMap.get(ParserType.EXTERNAL_CONTENT_SPEC)[0], title, line);
//            }

            // Process any relationship content that might have been defined only if an initial content topic has been defined
            if (variableMap.containsKey(ParserType.REFER_TO) || variableMap.containsKey(ParserType.PREREQUISITE) || variableMap.containsKey(
                    ParserType.NEXT) || variableMap.containsKey(ParserType.PREVIOUS) || variableMap.containsKey(ParserType.LINKLIST)) {

                // Check that no relationships were specified for the level
                if (newLvl instanceof InitialContent) {
                    processLevelRelationships(parserData, newLvl, flattenedVariableMap, line, lineNumber);
                } else if (newLvl.getChildLevels().isEmpty()) {
                    throw new ParsingException(
                            format(ProcessorConstants.ERROR_LEVEL_RELATIONSHIP_MSG, lineNumber, levelType.getTitle(), levelType.getTitle(),
                                    line));
                } else {
                    processLevelRelationships(parserData, (Level) newLvl.getChildLevels().get(0), flattenedVariableMap, line, lineNumber);
                }
            }
        }

        return newLvl;
    }

    /**
     * Process the relationships parsed for a topic.
     *
     * @param parserData
     * @param level       The temporary topic that will be turned into a full topic once fully parsed.
     * @param variableMap The list of variables containing the parsed relationships.
     * @param line        The line representing the topic and it's relationships.
     * @param lineNumber  The number of the line the relationships are on.
     * @throws ParsingException Thrown if the variables can't be parsed due to incorrect syntax.
     */
    protected void processLevelRelationships(final ParserData parserData, final Level level,
            final HashMap<ParserType, String[]> variableMap, final String line, int lineNumber) throws ParsingException {
        // Process the relationships
        final String uniqueId = level.getUniqueId();
        final ArrayList<Relationship> levelRelationships = new ArrayList<Relationship>();

        // Refer-To relationships
        processRelationshipList(ParserType.REFER_TO, level, variableMap, levelRelationships, lineNumber);

        // Prerequisite relationships
        processRelationshipList(ParserType.PREREQUISITE, level, variableMap, levelRelationships, lineNumber);

        // Link-List relationships
        processRelationshipList(ParserType.LINKLIST, level, variableMap, levelRelationships, lineNumber);

        // Next and Previous relationships should only be created internally and shouldn't be specified by the user
        if (variableMap.containsKey(ParserType.NEXT) || variableMap.containsKey(ParserType.PREVIOUS)) {
            throw new ParsingException(format(ProcessorConstants.ERROR_TOPIC_NEXT_PREV_MSG, lineNumber, line));
        }

        // Add the relationships to the global list if any exist
        if (!levelRelationships.isEmpty()) {
            parserData.getLevelRelationships().put(uniqueId, levelRelationships);
        }
    }

    /**
     * Gets the variables from a string. The variables are at the end of a line and are inside of the starting and ending delimiter and are
     * separated by the separator.
     *
     * @param parserData
     * @param line        The line of input to get the variables for.
     * @param lineNumber  TODO
     * @param startDelim  The starting delimiter of the variables.
     * @param endDelim    The ending delimiter of the variables.
     * @param separator   The separator used to separate the variables.
     * @param ignoreTypes Used if all variables are to be stored inside of the Relationship NONE type.
     * @return A Map of String arrays for different relationship. Inside each string array is the singular variables.
     * @throws ParsingException Thrown if the line can't be successfully parsed.
     */
    public HashMap<ParserType, String[]> getLineVariables(final ParserData parserData, final String line, int lineNumber, char startDelim,
            char endDelim, char separator, boolean ignoreTypes) throws ParsingException {
        final HashMap<ParserType, List<String[]>> lineVariables = getLineVariables(parserData, line, lineNumber, startDelim, endDelim,
                separator, ignoreTypes, false);

        final HashMap<ParserType, String[]> retValue = new HashMap<ParserType, String[]>();
        for (final Map.Entry<ParserType, List<String[]>> lineVariable : lineVariables.entrySet()) {
            retValue.put(lineVariable.getKey(), lineVariable.getValue().get(0));
        }
        return retValue;
    }

    /**
     * Gets the variables from a string. The variables are at the end of a line and are inside of the starting and ending delimiter and are
     * separated by the separator.
     *
     * @param parserData
     * @param line        The line of input to get the variables for.
     * @param lineNumber  TODO
     * @param startDelim  The starting delimiter of the variables.
     * @param endDelim    The ending delimiter of the variables.
     * @param separator   The separator used to separate the variables.
     * @param ignoreTypes Used if all variables are to be stored inside of the Relationship NONE type.
     * @param groupTypes  Used if the relationship types should be group if two or more of the same types are found.
     * @return A Map of String arrays for different relationship. Inside each string array is the singular variables.
     * @throws ParsingException Thrown if the line can't be successfully parsed.
     */
    public HashMap<ParserType, List<String[]>> getLineVariables(final ParserData parserData, final String line, int lineNumber,
            final char startDelim, final char endDelim, final char separator, final boolean ignoreTypes,
            final boolean groupTypes) throws ParsingException {
        final HashMap<ParserType, List<String[]>> output = new HashMap<ParserType, List<String[]>>();

        final int lastStartDelimPos = StringUtilities.lastIndexOf(line, startDelim);
        final int lastEndDelimPos = StringUtilities.lastIndexOf(line, endDelim);

        final String nextLine = parserData.getLines().peek();

        /*
         * Check to see if the line doesn't match the regex even once. Also check to see if the next
         * line is a continuation of the current line. If so then attempt to read the next line.
         */
        if (lastEndDelimPos < lastStartDelimPos || (nextLine != null && nextLine.trim().toUpperCase(Locale.ENGLISH).matches("^\\" +
                startDelim + "[ ]*(R|L|P|T|B).*")) || line.trim().matches("(.|\n|\r\n)*(?<!\\\\)" + separator + "$")) {
            // Read in a new line and increment relevant counters
            String temp = parserData.getLines().poll();
            if (temp != null) {
                parserData.setLineCount(parserData.getLineCount() + 1);

                return getLineVariables(parserData, line + "\n" + temp, lineNumber, startDelim, endDelim, separator, ignoreTypes,
                        groupTypes);
            }
        }

        // Check that we have variables to process
        if (lastStartDelimPos == -1 && lastEndDelimPos == -1) return output;

        /* Get the variables from the line */
        final List<VariableSet> varSets = findVariableSets(parserData, line, startDelim, endDelim);

        // Process the variables that were found
        for (final VariableSet set : varSets) {
            // Check that a opening bracket wasn't missed
            if (set.getStartPos() == null) {
                throw new ParsingException(format(ProcessorConstants.ERROR_NO_OPENING_BRACKET_MSG, lineNumber, startDelim));
            }

            // Check that a closing bracket wasn't missed
            if (set.getEndPos() == null) {
                throw new ParsingException(format(ProcessorConstants.ERROR_NO_ENDING_BRACKET_MSG, lineNumber, endDelim));
            }

            final ArrayList<String> variables = new ArrayList<String>();
            final String variableSet = set.getContents().substring(1, set.getContents().length() - 1);

            // Split the variables set into individual variables
            final ParserType type = getType(variableSet);
            if (!ignoreTypes && (type == ParserType.REFER_TO || type == ParserType.PREREQUISITE || type == ParserType.NEXT || type ==
                    ParserType.PREVIOUS || type == ParserType.LINKLIST || type == ParserType.INFO)) {
                // Remove the type specifier from the start of the variable set
                String splitString[] = StringUtilities.split(variableSet.trim(), ':', 2);
                // Check that there are actually variables set
                if (splitString.length > 1) {
                    // Replace any inner content with markers, so that any splitting doesn't get messed up
                    final Matcher matcher = SQUARE_BRACKET_PATTERN.matcher(splitString[1]);
                    final HashMap<String, String> replacements = new HashMap<String, String>();
                    int i = 0;
                    String fixedString = splitString[1];
                    while (matcher.find()) {
                        final String replacement = matcher.group(CSConstants.BRACKET_CONTENTS);
                        // If the content has already been replaced then skip it
                        if (!replacements.containsKey(replacement)) {
                            final String marker = "###" + i + "###";
                            replacements.put(replacement, marker);
                            fixedString = fixedString.replace(replacement, marker);
                            i++;
                        }
                    }

                    // Split the string and add the variables
                    splitString = StringUtilities.split(fixedString, separator);
                    for (final String s : splitString) {
                        final String var = s.replaceAll("(^\\s*(\r?\n)*)|((\r?\n)*\\s*$)", "");

                        // Replace any markers
                        String fixedVar = var.trim();
                        for (final Map.Entry<String, String> entry : replacements.entrySet()) {
                            fixedVar = fixedVar.replace(entry.getValue(), entry.getKey());
                        }

                        // Check that a separator wasn't missed.
                        if (StringUtilities.lastIndexOf(var, startDelim) != StringUtilities.indexOf(var, startDelim) || var.indexOf(
                                '\n') != -1) {
                            throw new ParsingException(format(ProcessorConstants.ERROR_MISSING_SEPARATOR_MSG, lineNumber, separator));
                        } else if (s.trim().isEmpty()) {
                            throw new ParsingException(format(ProcessorConstants.ERROR_MISSING_ATTRIB_FORMAT_MSG, lineNumber, line));
                        } else {
                            variables.add(fixedVar);
                        }
                    }
                } else {
                    throw new ParsingException(format(ProcessorConstants.ERROR_INVALID_ATTRIB_FORMAT_MSG, lineNumber, line));
                }
            } else if (!ignoreTypes && type == ParserType.TARGET) {
                variables.add(variableSet.replaceAll("\\s", ""));
            } else if (!ignoreTypes && type == ParserType.EXTERNAL_TARGET) {
                variables.add(variableSet.replaceAll("\\s", ""));
            } else if (!ignoreTypes && type == ParserType.EXTERNAL_CONTENT_SPEC) {
                variables.add(variableSet.trim());
            } else if (!variableSet.trim().isEmpty()) {
                // Replace any inner content with markers, so that any splitting doesn't get messed up
                final Matcher matcher = SQUARE_BRACKET_PATTERN.matcher(variableSet);
                final HashMap<String, String> replacements = new HashMap<String, String>();
                int i = 0;
                String fixedString = variableSet;
                while (matcher.find()) {
                    final String replacement = matcher.group(CSConstants.BRACKET_CONTENTS);
                    final String marker = "###" + i + "###";
                    replacements.put(replacement, marker);
                    fixedString = fixedString.replace(replacement, marker);
                    i++;
                }

                // Normal set of variables that contains the ID and/or tags
                final String splitString[] = StringUtilities.split(fixedString, separator);
                for (final String s : splitString) {
                    if (!s.trim().isEmpty()) {
                        // Replace any markers
                        String fixedVar = s.trim();
                        for (final Map.Entry<String, String> entry : replacements.entrySet()) {
                            fixedVar = fixedVar.replace(entry.getValue(), entry.getKey());
                        }

                        variables.add(fixedVar);
                    } else {
                        throw new ParsingException(format(ProcessorConstants.ERROR_MISSING_ATTRIB_FORMAT_MSG, lineNumber, line));
                    }
                }
            }

            // Add the variable set to the mapping
            if (output.containsKey(type)) {
                if (ignoreTypes || groupTypes) {
                    output.get(type).add(variables.toArray(new String[variables.size()]));
                } else {
                    throw new ParsingException(format(ProcessorConstants.ERROR_DUPLICATED_RELATIONSHIP_TYPE_MSG, lineNumber, line));
                }
            } else {
                final ArrayList<String[]> list = new ArrayList<String[]>();
                list.add(variables.toArray(new String[variables.size()]));
                output.put(type, list);
            }
        }

        return output;
    }

    /**
     * Processes a string of variables to find the type that exists within the string.
     *
     * @param variableString The variable string to be processed.
     * @return The parser type that was found in the string otherwise a NONE type is returned.
     */
    protected ParserType getType(final String variableString) {
        final String uppercaseVarSet = variableString.trim().toUpperCase(Locale.ENGLISH);
        if (uppercaseVarSet.matches(ProcessorConstants.RELATED_REGEX)) {
            return ParserType.REFER_TO;
        } else if (uppercaseVarSet.matches(ProcessorConstants.PREREQUISITE_REGEX)) {
            return ParserType.PREREQUISITE;
        } else if (uppercaseVarSet.matches(ProcessorConstants.NEXT_REGEX)) {
            return ParserType.NEXT;
        } else if (uppercaseVarSet.matches(ProcessorConstants.PREV_REGEX)) {
            return ParserType.PREVIOUS;
        } else if (uppercaseVarSet.matches(ProcessorConstants.TARGET_REGEX)) {
            return ParserType.TARGET;
        } else if (uppercaseVarSet.matches(ProcessorConstants.EXTERNAL_TARGET_REGEX)) {
            return ParserType.EXTERNAL_TARGET;
        } else if (uppercaseVarSet.matches(ProcessorConstants.EXTERNAL_CSP_REGEX)) {
            return ParserType.EXTERNAL_CONTENT_SPEC;
        } else if (uppercaseVarSet.matches(ProcessorConstants.LINK_LIST_REGEX)) {
            return ParserType.LINKLIST;
        } else if (uppercaseVarSet.matches(ProcessorConstants.INFO_REGEX)) {
            return ParserType.INFO;
        } else {
            return ParserType.NONE;
        }
    }

    /**
     * Adds the options from an array of variables to a node (Level or Topic). It starts checking the variables from the startPos
     * position of the variable array, then check to see if the variable is a tag or attribute and processes it.
     *
     * @param parserData
     * @param node          The node to add the options to.
     * @param vars          An array of variables to get the options for.
     * @param startPos      The starting position in the variable array to start checking.
     * @param originalInput The original string used to create these options.
     * @throws ParsingException Thrown if the variables can't be successfully parsed as options.
     */
    protected void addOptions(final ParserData parserData, final IOptionsNode node, final String[] vars, final int startPos,
            final String originalInput, int lineNumber) throws ParsingException {
        // Process each variable in vars starting from the start position
        for (int i = startPos; i < vars.length; i++) {
            String str = vars[i];
            // If the variable contains a "=" then it isn't a tag so process it separately
            if (StringUtilities.indexOf(str, '=') != -1) {
                String temp[] = StringUtilities.split(str, '=', 2);
                temp = CollectionUtilities.trimStringArray(temp);
                if (temp.length == 2) {
                    if (temp[0].equalsIgnoreCase("description")) {
                        if (node.getDescription(false) == null) {
                            node.setDescription(ProcessorUtilities.replaceEscapeChars(temp[1]));
                        } else {
                            throw new ParsingException(
                                    String.format(ProcessorConstants.ERROR_DUPLICATE_ATTRIBUTE_MSG, lineNumber, "description",
                                            originalInput));
                        }
                    } else if (temp[0].equalsIgnoreCase("writer")) {
                        if (node.getAssignedWriter(false) == null) {
                            node.setAssignedWriter(ProcessorUtilities.replaceEscapeChars(temp[1]));
                        } else {
                            throw new ParsingException(
                                    String.format(ProcessorConstants.ERROR_DUPLICATE_ATTRIBUTE_MSG, lineNumber, "writer", originalInput));
                        }
                    } else if (temp[0].equalsIgnoreCase("condition")) {
                        if (node.getConditionStatement() == null) {
                            final String condition = temp[1];
                            node.setConditionStatement(condition);
                            try {
                                Pattern.compile(condition);
                            } catch (PatternSyntaxException exception) {
                                throw new ParsingException(
                                        format(ProcessorConstants.ERROR_INVALID_CONDITION_MSG, lineNumber, originalInput));
                            }
                        } else {
                            throw new ParsingException(
                                    String.format(ProcessorConstants.ERROR_DUPLICATE_ATTRIBUTE_MSG, lineNumber, "condition",
                                            originalInput));
                        }
                    } else if (temp[0].equalsIgnoreCase("URL") && node instanceof SpecNode) {
                        ((SpecNode) node).addSourceUrl(ProcessorUtilities.replaceEscapeChars(temp[1]));
                    } else if (temp[0].equalsIgnoreCase("Fixed URL") && node instanceof SpecNode) {
                        ((SpecNode) node).setFixedUrl(ProcessorUtilities.replaceEscapeChars(temp[1]));
                    } else {
                        throw new ParsingException(format(ProcessorConstants.ERROR_INVALID_ATTRIBUTE_MSG, lineNumber, originalInput));
                    }
                } else {
                    throw new ParsingException(format(ProcessorConstants.ERROR_INVALID_ATTRIB_FORMAT_MSG, lineNumber, originalInput));
                }
            }
            // The variable is a tag with a category specified
            else if (StringUtilities.indexOf(str, ':') != -1) {
                String temp[] = StringUtilities.split(str, ':', 2);
                temp = CollectionUtilities.trimStringArray(temp);
                if (temp.length == 2) {
                    // Check if the category has an array of tags
                    if (StringUtilities.indexOf(temp[1], '(') != -1) {
                        String[] tempTags;
                        final StringBuilder input = new StringBuilder(temp[1]);
                        if (StringUtilities.indexOf(temp[1], ')') == -1) {
                            for (int j = i + 1; j < vars.length; j++) {
                                i++;
                                if (StringUtilities.indexOf(vars[j], ')') != -1) {
                                    input.append(", ").append(vars[j]);
                                    break;
                                } else {
                                    input.append(", ").append(vars[j]);
                                }
                            }
                        }

                        // Get the mapping of variables
                        final HashMap<ParserType, String[]> variableMap = getLineVariables(parserData, input.toString(), lineNumber, '(',
                                ')', ',', false);
                        if (variableMap.containsKey(ParserType.NONE)) {
                            tempTags = variableMap.get(ParserType.NONE);
                        } else {
                            tempTags = null;
                        }

                        if (tempTags != null && tempTags.length >= 2) {
                            final String tags[] = new String[tempTags.length];
                            System.arraycopy(tempTags, 0, tags, 0, tempTags.length);

                            if (!node.addTags(Arrays.asList(tags))) {
                                throw new ParsingException(
                                        format(ProcessorConstants.ERROR_MULTI_TAG_DUPLICATED_MSG, lineNumber, originalInput));
                            }
                        } else {
                            throw new ParsingException(
                                    format(ProcessorConstants.ERROR_INVALID_TAG_ATTRIB_FORMAT_MSG, lineNumber, originalInput));
                        }
                    }
                    // Just a single tag so add it straight away
                    else {
                        if (!node.addTag(ProcessorUtilities.replaceEscapeChars(temp[1]))) {
                            throw new ParsingException(format(ProcessorConstants.ERROR_TAG_DUPLICATED_MSG, lineNumber, originalInput));
                        }
                    }
                } else {
                    throw new ParsingException(format(ProcessorConstants.ERROR_INVALID_TAG_ATTRIB_FORMAT_MSG, lineNumber, originalInput));
                }
            }
            // Variable is a tag with no category specified
            else {
                if (str.matches(CSConstants.ALL_TOPIC_ID_REGEX)) {
                    throw new ParsingException(format(ProcessorConstants.ERROR_INCORRECT_TOPIC_ID_LOCATION_MSG, lineNumber, originalInput));
                }

                if (!node.addTag(str)) {
                    throw new ParsingException(format(ProcessorConstants.ERROR_TAG_DUPLICATED_MSG, lineNumber, originalInput));
                }
            }
        }
    }

    /**
     * Gets the title of a chapter/section/appendix/topic by returning everything before the start delimiter.
     *
     * @param input      The input to be parsed to get the title.
     * @param startDelim The delimiter that specifies that start of options (ie '[')
     * @return The title as a String or null if the title is blank.
     */
    protected String getTitle(final String input, final char startDelim) {
        if (isStringNullOrEmpty(input)) {
            return null;
        } else {
            return ProcessorUtilities.cleanXMLCharacterReferences(StringUtilities.split(input, startDelim)[0].trim());
        }
    }

    /**
     * Process the relationships without logging any errors.
     *
     * @param parserData
     */
    protected void processRelationships(final ParserData parserData) {
        // Process the level relationships
        for (final Map.Entry<String, List<Relationship>> entry : parserData.getLevelRelationships().entrySet()) {
            final String levelId = entry.getKey();
            final Level level = parserData.getLevels().get(levelId);

            assert level != null;

            for (final Relationship relationship : entry.getValue()) {
                processRelationship(parserData, level, relationship);
            }
        }

        // Process the topic relationships
        for (final Map.Entry<String, List<Relationship>> entry : parserData.getTopicRelationships().entrySet()) {
            final String topicId = entry.getKey();
            final SpecTopic specTopic = parserData.getSpecTopics().get(topicId);

            assert specTopic != null;

            for (final Relationship relationship : entry.getValue()) {
                processRelationship(parserData, specTopic, relationship);
            }
        }
    }

    /**
     * Process a specific relationship without any error checking.
     *
     * @param parserData
     * @param specNode
     * @param relationship
     */
    protected void processRelationship(final ParserData parserData, final SpecNodeWithRelationships specNode,
            final Relationship relationship) {
        final String relatedId = relationship.getSecondaryRelationshipId();
        // The relationship points to a target so it must be a level or topic
        if (relatedId.toUpperCase(Locale.ENGLISH).matches(ProcessorConstants.TARGET_REGEX)) {
            if (parserData.getTargetTopics().containsKey(relatedId) && !parserData.getTargetLevels().containsKey(relatedId)) {
                specNode.addRelationshipToTarget(parserData.getTargetTopics().get(relatedId), relationship.getType(),
                        relationship.getRelationshipTitle());
            } else if (!parserData.getTargetTopics().containsKey(relatedId) && parserData.getTargetLevels().containsKey(relatedId)) {
                specNode.addRelationshipToTarget(parserData.getTargetLevels().get(relatedId), relationship.getType(),
                        relationship.getRelationshipTitle());
            } else {
                final SpecTopic dummyTopic = new SpecTopic(-1, "");
                dummyTopic.setTargetId(relatedId);
                dummyTopic.setUniqueId("-1");
                specNode.addRelationshipToTarget(dummyTopic, relationship.getType(), relationship.getRelationshipTitle());
            }
        }
        // The relationship isn't a target so it must point to a topic directly
        else {
            if (!CSConstants.NEW_TOPIC_ID_PATTERN.matcher(relatedId).matches()) {
                // The relationship isn't a unique new topic so it will contain the line number in front of
                // the topic ID
                if (!relatedId.startsWith("X")) {
                    int count = 0;
                    SpecTopic relatedTopic = null;

                    // Get the related topic and count if more then one is found
                    for (final Map.Entry<String, SpecTopic> specTopicEntry : parserData.getSpecTopics().entrySet()) {
                        if (specTopicEntry.getKey().matches("^[\\w\\d]+-" + relatedId + "$")) {
                            relatedTopic = specTopicEntry.getValue();
                            count++;
                        }
                    }

                    /*
                     * Add the relationship to the node even if the relationship isn't duplicated
                     * and the related topic isn't the current topic. This is so it shows up in the
                     * output.
                     */
                    if (count > 0) {
                        specNode.addRelationshipToTopic(relatedTopic, relationship.getType(), relationship.getRelationshipTitle());
                    } else {
                        final SpecTopic dummyTopic = new SpecTopic(-1, "");
                        dummyTopic.setId(relatedId);
                        dummyTopic.setUniqueId("-1");
                        specNode.addRelationshipToTopic(dummyTopic, relationship.getType(), relationship.getRelationshipTitle());
                    }
                } else {
                    final SpecTopic dummyTopic = new SpecTopic(-1, "");
                    dummyTopic.setId(relatedId);
                    dummyTopic.setUniqueId("-1");
                    specNode.addRelationshipToTopic(dummyTopic, relationship.getType(), relationship.getRelationshipTitle());
                }
            } else {
                if (parserData.getSpecTopics().containsKey(relatedId)) {
                    final SpecTopic relatedSpecTopic = parserData.getSpecTopics().get(relatedId);

                    // Check that a duplicate doesn't exist, because if it does the new topic isn't unique
                    String duplicatedId = "X" + relatedId.substring(1);
                    boolean duplicateExists = false;
                    for (String uniqueTopicId : parserData.getSpecTopics().keySet()) {
                        if (uniqueTopicId.matches("^[\\w\\d]+-" + duplicatedId + "$")) {
                            duplicateExists = true;
                            break;
                        }
                    }

                    if (relatedSpecTopic != specNode) {
                        if (!duplicateExists) {
                            specNode.addRelationshipToTopic(relatedSpecTopic, relationship.getType(), relationship.getRelationshipTitle());
                        } else {
                            // Only create a new target if one doesn't already exist
                            if (relatedSpecTopic.getTargetId() == null) {
                                String targetId = ContentSpecUtilities.generateRandomTargetId(relatedSpecTopic.getUniqueId());
                                while (parserData.getTargetTopics().containsKey(targetId) || parserData.getTargetLevels().containsKey(
                                        targetId)) {
                                    targetId = ContentSpecUtilities.generateRandomTargetId(relatedSpecTopic.getUniqueId());
                                }
                                parserData.getSpecTopics().get(relatedId).setTargetId(targetId);
                                parserData.getTargetTopics().put(targetId, relatedSpecTopic);
                            }
                            specNode.addRelationshipToTopic(relatedSpecTopic, relationship.getType(), relationship.getRelationshipTitle());
                        }
                    }
                } else {
                    final SpecTopic dummyTopic = new SpecTopic(-1, "");
                    dummyTopic.setId(relatedId);
                    dummyTopic.setUniqueId("-1");
                    specNode.addRelationshipToTopic(dummyTopic, relationship.getType(), relationship.getRelationshipTitle());
                }
            }
        }
    }

//    /**
//     * Process an external level and inject it into the current content specification.
//     *
//     * @param lvl                  The level to inject the external levels contents.
//     * @param externalCSPReference The reference to the external level. The CSP ID and possibly the External Target ID.
//     * @param title                The title of the external level.
//     * @param input                The original input used to specify the external level.
//     */
//    // TODO Finish External level processing so specs can reference other specs levels
//    protected void processExternalLevel(final Level lvl, final String externalCSPReference, final String title,
//            final String input) throws ParsingException {
//        //TODO Add the level/topic contents to the local parser variables
//        String[] vars = externalCSPReference.split(":");
//        vars = CollectionUtilities.trimStringArray(vars);
//
//        /* No need to check for an exception as the regex that produces this will take care of it. */
//        final Integer cspId = Integer.parseInt(vars[0]);
//        final Integer targetId = vars.length > 1 ? Integer.parseInt(vars[1]) : null;
//
//        final TopicWrapper externalContentSpec = topicProvider.getTopic(cspId);
//
//        if (externalContentSpec != null) {
//            /* We are importing part of an external content specification */
//            if (targetId != null) {
//                final ContentSpecParser parser = new ContentSpecParser(providerFactory, loggerManager);
//                boolean foundTargetId = false;
//                try {
//                    parser.parse(externalContentSpec.getXml());
//                    for (final String externalTargetId : parser.getExternalTargetLevels().keySet()) {
//                        final String id = externalTargetId.replaceAll("ET", "");
//                        if (id.equals(targetId.toString())) {
//                            foundTargetId = true;
//
//                            final Level externalLvl = parser.getExternalTargetLevels().get(externalTargetId);
//
//                            // TODO Deal with processes
//
//                            /* Check that the title matches */
//                            if (externalLvl.getTitle().equals(title)) {
//                                for (final Node externalChildNode : externalLvl.getChildNodes()) {
//                                    if (externalChildNode instanceof SpecNode) {
//                                        lvl.appendChild(externalChildNode);
//                                    } else if (externalChildNode instanceof Comment) {
//                                        lvl.appendComment((Comment) externalChildNode);
//                                    }
//                                }
//                            } else {
//                                // TODO Error Message
//                                throw new ParsingException("Title doesn't match the referenced target id.");
//                            }
//                        }
//                    }
//
//                    if (!foundTargetId) {
//                        // TODO Error Message
//                        throw new ParsingException("External target doesn't exist in the content specification");
//                    }
//                } catch (Exception e) {
//                    // TODO Error message
//                    throw new ParsingException("Failed to pull in external content spec reference");
//                }
//            }
//            /* Import the entire content spec, excluding the metadata */
//            else if (lvl.getType() == LevelType.BASE) {
//                // TODO Handle importing the entire content specification
//            } else {
//                //TODO Error Message
//                throw new ParsingException("Invalid place to import external content");
//            }
//        } else {
//            // TODO Error Message
//            throw new ParsingException("Unable to find the external content specification");
//        }
//    }

    /**
     * Finds a List of variable sets within a string. If the end of a set
     * can't be determined then it will continue to parse the following
     * lines until the end is found.
     *
     * @param parserData
     * @param input      The string to find the sets in.
     * @param startDelim The starting character of the set.
     * @param endDelim   The ending character of the set.
     * @return A list of VariableSets that contain the contents of each set
     *         and the start and end position of the set.
     */
    protected List<VariableSet> findVariableSets(final ParserData parserData, final String input, final char startDelim,
            final char endDelim) {
        final StringBuilder varLine = new StringBuilder(input);
        final List<VariableSet> retValue = new ArrayList<VariableSet>();
        int startPos = 0;
        VariableSet set = ProcessorUtilities.findVariableSet(input, startDelim, endDelim, startPos);

        while (set != null && set.getContents() != null) {
            /*
             * Check if we've found the end of a set. If we have add the set to the
             * list and try and see if another set exists. If not then get the next line
             * in the content spec and keep processing the set until the end of the set
             * is found or the end of the content spec.
             */
            if (set.getEndPos() != null) {
                retValue.add(set);

                final String nextLine = parserData.getLines().peek();
                startPos = set.getEndPos() + 1;
                set = ProcessorUtilities.findVariableSet(varLine.toString(), startDelim, endDelim, startPos);

                /*
                 * If the next set and/or its contents are empty then it means we found all the sets
                 * for the input line. However the next line in the content spec maybe a continuation
                 * but we couldn't find it originally because of a missing separator. So peek at the next
                 * line and see if it's a continuation (ie another relationship) and if it is then add the
                 * line and continue to find sets.
                 */
                if ((set == null || set.getContents() == null) && (nextLine != null && nextLine.trim().toUpperCase(Locale.ENGLISH).matches(
                        "^\\" + startDelim + "[ ]*(R|L|P|T|B).*"))) {
                    final String line = parserData.getLines().poll();
                    parserData.setLineCount(parserData.getLineCount() + 1);

                    if (line != null) {
                        varLine.append("\n").append(line);

                        set = ProcessorUtilities.findVariableSet(varLine.toString(), startDelim, endDelim, startPos);
                    }
                }
            } else {
                final String line = parserData.getLines().poll();
                parserData.setLineCount(parserData.getLineCount() + 1);

                if (line != null) {
                    varLine.append("\n").append(line);

                    set = ProcessorUtilities.findVariableSet(varLine.toString(), startDelim, endDelim, startPos);
                } else {
                    retValue.add(set);
                    break;
                }
            }
        }
        return retValue;
    }

    protected static class ParserData {
        private int spaces = 2;
        private ContentSpec contentSpec = new ContentSpec();
        private int indentationLevel = 0;
        private HashMap<String, SpecTopic> specTopics = new HashMap<String, SpecTopic>();
        private HashMap<String, Level> levels = new HashMap<String, Level>();
        private HashMap<String, Level> targetLevels = new HashMap<String, Level>();
        private HashMap<String, Level> externalTargetLevels = new HashMap<String, Level>();
        private HashMap<String, SpecTopic> targetTopics = new HashMap<String, SpecTopic>();
        private HashMap<String, List<Relationship>> topicRelationships = new HashMap<String, List<Relationship>>();
        private HashMap<String, List<Relationship>> levelRelationships = new HashMap<String, List<Relationship>>();
        private ArrayList<Process> processes = new ArrayList<Process>();
        private Set<String> parsedMetaDataKeys = new HashSet<String>();
        private Level lvl = contentSpec.getBaseLevel();
        private int lineCount = 0;
        private LinkedList<String> lines = new LinkedList<String>();

        public ParserData() {
            lvl.setUniqueId("L0");
            levels.put("L0", lvl);
        }

        public int getLineCount() {
            return lineCount;
        }

        public void setLineCount(int lineCount) {
            this.lineCount = lineCount;
        }

        public int getIndentationLevel() {
            return indentationLevel;
        }

        public void setIndentationLevel(int indentationLevel) {
            this.indentationLevel = indentationLevel;
        }

        public int getIndentationSize() {
            return spaces;
        }

        public void setIndentationSize(int indentationSize) {
            spaces = indentationSize;
        }

        public Level getCurrentLevel() {
            return lvl;
        }

        public void setCurrentLevel(final Level level) {
            lvl = level;
        }

        /**
         * Get the list of lines that are to be processed from a Content Specification File/String.
         *
         * @return The list of lines in the content spec.
         */
        public LinkedList<String> getLines() {
            return lines;
        }

        public void addLine(final String line) {
            lines.add(line);
        }

        public Set<String> getParsedMetaDataKeys() {
            return parsedMetaDataKeys;
        }

        /**
         * Gets the Content Specification Topics inside of a content specification
         *
         * @return The mapping of topics to their unique Content Specification Topic ID's
         */
        public HashMap<String, SpecTopic> getSpecTopics() {
            return specTopics;
        }

        /**
         * Gets the Content Specification Levels/Containers inside of a content specification
         *
         * @return The mapping of levels to their line numbers.
         */
        public HashMap<String, Level> getLevels() {
            return levels;
        }

        /**
         * Gets a list of processes that were parsed in the content specification
         *
         * @return A List of Processes
         */
        public List<Process> getProcesses() {
            return processes;
        }

        /**
         * Gets a list of Content Specification Topics that were parsed as being targets
         *
         * @return A list of Content Specification Topics mapped by their Target ID.
         */
        public HashMap<String, SpecTopic> getTargetTopics() {
            return targetTopics;
        }

        /**
         * Gets a list of Levels that were parsed as being targets.
         *
         * @return A List of Levels mapped by their Target ID.
         */
        public HashMap<String, Level> getTargetLevels() {
            return targetLevels;
        }

        /**
         * Gets a list of External Levels that were parsed as being targets.
         *
         * @return A List of External Levels mapped by their Target ID.
         */
        public HashMap<String, Level> getExternalTargetLevels() {
            return externalTargetLevels;
        }

        /**
         * Gets the relationships that were created for topics when parsing the Content Specification.
         *
         * @return The map of Unique id's to relationships
         */
        public HashMap<String, List<Relationship>> getTopicRelationships() {
            return topicRelationships;
        }

        /**
         * Gets the relationships that were created for levels when parsing the Content Specification.
         *
         * @return The map of Unique id's to relationships
         */
        public HashMap<String, List<Relationship>> getLevelRelationships() {
            return levelRelationships;
        }

        /**
         * Get the Content Specification object that represents a Content Specification
         *
         * @return The Content Specification object representation.
         */
        public ContentSpec getContentSpec() {
            return contentSpec;
        }

        public void setContentSpec(final ContentSpec contentSpec) {
            this.contentSpec = contentSpec;
        }
    }
}
