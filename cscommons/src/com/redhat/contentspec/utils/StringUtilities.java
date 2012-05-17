package com.redhat.contentspec.utils;

import java.util.ArrayList;

/**
 * A set of Utilities that are used for manipulating Strings.
 */
public class StringUtilities extends com.redhat.ecs.commonutils.StringUtilities {

	/**
	 * Gets the first index of a character ignoring characters that have been escaped
	 * 
	 * @param input The string to be searched
	 * @param delim The character to be found
	 * @return The index of the found character or -1 if the character wasn't found
	 */
	public static int indexOf(String input, char delim) {
		if (input == null) return -1;
		int index = input.indexOf(delim);
		if (index != 0) {
			while (index != -1 && index != (input.length() - 1)) {
				if (input.charAt(index - 1) != '\\') break;
				index = input.indexOf(delim, index + 1);
			}	
		}
		return index;
	}
	
	/**
	 * Gets the first index of a character after fromIndex. Ignoring characters that have been escaped
	 * 
	 * @param input The string to be searched
	 * @param delim The character to be found
	 * @param fromIndex Start searching from this index
	 * @return The index of the found character or -1 if the character wasn't found
	 */
	public static int indexOf(String input, char delim, int fromIndex) {
		if (input == null) return -1;
		int index = input.indexOf(delim, fromIndex);
		if (index != 0) {
			while (index != -1 && index != (input.length() - 1)) {
				if (input.charAt(index - 1) != '\\') break;
				index = input.indexOf(delim, index + 1);
			}	
		}
		return index;
	}
	
	/**
	 * Gets the last index of a character ignoring characters that have been escaped
	 * 
	 * @param input The string to be searched
	 * @param delim The character to be found
	 * @return The index of the found character or -1 if the character wasn't found
	 */
	public static int lastIndexOf(String input, char delim) {
		if (input == null) return -1;
		int index = input.lastIndexOf(delim);
		while (index != -1 && index != 0) {
			if (input.charAt(index-1) != '\\') break;
			index = input.lastIndexOf(delim, index-1);
		}
		return index;
	}
	
	/**
	 * Gets the last index of a character starting at fromIndex. Ignoring characters that have been escaped
	 * 
	 * @param input The string to be searched
	 * @param delim The character to be found
	 * @param fromIndex Start searching from this index
	 * @return The index of the found character or -1 if the character wasn't found
	 */
	public static int lastIndexOf(String input, char delim, int fromIndex) {
		if (input == null) return -1;
		int index = input.lastIndexOf(delim, fromIndex);
		while (index != -1 && index != 0) {
			if (input.charAt(index-1) != '\\') break;
			index = input.lastIndexOf(delim, index-1);
		}
		return index;
	}
	
	/**
	 * Similar to the normal String split function. However this function ignores escaped characters (i.e. \[ ).
	 * 
	 * @param input The string to be split
	 * @param split The char to be used to split the input string
	 * @return An array of split strings
	 */
	public static String[] split(String input, char split) {
		int index = indexOf(input, split);
		int prevIndex = 0;
		ArrayList<String> output = new ArrayList<String>();
		if (index == -1) {
			output.add(input);
			return output.toArray(new String[1]);
		}
		while (index != -1) {
			output.add(input.substring(prevIndex, index));
			prevIndex = index + 1;
			index = indexOf(input, split, index+1);
		}
		output.add(input.substring(prevIndex, input.length()));
		return output.toArray(new String[1]);
	}
	
	/**
	 * Similar to the normal String split function. However this function ignores escaped characters (i.e. \[ ).
	 * 
	 * @param input The string to be split
	 * @param split The char to be used to split the input string
	 * @param limit The maximum number of times to split the string
	 * @return An array of split strings
	 */
	public static String[] split(String input, char split, int limit) {
		int index = indexOf(input, split);
		int prevIndex = 0, count = 1;
		ArrayList<String> output = new ArrayList<String>();
		if (index == -1) {
			output.add(input);
			return output.toArray(new String[1]);
		}
		while (index != -1 && count != limit) {
			output.add(input.substring(prevIndex, index));
			prevIndex = index + 1;
			index = indexOf(input, split, index+1);
			count++;
		}
		output.add(input.substring(prevIndex, input.length()));
		return output.toArray(new String[1]);
		
	}
	
