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
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

import java.util.Random;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;
import org.jboss.pressgang.ccms.contentspec.File;

public class FileMaker {
    public static final Property<File, String> title = newProperty();
    public static final Property<File, Integer> id = newProperty();
    public static final Property<File, Integer> revision = newProperty();
    public static final Property<File, String> uniqueId = newProperty();

    public static final Instantiator<File> File = new Instantiator<File>() {
        @Override
        public File instantiate(PropertyLookup<File> lookup) {
            File file = new File(lookup.valueOf(title, randomAlphanumeric(10)), lookup.valueOf(id, new Random().nextInt()));
            file.setRevision(lookup.valueOf(revision, (Integer) null));
            file.setUniqueId(lookup.valueOf(uniqueId, (String) null));
            return file;
        }
    };
}
