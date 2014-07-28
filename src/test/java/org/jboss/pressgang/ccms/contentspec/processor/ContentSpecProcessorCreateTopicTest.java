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

    private String locale = "en-US";

    @Test
    public void shouldReturnNullWhenSpecTopicIsDuplicate() {
        // Given a spec topic that is a duplicate
        final SpecTopic specTopic = make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, "X1")));

        // When creating the topic
        TopicWrapper topic = null;
        try {
            topic = processor.createTopicEntity(providerFactory, specTopic, DOCBOOK_45, locale);
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
            topic = processor.createTopicEntity(providerFactory, specTopic, DOCBOOK_45, locale);
        } catch (Exception e) {
            fail("Creating a topic should not have thrown an exception");
        }

        // Then check that the spec topic is null
        assertNull(topic);
    }
}
