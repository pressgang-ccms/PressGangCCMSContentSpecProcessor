package org.jboss.pressgang.ccms.contentspec.test.makers.parser;

import static com.natpryce.makeiteasy.Property.newProperty;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;
import org.jboss.pressgang.ccms.contentspec.enums.LevelType;

public class LevelStringMaker {
    public static final Property<String, Integer> indentation = newProperty();
    public static final Property<String, LevelType> levelType = newProperty();
    public static final Property<String, String> title = newProperty();
    public static final Property<String, Boolean> missingOpeningBracket = newProperty();
    public static final Property<String, Boolean> missingClosingBracket = newProperty();
    public static final Property<String, String> url = newProperty();
    public static final Property<String, String> description = newProperty();
    public static final Property<String, String> targetId = newProperty();

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

            // add the chapter type
            LevelType type = lookup.valueOf(LevelStringMaker.levelType, LevelType.CHAPTER);
            retValue.append(type.getTitle()).append(": ");

            // add the level title
            String title = lookup.valueOf(LevelStringMaker.title, nullString);
            if (title != null) {
                retValue.append(title).append(" ");
            }

            // add the opening bracket
            if (!lookup.valueOf(missingOpeningBracket, false)) {
                retValue.append("[");
            }

            // add the levels url
            String url = lookup.valueOf(LevelStringMaker.url, nullString);
            if (url != null) {
                retValue.append(", URL = ").append(url).append(" ");
            }

            // add the levels description
            String description = lookup.valueOf(LevelStringMaker.description, nullString);
            if (url != null) {
                retValue.append(", Description = ").append(description).append(" ");
            }

            // add the closing bracket
            if (!lookup.valueOf(missingClosingBracket, false)) {
                retValue.append("]");
            }

            // add the target id
            String targetId = lookup.valueOf(LevelStringMaker.targetId, nullString);
            if (targetId != null) {
                retValue.append(targetId);
            }

            return retValue.toString();
        }
    };
}
