package com.redhat.contentspec.processor.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.code.regexp.NamedMatcher;
import com.google.code.regexp.NamedPattern;
import com.redhat.contentspec.ContentSpec;
import com.redhat.contentspec.SpecTopic;
import com.redhat.contentspec.processor.constants.ProcessorConstants;
import com.redhat.ecs.commonutils.HashUtilities;
import com.redhat.ecs.commonutils.StringUtilities;
import com.redhat.topicindex.rest.entities.interfaces.RESTCategoryV1;
import com.redhat.topicindex.rest.entities.interfaces.RESTTagV1;

public class ProcessorUtilities {
	
	private static final Logger log = Logger.getLogger(ProcessorUtilities.class);

	/**
	 * Converts a list of tags into a mapping of categories to tags. The key is the Category and the value is a List of Tags for that category.
	 * 
	 * @param tags The List of tags to be converted.
	 * @return The mapping of Categories to Tags.
	 */
	public static Map<RESTCategoryV1, List<RESTTagV1>> getCategoryMappingFromTagList(final List<RESTTagV1> tags) {
		final HashMap<RESTCategoryV1, List<RESTTagV1>> mapping = new HashMap<RESTCategoryV1, List<RESTTagV1>>();
		for (final RESTTagV1 tag: tags) {
			final List<RESTCategoryV1> catList = tag.getCategories().getItems();
			if (catList != null) {
				for (final RESTCategoryV1 cat: catList) {
					if (!mapping.containsKey(cat)) mapping.put(cat, new ArrayList<RESTTagV1>());
					mapping.get(cat).add(tag);
				}
			}
		}
		return mapping;
	}
	
	/**
	 * Creates a Post Processed Content Specification from a processed ContentSpec object.
	 * 
	 * @param contentSpec The ContenSpec object to create the Post Processed Content Specification for. 
	 * @param specTopics A HashMap of the all the Content Specification Topics that can exist in the Content Specification. The key is the Topics ID.
	 * @param editing Whether the content specification is being edited or created.
	 * @return A string that contains the Post Content Specification or null if an error occurred.
	 */
	public static String generatePostContentSpec(ContentSpec contentSpec, HashMap<String, SpecTopic> specTopics, boolean editing){
		String output = "ID=" + contentSpec.getId() + "\n";
		NamedPattern pattern1 = NamedPattern.compile("\\[[ ]*(?<" + ProcessorConstants.TOPIC_ID_CONTENTS + ">[0-9]+)[ ]*(,|\\])");
		NamedPattern pattern2 = NamedPattern.compile("\\[[ ]*(?<" + ProcessorConstants.TOPIC_ID_CONTENTS + ">N)[ ]*,");
		NamedPattern pattern3 = NamedPattern.compile("(B:|P:|R:|NEXT:|PREV:|,|\\[)[ ]*(?<" + ProcessorConstants.TOPIC_ID_CONTENTS + ">N[0-9]+)[ ]*(?=(,|\\]))");
		NamedPattern pattern4 = NamedPattern.compile("(B:|P:|R:|NEXT:|PREV:|,|\\[)[ ]*(?<" + ProcessorConstants.TOPIC_ID_CONTENTS + ">X[0-9]+)[ ]*(?=(,|\\]))");
		NamedPattern pattern5 = NamedPattern.compile("(B:|P:|R:|NEXT:|PREV:|,|\\[)[ ]*(?<" + ProcessorConstants.TOPIC_ID_CONTENTS + ">C[0-9]+)[ ]*(?=(,|\\]))");
		NamedPattern pattern6 = NamedPattern.compile("(B:|P:|R:|NEXT:|PREV:|,|\\[)[ ]*(?<" + ProcessorConstants.TOPIC_ID_CONTENTS + ">XC[0-9]+)[ ]*(?=(,|\\]))");
		int count = 1;
		if (editing) count += 2;
		// For each line in the CS check if it matches each pattern and then do an action depending on what pattern is found
		for (String line: contentSpec.getPreProcessedText()) {
			if (line.trim().matches("^#.*")) {
				count++;
				output += line + "\n";
				continue;
			}
			log.debug(line);
			// Existing Topic
			NamedMatcher m = pattern1.matcher(line.toUpperCase());
			while (m.find()) {
				log.debug("Pattern1");
			    for (String key: specTopics.keySet()) {
			    	if (specTopics.get(key).getLineNumber() == count) {
			    		if (m.group().startsWith("[")) {
			    			line = stripVariables(line, specTopics.get(key).getDBId(), specTopics.get(key).getTitle());
			    		}
			    		break;
			    	}
			    }
			}
			// New Topic without an identifying number
			m = pattern2.matcher(line.toUpperCase());
			while (m.find()) {
				log.debug("Pattern2");
			    for (String key: specTopics.keySet()) {
			    	if (specTopics.get(key).getLineNumber() == count) {
			    		//line = line.replaceAll(s, "[" + Integer.toString(relatedTopics.get(key).getDBId()) + ",");
			    		line = stripVariables(line, specTopics.get(key).getDBId(), null);
			    		break;
			    	}
			    }
			}
			// New Topic with an identifying number
			m = pattern3.matcher(line.toUpperCase());
			while (m.find()) {
				log.debug("Pattern3");
				String s = m.group(ProcessorConstants.TOPIC_ID_CONTENTS);
			    log.debug(s);
			    if (m.group().startsWith("[")) {
			    	line = stripVariables(line, specTopics.get(s).getDBId(), null);
			    	// Add the target id that was created during relationship processing if one exists
			    	if (specTopics.get(s).getTargetId() != null && !line.matches("^.*\\[[ ]*" + specTopics.get(s).getTargetId() + "[ ]*\\].*$")) {
			    		line += " [" + specTopics.get(s).getTargetId() + "]";
			    	}
			    }
			    line = line.replace(s, specTopics.get(s).getTargetId() == null ? Integer.toString(specTopics.get(s).getDBId()) : specTopics.get(s).getTargetId());
			}
			// Duplicated Topic
			m = pattern4.matcher(line.toUpperCase());
			while (m.find()) {
				log.debug("Pattern4");
				String s = m.group(ProcessorConstants.TOPIC_ID_CONTENTS);
			    String key = s.replace('X', 'N');
			    if (m.group().startsWith("[")) {
			    	line = stripVariables(line, specTopics.get(key).getDBId(), specTopics.get(key).getTitle());
			    }
			    line = line.replace(s, Integer.toString(specTopics.get(key).getDBId()));
			}
			// Cloned Topic
			m = pattern5.matcher(line.toUpperCase());
			while (m.find()) {
				log.debug("Pattern5");
				String s = m.group(ProcessorConstants.TOPIC_ID_CONTENTS);
			    for (String key: specTopics.keySet()) {
			    	if (specTopics.get(key).getLineNumber() == count) {
			    		if (m.group().startsWith("[")) {
			    			line = stripVariables(line, specTopics.get(key).getDBId(), specTopics.get(key).getTitle());
			    		}
			    		line = line.replace(s, Integer.toString(specTopics.get(key).getDBId()));
			    		break;
			    	}
			    }
			}
			// Duplicated Cloned Topic
			m = pattern6.matcher(line.toUpperCase());
			while (m.find()) {
				log.debug("Pattern6");
				String s = m.group(ProcessorConstants.TOPIC_ID_CONTENTS);
			    // Remove the X
			    String clonedId = s.substring(1);
			    for (String key: specTopics.keySet()) {
			    	if (key.matches("^[0-9]+-" + clonedId + "$")) {
			    		if (m.group().startsWith("[")) {
			    			line = stripVariables(line, specTopics.get(key).getDBId(), specTopics.get(key).getTitle());
			    		}
			    		line = line.replace(s, Integer.toString(specTopics.get(key).getDBId()));
			    		break;
			    	}
			    }
			}
			count++;
			
			output += line + "\n";
		}
		return "CHECKSUM=" + HashUtilities.generateMD5(output) + "\n" + output;
	}
    
