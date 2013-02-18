package org.jboss.pressgang.ccms.contentspec.test.makers.parser;

import static com.natpryce.makeiteasy.Property.newProperty;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;

public class TopicStringMaker {
    public static final Property<String, Integer> indentation = newProperty();
    public static final Property<String, String> title = newProperty();
    public static final Property<String, String> id = newProperty();
    public static final Property<String, String> revision = newProperty();
    public static final Property<String, Boolean> missingOpeningBracket = newProperty();
    public static final Property<String, Boolean> missingClosingBracket = newProperty();
    public static final Property<String, Boolean> missingVariable = newProperty();
    public static final Property<String, String> url = newProperty();
    public static final Property<String, String> description = newProperty();

    public static final Instantiator<String> TopicString = new Instantiator<String>() {
        @Override
        public String instantiate(PropertyLookup<String> lookup) {
            final StringBuilder retValue = new StringBuilder();
            final String nullString = null;

            // add the topics indentation
            final Integer indentationLevel = lookup.valueOf(indentation, 0);
            for (int i = 0; i < indentationLevel; i++) {
                retValue.append("  ");
            }

            // add the topics title
            String title = lookup.valueOf(TopicStringMaker.title, nullString);
            if (title != null) {
                retValue.append(title).append(" ");
            }

            // add the opening bracket
            if (!lookup.valueOf(missingOpeningBracket, false)) {
                retValue.append("[");
            }

            // add the topics id
            String id = lookup.valueOf(TopicStringMaker.id, nullString);
            if (id != null) {
                retValue.append(id);
            }

            // add the topics revision
            String revision = lookup.valueOf(TopicStringMaker.revision, nullString);
            if (revision != null) {
                retValue.append(", rev: ").append(revision);
            }

            // add the topics url
            String url = lookup.valueOf(TopicStringMaker.url, nullString);
            if (url != null) {
                retValue.append(", URL = ").append(url).append(" ");
            }

            // add the topics description
            String description = lookup.valueOf(TopicStringMaker.description, nullString);
            if (url != null) {
                retValue.append(", Description = ").append(description).append(" ");
            }

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
    };
}
