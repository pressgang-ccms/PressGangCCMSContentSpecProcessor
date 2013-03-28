package org.jboss.pressgang.ccms.contentspec.test.makers.validator;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.Property.newProperty;
import static java.lang.String.valueOf;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextInt;

import java.util.List;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.enums.BookType;
import org.jboss.pressgang.ccms.contentspec.test.makers.shared.SpecTopicMaker;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
public class ContentSpecMaker {
    public static final Property<ContentSpec, String> title = newProperty();
    public static final Property<ContentSpec, String> product = newProperty();
    public static final Property<ContentSpec, String> version = newProperty();
    public static final Property<ContentSpec, String> copyrightHolder = newProperty();
    public static final Property<ContentSpec, String> copyrightYear = newProperty();
    public static final Property<ContentSpec, String> dtd = newProperty();
    public static final Property<ContentSpec, BookType> bookType = newProperty();
    public static final Property<ContentSpec, String> bookVersion = newProperty();
    public static final Property<ContentSpec, String> edition = newProperty();
    public static final Property<ContentSpec, Integer> id = newProperty();
    public static final Property<ContentSpec, String> checksum = newProperty();

    public static final Instantiator<ContentSpec> ContentSpec = new Instantiator<org.jboss.pressgang.ccms.contentspec.ContentSpec>() {
        @Override
        public ContentSpec instantiate(PropertyLookup<ContentSpec> lookup) {
            ContentSpec contentSpec = new ContentSpec(lookup.valueOf(title, randomAlphanumeric(10)));
            contentSpec.setProduct(lookup.valueOf(product, randomAlphanumeric(10)));
            contentSpec.setVersion(lookup.valueOf(version, "1-A"));
            contentSpec.setCopyrightHolder(lookup.valueOf(copyrightHolder, randomAlphanumeric(10)));
            contentSpec.setCopyrightYear(lookup.valueOf(copyrightYear, (String) null));
            contentSpec.setDtd(lookup.valueOf(dtd, "Docbook 4.5"));
            contentSpec.setBookType(lookup.valueOf(bookType, BookType.BOOK));
            contentSpec.setBookVersion(lookup.valueOf(bookVersion, valueOf(nextInt())));
            contentSpec.setEdition(lookup.valueOf(edition, valueOf(nextInt())));
            contentSpec.setId(lookup.valueOf(id, nextInt()));
            contentSpec.setChecksum(lookup.valueOf(checksum, randomAlphanumeric(10)));
            Level baseLevel = contentSpec.getBaseLevel();
            List<SpecTopic> specTopics = baseLevel.getSpecTopics();
            specTopics.add(make(a(SpecTopicMaker.SpecTopic)));
            return contentSpec;
        }
    };
}
