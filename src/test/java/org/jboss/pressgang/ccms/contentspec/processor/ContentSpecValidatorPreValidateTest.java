package org.jboss.pressgang.ccms.contentspec.processor;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.enums.BookType;
import org.jboss.pressgang.ccms.contentspec.provider.TagProvider;
import org.jboss.pressgang.ccms.contentspec.test.makers.ContentSpecMaker;
import org.jboss.pressgang.ccms.contentspec.test.makers.SpecTopicMaker;
import org.jboss.pressgang.ccms.contentspec.wrapper.TagWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.collection.CollectionWrapper;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static com.natpryce.makeiteasy.MakeItEasy.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.jboss.pressgang.ccms.contentspec.test.makers.ContentSpecMaker.*;
import static org.jboss.pressgang.ccms.contentspec.test.makers.SpecTopicMaker.SpecTopic;
import static org.jboss.pressgang.ccms.contentspec.test.makers.SpecTopicMaker.id;
import static org.jboss.pressgang.ccms.contentspec.test.makers.SpecTopicMaker.revision;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
public class ContentSpecValidatorPreValidateTest extends ContentSpecValidatorTest {
    @Arbitrary Integer randomInt;

    @Test
    public void shouldPreValidateValidContentSpec() {
        // Given a valid content spec
        ContentSpec contentSpec = make(a(ContentSpec));

        // When the spec is prevalidated
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then the result should be a success
        assertThat(result, is(true));
        // And no error messages should be output
        assertThat(logger.getLogMessages().toString(), containsString("[]"));
    }

    @Test
    public void shouldFailAndLogErrorWhenInvalidBookType() {
        // Given an otherwise valid content spec with an invalid book type
        ContentSpec contentSpec = make(a(ContentSpec, with(bookType, BookType.INVALID)));

        // When the spec is prevalidated
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Content Specification! The specified book type is not valid."));
    }

    @Test
    public void shouldFailAndLogErrorWhenInvalidVersion() {
        // Given an otherwise valid content spec with an invalid version
        ContentSpec contentSpec = make(a(ContentSpec, with(version, "AAA")));

        // When the spec is prevalidated
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Version specified. The value must be a valid version."));
    }

    @Test
    public void shouldFailAndLogErrorWhenNoVersion() {
        // Given an otherwise valid content spec with no version set
        ContentSpec contentSpec = make(a(ContentSpec));
        contentSpec.setVersion(null);

        // When the spec is prevalidated
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Content Specification! No Version specified."));
    }

    @Test
    public void shouldFailAndLogErrorWhenEmptyVersion() {
        // Given an otherwise valid content spec with an empty version set
        ContentSpec contentSpec = make(a(ContentSpec, with(version, "")));

        // When the spec is prevalidated
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Content Specification! No Version specified."));
    }

    @Test
    public void shouldFailAndLogErrorWhenInvalidEdition() {
        // Given an otherwise valid content spec with an invalid edition
        ContentSpec contentSpec = make(a(ContentSpec, with(edition, "AAA")));

        // When the spec is prevalidated
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Edition specified. The value must be a valid version."));
    }

    @Test
    public void shouldFailAndLogErrorWhenInvalidBookVersion() {
        // Given an otherwise valid content spec with an invalid book version
        ContentSpec contentSpec = make(a(ContentSpec, with(bookVersion, "AAA")));

        // When the spec is prevalidated
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Book Version specified. The value must be a valid version."));
    }

    @Test
    public void shouldFailAndLogErrorWhenEmptyTitle() {
        // Given an otherwise valid content spec with an empty title
        ContentSpec contentSpec = make(a(ContentSpec, with(title, "")));

        // When the spec is prevalidated
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Content Specification! No Title."));
    }

    @Test
    public void shouldFailAndLogErrorWhenEmptyProduct() {
        // Given an otherwise valid content spec with an empty product
        ContentSpec contentSpec = make(a(ContentSpec, with(product, "")));

        // When the spec is prevalidated
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Content Specification! No Product specified."));
    }

    @Test
    public void shouldFailAndLogErrorWhenNoPreProcessedText() {
        // Given an otherwise valid content spec with no preprocessed text
        ContentSpec contentSpec = make(a(ContentSpec));
        contentSpec.getPreProcessedText().clear();

        // When the spec is prevalidated
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("An error occurred during processing please try again."));
    }

    @Test
    public void shouldFailAndLogErrorWhenEmptyDtd() {
        // Given an otherwise valid content spec with an empty DTD
        ContentSpec contentSpec = make(a(ContentSpec, with(dtd, "")));

        // When the spec is prevalidated
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Content Specification! No DTD specified."));
    }

    @Test
    public void shouldFailAndLogErrorWhenUnexpectedDtd() {
        // Given an otherwise valid content spec with an unexpected DTD
        ContentSpec contentSpec = make(a(ContentSpec, with(dtd, "Docbook 3")));

        // When the spec is prevalidated
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Content Specification! DTD specified is unsupported. Docbook 4.5 is the only currently supported DTD."));
    }

    @Test
    public void shouldFailAndLogErrorWhenEmptyCopyrightHolder() {
        // Given an otherwise valid content spec with an empty copyright holder
        ContentSpec contentSpec = make(a(ContentSpec, with(copyrightHolder, "")));

        // When the spec is prevalidated
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Content Specification! A Copyright Holder must be specified."));
    }

    @Test
    public void shouldFailAndLogErrorWhenLevelsInvalid() {
        // Given an otherwise valid content spec with invalid levels
        ContentSpec contentSpec = make(a(ContentSpec));
        contentSpec.getBaseLevel().getSpecTopics().clear();

        // When the spec is prevalidated
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("No topics or levels"));
    }

    @Test
    public void shouldFailAndLogErrorWhenTopicsHaveSameIdAndDifferentRevisions() {
        // Given an otherwise valid content spec with two spec topics that are existing topics with the same id
        // but different revisions
        ContentSpec contentSpec = make(a(ContentSpec));
        given(processingOptions.isUpdateRevisions()).willReturn(false);
        SpecTopic specTopic = make(a(SpecTopicMaker.SpecTopic, with(id, randomInt.toString()), with(revision, randomInt)));
        SpecTopic specTopic2 = make(a(SpecTopicMaker.SpecTopic, with(id, randomInt.toString()), with(revision, randomInt+1)));
        contentSpec.getBaseLevel().getSpecTopics().clear();
        contentSpec.getBaseLevel().getSpecTopics().addAll(Arrays.asList(specTopic, specTopic2));

        // When the spec is prevalidated
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Content Specification! Topic " + randomInt
                + " has two or more different revisions included in the Content Specification."));
    }
}
