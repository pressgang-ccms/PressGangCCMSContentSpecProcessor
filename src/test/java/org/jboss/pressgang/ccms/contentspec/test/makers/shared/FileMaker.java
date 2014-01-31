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
