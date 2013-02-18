package org.jboss.pressgang.ccms.contentspec.processor.utils;

import org.apache.log4j.Logger;
import org.jboss.pressgang.ccms.contentspec.processor.exceptions.InvalidKeyValueException;
import org.jboss.pressgang.ccms.contentspec.processor.structures.VariableSet;
import org.jboss.pressgang.ccms.utils.common.CollectionUtilities;
import org.jboss.pressgang.ccms.utils.common.StringUtilities;
import org.jboss.pressgang.ccms.utils.structures.Pair;

public class ProcessorUtilities {
    private static final Logger log = Logger.getLogger(ProcessorUtilities.class);

    /**
     * Finds a set of variables that are group by delimiters. It also skips nested
     * groups and returns them as part of the set so they can be processed separately.
     * eg. [var1, var2, [var3, var4], var5]
     *
     * @param input      The string to find the set for.
     * @param startDelim The starting delimiter for the set.
     * @param endDelim   The ending delimiter for the set.
     * @param startPos   The position to start searching from in the string.
     * @return A VariableSet object that contains the contents of the set, the start position
     *         in the string and the end position.
     */
    public static VariableSet findVariableSet(final String input, final char startDelim, final char endDelim, final int startPos) {
        final int startIndex = StringUtilities.indexOf(input, startDelim, startPos);
        int endIndex = StringUtilities.indexOf(input, endDelim, startPos);
        int nextStartIndex = StringUtilities.indexOf(input, startDelim, startIndex + 1);

        /*
           * Find the ending delimiter that matches the start delimiter. This is done
           * by checking to see if the next start delimiter is before the current end
           * delimiter. If that is the case then there is a nested set so look for the
           * next end delimiter.
           */
        while (nextStartIndex < endIndex && nextStartIndex != -1 && endIndex != -1) {
            final int prevEndIndex = endIndex;
            endIndex = StringUtilities.indexOf(input, endDelim, endIndex + 1);
            nextStartIndex = StringUtilities.indexOf(input, startDelim, prevEndIndex + 1);
        }

        // Build the resulting set object
        final VariableSet set = new VariableSet();

        if (endIndex == -1 && startIndex != -1) {
            set.setContents(input.substring(startIndex));
            set.setEndPos(null);
            set.setStartPos(startIndex);
        } else if (startIndex != -1) {
            set.setContents(input.substring(startIndex, endIndex + 1));
            set.setEndPos(endIndex);
            set.setStartPos(startIndex);
        } else {
            set.setContents(null);
            set.setEndPos(null);
            set.setStartPos(null);
        }
        return set;
    }

    /**
     * Validates a KeyValue pair for a content specification and then returns the processed key and value..
     *
     * @param keyValueString The string to be broken down and validated.
     * @return A Pair where the first value is the key and the second is the value.
     * @throws InvalidKeyValueException
     */
    public static Pair<String, String> getAndValidateKeyValuePair(final String keyValueString) throws InvalidKeyValueException {
        String tempInput[] = StringUtilities.split(keyValueString, '=', 2);
        // Remove the whitespace from each value in the split array
        tempInput = CollectionUtilities.trimStringArray(tempInput);
        if (tempInput.length >= 2) {
            return new Pair<String, String>(tempInput[0], StringUtilities.replaceEscapeChars(tempInput[1]));
        } else {
            throw new InvalidKeyValueException();
        }
    }
}