    /**
     * Removes all the variables from a topics Content Specification line except the database ID.
     * 
     * @param input The line to remove the variables from.
     * @param DBId The database ID of the topic for the specified line.
     * @param topicTitle The title of the topic if it is to be replaced.
     * @return The line with all variables removed.
     */
    private static String stripVariables(String input, int DBId, String topicTitle) {
    	String regex = String.format(ProcessorConstants.BRACKET_NAMED_PATTERN, '[', ']');
    	NamedPattern bracketPattern = NamedPattern.compile(regex);
		NamedMatcher matcher = bracketPattern.matcher(input);
		// Find the contents of the brackets that aren't a relationship or target
		String replacementTarget = null;
		while (matcher.find()) {
			String variableSet = matcher.group(ProcessorConstants.BRACKET_CONTENTS).replaceAll("\n", "");
			if (!(variableSet.toUpperCase().matches(ProcessorConstants.RELATED_REGEX) || variableSet.toUpperCase().matches(ProcessorConstants.PREREQUISITE_REGEX)
					|| variableSet.toUpperCase().matches(ProcessorConstants.NEXT_REGEX) || variableSet.toUpperCase().matches(ProcessorConstants.PREV_REGEX)
					|| variableSet.toUpperCase().matches(ProcessorConstants.TARGET_REGEX) || variableSet.toUpperCase().matches(ProcessorConstants.BRANCH_REGEX))) {
				// Normal set of variables that contains the ID and/or tags
				replacementTarget = variableSet;
			}
		}
		// Replace the non relationship variable set with the database id.
		input = input.replace(replacementTarget, Integer.toString(DBId));
		// Replace the title
		if (topicTitle != null && StringUtilities.indexOf(input, '[') != -1) {
			// Get the original whitespace to add to the line
			char[] chars = input.toCharArray();
            int i;
            for (i = 0; i < chars.length; i++) {
                    char c = chars[i];
                    if (!Character.isWhitespace(c)) break;
            }
			input = input.substring(0, i) + topicTitle + " " + input.substring(StringUtilities.indexOf(input, '['));
		}
		return input;
    }
}
