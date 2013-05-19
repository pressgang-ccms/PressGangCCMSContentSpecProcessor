package org.jboss.pressgang.ccms.contentspec.test.makers.shared;

import static com.natpryce.makeiteasy.Property.newProperty;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static org.apache.commons.lang.math.RandomUtils.nextInt;

import java.util.ArrayList;
import java.util.List;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
public class SpecTopicMaker {
    public static final Property<SpecTopic, String> id = newProperty();
    public static final Property<SpecTopic, String> uniqueId = newProperty();
    public static final Property<SpecTopic, String> targetId = newProperty();
    public static final Property<SpecTopic, String> title = newProperty();
    public static final Property<SpecTopic, Integer> lineNumber = newProperty();
    public static final Property<SpecTopic, String> specLine = newProperty();
    public static final Property<SpecTopic, String> type = newProperty();
    public static final Property<SpecTopic, String> assignedWriter = newProperty();
    public static final Property<SpecTopic, Integer> revision = newProperty();
    public static final Property<SpecTopic, String> description = newProperty();
    public static final Property<SpecTopic, String> condition = newProperty();
    public static final Property<SpecTopic, List<String>> tags = newProperty();
    public static final Property<SpecTopic, List<String>> removeTags = newProperty();
    public static final Property<SpecTopic, List<String>> urls = newProperty();

    public static final Instantiator<SpecTopic> SpecTopic = new Instantiator<org.jboss.pressgang.ccms.contentspec.SpecTopic>() {
        @Override
        public SpecTopic instantiate(PropertyLookup<SpecTopic> lookup) {
            SpecTopic specTopic = new SpecTopic(lookup.valueOf(id, randomNumeric(4)), lookup.valueOf(title, randomAlphanumeric(10)),
                    lookup.valueOf(lineNumber, nextInt()), lookup.valueOf(specLine, randomAlphanumeric(10)),
                    lookup.valueOf(type, randomAlphanumeric(10)));
            specTopic.setRevision(lookup.valueOf(revision, nextInt()));
            specTopic.setAssignedWriter(lookup.valueOf(assignedWriter, (String) null));
            specTopic.setDescription(lookup.valueOf(description, (String) null));
            specTopic.setUniqueId(lookup.valueOf(uniqueId, (String) null));
            specTopic.setTags(lookup.valueOf(tags, new ArrayList<String>()));
            specTopic.setRemoveTags(lookup.valueOf(removeTags, new ArrayList<String>()));
            specTopic.setSourceUrls(lookup.valueOf(urls, new ArrayList<String>()));
            specTopic.setTargetId(lookup.valueOf(targetId, (String) null));
            specTopic.setConditionStatement(lookup.valueOf(condition, (String) null));
            return specTopic;
        }
    };
}
