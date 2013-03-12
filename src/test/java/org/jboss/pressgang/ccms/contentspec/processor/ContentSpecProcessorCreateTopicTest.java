package org.jboss.pressgang.ccms.contentspec.processor;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.test.makers.shared.SpecTopicMaker;
import org.jboss.pressgang.ccms.wrapper.TopicWrapper;
import org.junit.Test;

public class ContentSpecProcessorCreateTopicTest extends ContentSpecProcessorTest {

    @Test
    public void shouldReturnNullWhenSpecTopicIsDuplicate() {
        // Given a spec topic that is a duplicate
        final SpecTopic specTopic = make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, "X1")));

        // When creating the topic
        TopicWrapper topic = null;
        try {
            topic = processor.createTopicEntity(providerFactory, specTopic);
        } catch (Exception e) {
            fail("Creating a topic should not have thrown an exception");
        }

        // Then check that the spec topic is null
        assertNull(topic);
    }

    @Test
    public void shouldReturnNullWhenSpecTopicIsCloneDuplicate() {
        // Given a spec topic that is a duplicate
        final SpecTopic specTopic = make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, "XC140")));

        // When creating the topic
        TopicWrapper topic = null;
        try {
            topic = processor.createTopicEntity(providerFactory, specTopic);
        } catch (Exception e) {
            fail("Creating a topic should not have thrown an exception");
        }

        // Then check that the spec topic is null
        assertNull(topic);
    }
}
