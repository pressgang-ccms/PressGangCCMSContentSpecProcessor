package org.jboss.pressgang.ccms.contentspec.test.makers.parser;

import static com.natpryce.makeiteasy.Property.newProperty;

import java.util.ArrayList;
import java.util.List;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;

public class TopicRelationshipStringMaker {
    public static final Property<String, Integer> indentation = newProperty();
    public static final Property<String, Boolean> longRelationship = newProperty();
    public static Property<String, String> relationshipType = newProperty();
    public static final Property<String, Boolean> missingOpeningBracket = newProperty();
    public static final Property<String, Boolean> missingClosingBracket = newProperty();
    public static final Property<String, Boolean> missingVariable = newProperty();
    public static final Property<String, Boolean> missingColon = newProperty();
    public static final Property<String, List<String>> relationships = newProperty();

    public static Instantiator<String> TopicRelationshipString = new Instantiator<String>() {
        @Override
        public String instantiate(PropertyLookup<String> lookup) {
            final StringBuilder retValue = new StringBuilder();
            final String nullString = null;

            // add the indentation
            final Integer indentationLevel = lookup.valueOf(indentation, 0);
            for (int i = 0; i < indentationLevel; i++) {
                retValue.append("  ");
            }

            // add the opening bracket
            if (!lookup.valueOf(missingOpeningBracket, false)) {
                retValue.append("[");
            }

            // add the relationship type
            String relationshipType = lookup.valueOf(TopicRelationshipStringMaker.relationshipType, nullString);
            if (relationshipType != null) {
                retValue.append(relationshipType);
            }

            // add the closing bracket
            if (!lookup.valueOf(missingColon, false)) {
                retValue.append(":");
            }

            // Add the topic relationships
            retValue.append(
                    generateTopicListString(lookup.valueOf(relationships, new ArrayList<String>()), lookup.valueOf(longRelationship, false),
                            indentationLevel));

            // add the missing variable
            if (lookup.valueOf(missingVariable, false)) {
                retValue.append(", ");
            }

            // add the closing bracket
            if (!lookup.valueOf(missingClosingBracket, false)) {
                retValue.append("]");
            }

            return retValue.toString();
        }

        protected String generateTopicListString(List<String> topicList, boolean longRelationship, int indentationLevel) {
            final StringBuilder retValue = new StringBuilder();

            // add the indentation
            StringBuilder indentation = new StringBuilder();
            for (int i = 0; i < indentationLevel; i++) {
                indentation.append("  ");
            }

            for (final String topic : topicList) {
                if (longRelationship) {
                    retValue.append("\n").append(indentation.toString()).append("  ");
                } else {
                    retValue.append(" ");
                }
                retValue.append(topic).append(",");
            }

            // Remove the last comma
            retValue.deleteCharAt(retValue.length() - 1);

            return retValue.toString();
        }
    };
}
