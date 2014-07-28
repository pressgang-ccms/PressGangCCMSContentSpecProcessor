/*
  Copyright 2011-2014 Red Hat

  This file is part of PresGang CCMS.

  PresGang CCMS is free software: you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  PresGang CCMS is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with PresGang CCMS.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.jboss.pressgang.ccms.contentspec.test.makers.parser;

import static com.natpryce.makeiteasy.Property.newProperty;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;

public class TopicStringMaker {
    public static final Property<String, Integer> indentation = newProperty();
    public static final Property<String, String> title = newProperty();
    public static final Property<String, String> id = newProperty();
    public static final Property<String, String> topicType = newProperty();
    public static final Property<String, String> revision = newProperty();
    public static final Property<String, Boolean> missingOpeningBracket = newProperty();
    public static final Property<String, Boolean> missingClosingBracket = newProperty();
    public static final Property<String, Boolean> missingVariable = newProperty();
    public static final Property<String, String> url = newProperty();
    public static final Property<String, String> description = newProperty();
    public static final Property<String, String> targetId = newProperty();
    public static final Property<String, String> relationship = newProperty();

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

            // add the topics type
            String type = lookup.valueOf(TopicStringMaker.topicType, nullString);
            if (type != null) {
                retValue.append(", ").append(type);
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

            // add the relationship
            String relationship = lookup.valueOf(TopicStringMaker.relationship, nullString);
            if (relationship != null) {
                retValue.append(relationship);
            }

            // add the target id
            String targetId = lookup.valueOf(TopicStringMaker.targetId, nullString);
            if (targetId != null) {
                retValue.append(targetId);
            }

            return retValue.toString();
        }
    };
}
