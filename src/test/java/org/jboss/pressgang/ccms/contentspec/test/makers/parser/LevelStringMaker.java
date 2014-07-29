/*
  Copyright 2011-2014 Red Hat

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

    public static final Instantiator<String> LevelString = new Instantiator<String>() {
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
