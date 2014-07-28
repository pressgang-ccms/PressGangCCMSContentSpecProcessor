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

package org.jboss.pressgang.ccms.contentspec.test.makers.shared;

import static com.natpryce.makeiteasy.Property.newProperty;

import java.util.ArrayList;
import java.util.List;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;
import org.jboss.pressgang.ccms.contentspec.File;
import org.jboss.pressgang.ccms.contentspec.FileList;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;

public class FileListMaker {
    public static final Property<FileList, Integer> id = newProperty();
    public static final Property<FileList, String> uniqueId = newProperty();
    public static final Property<FileList, List<File>> files = newProperty();

    public static final Instantiator<FileList> FileList = new Instantiator<FileList>() {
        @Override
        public FileList instantiate(PropertyLookup<FileList> lookup) {
            FileList fileList = new FileList(CommonConstants.CS_FILE_TITLE, lookup.valueOf(files, new ArrayList<File>()));
            fileList.setUniqueId(lookup.valueOf(uniqueId, (String) null));
            return fileList;
        }
    };
}
