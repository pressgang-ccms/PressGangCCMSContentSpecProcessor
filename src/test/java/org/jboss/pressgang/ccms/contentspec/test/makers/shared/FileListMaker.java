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
