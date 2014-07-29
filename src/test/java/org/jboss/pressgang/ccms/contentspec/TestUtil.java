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

package org.jboss.pressgang.ccms.contentspec;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.math.RandomUtils.nextInt;

import java.util.ArrayList;
import java.util.List;

import org.jboss.pressgang.ccms.contentspec.enums.LevelType;

/**
 * Shared utilities methods to assist with testing.
 *
 * @author kamiller@redhat.com (Katie Miller)
 */
public class TestUtil {

    public static <T> T selectRandomListItem(List<T> list) {
        return list.get(nextInt(list.size()));
    }

    public static LevelType selectRandomLevelType() {
        ArrayList<LevelType> levelTypes = new ArrayList<LevelType>(asList(LevelType.values()));
        levelTypes.remove(LevelType.BASE);
        return selectRandomListItem(levelTypes);
    }
}
