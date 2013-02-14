package org.jboss.pressgang.ccms.contentspec.test.makers;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;
import org.apache.commons.lang.math.RandomUtils;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.enums.BookType;

import java.util.List;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.Property.newProperty;
import static java.lang.String.valueOf;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.jboss.pressgang.ccms.contentspec.test.makers.SpecTopicMaker.SpecTopic;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
public class ContentSpecMaker {
    public static final Property<ContentSpec, String> title = newProperty();
    public static final Property<ContentSpec, String> product = newProperty();
    public static final Property<ContentSpec, String> version = newProperty();
    public static final Property<ContentSpec, String> copyrightHolder = newProperty();
    public static final Property<ContentSpec, String> dtd = newProperty();
    public static final Property<ContentSpec, BookType> bookType = newProperty();
    public static final Property<ContentSpec, String> bookVersion = newProperty();
    public static final Property<ContentSpec, String> edition = newProperty();

    public static final Instantiator<ContentSpec> ContentSpec = new Instantiator<org.jboss.pressgang.ccms.contentspec.ContentSpec>() {
        @Override
        public ContentSpec instantiate(PropertyLookup<ContentSpec> lookup) {
            ContentSpec contentSpec = new ContentSpec(lookup.valueOf(title, randomAlphanumeric(10)));
            contentSpec.setProduct(lookup.valueOf(product, randomAlphanumeric(10)));
            contentSpec.setVersion(lookup.valueOf(version, "1-A"));
            contentSpec.setCopyrightHolder(lookup.valueOf(copyrightHolder, randomAlphanumeric(10)));
            contentSpec.setDtd(lookup.valueOf(dtd, "Docbook 4.5"));
            contentSpec.setBookType(lookup.valueOf(bookType, BookType.BOOK));
            contentSpec.setBookVersion(lookup.valueOf(bookVersion, valueOf(nextInt())));
            contentSpec.setEdition(lookup.valueOf(edition, valueOf(nextInt())));
            List<String> preProcessedText = contentSpec.getPreProcessedText();
            preProcessedText.add(randomAlphanumeric(10));
            contentSpec.setPreProcessedTextForLine(randomAlphanumeric(10), 1);
            Level baseLevel = contentSpec.getBaseLevel();
            List<SpecTopic> specTopics = baseLevel.getSpecTopics();
            specTopics.add(make(a(SpecTopic)));
            return contentSpec;
        }
    };
}
