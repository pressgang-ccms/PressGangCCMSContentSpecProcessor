package org.jboss.pressgang.ccms.contentspec.processor;

import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.enums.BookType;
import org.jboss.pressgang.ccms.contentspec.processor.structures.ProcessingOptions;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLogger;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.natpryce.makeiteasy.MakeItEasy.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.jboss.pressgang.ccms.contentspec.test.makers.ContentSpecMaker.ContentSpec;
import static org.jboss.pressgang.ccms.contentspec.test.makers.ContentSpecMaker.bookType;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class ContentSpecValidatorTest extends BaseUnitTest {

    @Mock DataProviderFactory dataProviderFactory;
    @Mock ErrorLoggerManager loggerManager;
    @Mock ProcessingOptions processingOptions;

    private ErrorLogger logger;
    private ContentSpecValidator validator;

    @Before
    public void setUp() {
        this.logger = new ErrorLogger("testLogger");
        when(loggerManager.getLogger(ContentSpecValidator.class)).thenReturn(logger);
        this.validator = new ContentSpecValidator(dataProviderFactory, loggerManager, processingOptions);
    }

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
        // Given an otherwise valid content spec with no book type
        ContentSpec contentSpec = make(a(ContentSpec, with(bookType, BookType.INVALID)));

        // When the spec is prevalidated
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Content Specification! The specified book type is not valid."));
    }

}
