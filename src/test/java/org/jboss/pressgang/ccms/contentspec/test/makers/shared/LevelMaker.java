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

package org.jboss.pressgang.ccms.contentspec.test.makers.shared;

import static com.natpryce.makeiteasy.Property.newProperty;
import static java.util.Collections.EMPTY_LIST;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

import java.util.List;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.enums.LevelType;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
public class LevelMaker {
    public static final Property<Level, String> title = newProperty();
    public static final Property<Level, String> uniqueId = newProperty();
    public static final Property<Level, String> targetId = newProperty();
    public static final Property<Level, String> condition = newProperty();
    public static final Property<Level, LevelType> levelType = newProperty();
    public static final Property<Level, List<String>> tags = newProperty();

    public static final Instantiator<Level> Level = new Instantiator<org.jboss.pressgang.ccms.contentspec.Level>() {
        @Override
        public Level instantiate(PropertyLookup<Level> lookup) {
            Level level = new Level(lookup.valueOf(title, randomAlphanumeric(10)), lookup.valueOf(levelType, LevelType.BASE));
            level.setTags(lookup.valueOf(tags, EMPTY_LIST));
            level.setTargetId(lookup.valueOf(targetId, (String) null));
            level.setUniqueId(lookup.valueOf(uniqueId, (String) null));
            level.setConditionStatement(lookup.valueOf(condition, (String) null));
            return level;
        }
    };
}
