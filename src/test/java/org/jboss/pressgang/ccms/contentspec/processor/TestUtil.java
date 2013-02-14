package org.jboss.pressgang.ccms.contentspec.processor;

import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.enums.BookType;

import java.util.List;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextInt;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
public class TestUtil {

    public static ContentSpec makeAValidContentSpec() {
        ContentSpec contentSpec = new ContentSpec(randomAlphanumeric(10), randomAlphanumeric(10), "1-A", randomAlphanumeric(10));
        contentSpec.setDtd("Docbook 4.5");
        contentSpec.setBookType(BookType.BOOK);
        List<String> preProcessedText = contentSpec.getPreProcessedText();
        preProcessedText.add(randomAlphanumeric(10));
        contentSpec.setPreProcessedTextForLine(randomAlphanumeric(10), 1);
        Level baseLevel = contentSpec.getBaseLevel();
        List<SpecTopic> specTopics = baseLevel.getSpecTopics();
        specTopics.add(makeAValidSpecTopic());
        return contentSpec;
    }

    public static SpecTopic makeAValidSpecTopic() {
        return new SpecTopic(randomAlphanumeric(4), randomAlphanumeric(10), nextInt(), randomAlphanumeric(10), randomAlphanumeric(5));
    }
}
