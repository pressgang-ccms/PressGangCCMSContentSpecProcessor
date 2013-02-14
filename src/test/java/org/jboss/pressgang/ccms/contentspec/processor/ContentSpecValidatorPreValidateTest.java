package org.jboss.pressgang.ccms.contentspec.processor;

import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.enums.BookType;
import org.jboss.pressgang.ccms.contentspec.test.makers.ContentSpecMaker;
import org.junit.Test;

import static com.natpryce.makeiteasy.MakeItEasy.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.jboss.pressgang.ccms.contentspec.test.makers.ContentSpecMaker.*;
import static org.junit.Assert.assertThat;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
public class ContentSpecValidatorPreValidateTest extends ContentSpecValidatorTest {
    @Test
    public void shouldPreValidateValidContentSpec() {
        // Given a valid content spec
        ContentSpec contentSpec = make(a(ContentSpecMaker.ContentSpec));

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
        ContentSpec contentSpec = make(a(ContentSpecMaker.ContentSpec, with(bookType, BookType.INVALID)));

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
        ContentSpec contentSpec = make(a(ContentSpecMaker.ContentSpec, with(version, "AAA")));

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
        ContentSpec contentSpec = make(a(ContentSpecMaker.ContentSpec));
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
        ContentSpec contentSpec = make(a(ContentSpecMaker.ContentSpec, with(version, "")));

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
        ContentSpec contentSpec = make(a(ContentSpecMaker.ContentSpec, with(edition, "AAA")));

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
        ContentSpec contentSpec = make(a(ContentSpecMaker.ContentSpec, with(bookVersion, "AAA")));

        // When the spec is prevalidated
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Book Version specified. The value must be a valid version."));
    }
}