	/**
	 * Trims an array of Strings to remove the whitespace. If the string is empty then its removed from the array.
	 * 
	 * @param input The array of strings to be trimmed
	 * @return The same array of strings but all elements have been trimmed of whitespace
	 */
	public static String[] trimArray(String[] input) {
		ArrayList<String> output = new ArrayList<String>();
		for (int i = 0; i < input.length; i++) {
			String s = input[i].trim();
			if (!s.equals("")) output.add(s);
		}
		return output.toArray(new String[0]);
	}
	
	/**
	 * Checks to see if a string entered is alpha numeric
	 * 
	 * @param input The string to be tested
	 * @return True if the string is alpha numeric otherwise false
	 */
	public static boolean isAlphanumeric(String input) {
		for (int i = 0; i < input.length(); i++) {
			if (!Character.isLetterOrDigit(input.charAt(i))) return false;
		}
		return true;
	}
	
	/**
	 * Replaces the escaped chars with their normal counterpart. Only replaces ('[', ']', '(', ')', ';', ',', '+', '-' and '=')
	 * 
	 * @param input The string to have all its escaped characters replaced.
	 * @return The input string with the escaped characters replaced back to normal.
	 */
	public static String replaceEscapeChars(String input) {
		String output = new String(input);
		output = output.replaceAll("\\\\\\[", "[");
		output = output.replaceAll("\\\\\\]", "]");
		output = output.replaceAll("\\\\\\(", "(");
		output = output.replaceAll("\\\\\\)", ")");
		output = output.replaceAll("\\\\:", ":");
		output = output.replaceAll("\\\\,", ",");
		output = output.replaceAll("\\\\=", "=");
		output = output.replaceAll("\\\\\\+", "+");
		output = output.replaceAll("\\\\-", "-");
		return output;
	}
	
	/**
	 * Checks a string to see if it has the UTF8 replacement character
	 * 
	 * @param input The string to be checked
	 * @return True of the replacement character is found otherwise false
	 */
	public static boolean hasInvalidUTF8Character(String input) {
		for (char c: input.toCharArray()) {
			if (c == 0xFFFD) return true;
		}
		return false;
	}
	
	/**
	 * Joins an array of strings together using the joiner string.
	 * 
	 * @param input The array of Strings to be joined
	 * @param joiner The string that is to be used to join the array.
	 * @return The string that was joined using the array. If the array is null or blank, then a blank string is returned.
	 */
	public static String join(String[] input, String joiner) {
		if (input == null || input.length == 0) return "";
		if (joiner == null) joiner = " ";
		String output = "";
		for (String s: input) {
			output += s + joiner;
		}
		return output.substring(0, output.length() - joiner.length());
	}
	
	/**
	 * Escapes a title so that it is alphanumeric or has a fullstop, underscore or hyphen only.
	 * 
	 * @param title The title to be escaped
	 * @return The escaped title string.
	 */
	public static String escapeTitle(String title) {
		return title.replaceAll(" ", "_").replaceAll("[^A-Za-z0-9\\._-]", "");
	}
	
	/**
	 * Converts a string so that it can be used in a regular expression.
	 * 
	 * @param input The string to be converted.
	 * @return An escaped string that can be used in a regular expression.
	 */
	public static String convertToRegexString(String input) {
		String output = new String(input);
		output = output.replaceAll("\\\\", "\\\\")
				.replaceAll("\\*", "\\*")
				.replaceAll("\\+", "\\+")
				.replaceAll("\\]", "\\]")
				.replaceAll("\\[", "\\[")
				.replaceAll("\\(", "\\(")
				.replaceAll("\\)", "\\)")
				.replaceAll("\\?", "\\?")
				.replaceAll("\\$", "\\$")
				.replaceAll("\\|", "\\|")
				.replaceAll("\\^", "\\^")
				.replaceAll("\\.", "\\.");
		return output;
	}
}
