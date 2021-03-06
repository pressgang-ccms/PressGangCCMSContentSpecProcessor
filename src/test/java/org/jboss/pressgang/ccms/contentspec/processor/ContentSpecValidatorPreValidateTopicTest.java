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

package org.jboss.pressgang.ccms.contentspec.processor;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import net.sf.ipsedixit.annotation.Arbitrary;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.enums.LevelType;
import org.jboss.pressgang.ccms.contentspec.test.makers.shared.LevelMaker;
import org.jboss.pressgang.ccms.contentspec.test.makers.shared.SpecTopicMaker;
import org.jboss.pressgang.ccms.contentspec.test.makers.validator.ContentSpecMaker;
import org.junit.Test;

public class ContentSpecValidatorPreValidateTopicTest extends ContentSpecValidatorTest {
    @Arbitrary Integer id;
    @Arbitrary Integer revision;
    @Arbitrary Integer revision2;
    @Arbitrary Integer lineNumber;

    @Test
    public void shouldFailAndLogErrorWhenSameTopicWithDifferentRevisions() {
        // Given a Content Spec
        ContentSpec contentSpec = make(a(ContentSpecMaker.ContentSpec));
        // with a level
        Level childLevel = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.APPENDIX)));
        contentSpec.getBaseLevel().appendChild(childLevel);
        // and two topics with different ids
        childLevel.appendChild(
                make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, id.toString()), with(SpecTopicMaker.revision, revision))));
        childLevel.appendChild(
                make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, id.toString()), with(SpecTopicMaker.revision, revision2))));
        childLevel.appendChild(
                make(a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.id, id.toString()), with(SpecTopicMaker.revision, revision2))));

        // When validating the topic for invalid duplicates
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then the result should be false
        assertFalse(result);
        // and an error should have been printed
        assertThat(logger.getLogMessages().toString(), containsString(
                "Invalid Content Specification! Topic " + id + " has two or more different revisions included in the Content " +
                        "Specification. The topic is located at:\n       -> Revision "));
    }
}
