package com.redhat.contentspec.processor.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.redhat.contentspec.processor.structures.VariableSet;
import org.jboss.pressgang.ccms.rest.v1.entities.RESTTagV1;
import org.jboss.pressgang.ccms.rest.v1.entities.join.RESTCategoryInTagV1;
import org.jboss.pressgang.ccms.utils.common.StringUtilities;

public class ProcessorUtilities {
    private static final Pattern LEFT_SQUARE_BRACKET_PATTERN = Pattern.compile("\\\\\\[");
    private static final Pattern RIGHT_SQUARE_BRACKET_PATTERN = Pattern.compile("\\\\\\]");
    private static final Pattern LEFT_BRACKET_PATTERN = Pattern.compile("\\\\\\(");
    private static final Pattern RIGHT_BRACKET_PATTERN = Pattern.compile("\\\\\\)");
    private static final Pattern COLON_PATTERN = Pattern.compile("\\\\:");
    private static final Pattern COMMA_PATTERN = Pattern.compile("\\\\,");
    private static final Pattern EQUALS_PATTERN = Pattern.compile("\\\\=");
    private static final Pattern PLUS_PATTERN = Pattern.compile("\\\\\\+");
    private static final Pattern MINUS_PATTERN = Pattern.compile("\\\\-");

    /**
     * Converts a list of tags into a mapping of categories to tags. The key is the Category and the value is a List
     * of Tags for that category.
     *
     * @param tags The List of tags to be converted.
     * @return The mapping of Categories to Tags.
     */
    public static Map<RESTCategoryInTagV1, List<RESTTagV1>> getCategoryMappingFromTagList(final List<RESTTagV1> tags) {
        final HashMap<RESTCategoryInTagV1, List<RESTTagV1>> mapping = new HashMap<RESTCategoryInTagV1, List<RESTTagV1>>();
        for (final RESTTagV1 tag : tags) {
            final List<RESTCategoryInTagV1> catList = tag.getCategories().returnItems();
            if (catList != null) {
                for (final RESTCategoryInTagV1 cat : catList) {
                    if (!mapping.containsKey(cat)) mapping.put(cat, new ArrayList<RESTTagV1>());
                    mapping.get(cat).add(tag);
                }
            }
        }
        return mapping;
    }

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
     * Replaces the escaped chars with their normal counterpart. Only replaces ('[', ']', '(', ')', ';', ',', '+', '-' and '=')
     *
     * @param input The string to have all its escaped characters replaced.
     * @return The input string with the escaped characters replaced back to normal.
     */
    public static String replaceEscapeChars(final String input) {
        if (input == null) return null;

        String retValue = LEFT_SQUARE_BRACKET_PATTERN.matcher(input).replaceAll("[");
        retValue = RIGHT_SQUARE_BRACKET_PATTERN.matcher(retValue).replaceAll("]");
        retValue = LEFT_BRACKET_PATTERN.matcher(retValue).replaceAll("(");
        retValue = RIGHT_BRACKET_PATTERN.matcher(retValue).replaceAll(")");
        retValue = COLON_PATTERN.matcher(retValue).replaceAll(":");
        retValue = COMMA_PATTERN.matcher(retValue).replaceAll(",");
        retValue = EQUALS_PATTERN.matcher(retValue).replaceAll("=");
        retValue = PLUS_PATTERN.matcher(retValue).replaceAll("+");
        return MINUS_PATTERN.matcher(retValue).replaceAll("-");
    }
}
