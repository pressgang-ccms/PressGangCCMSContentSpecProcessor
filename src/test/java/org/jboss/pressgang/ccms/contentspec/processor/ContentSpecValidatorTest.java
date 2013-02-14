package org.jboss.pressgang.ccms.contentspec.processor;

import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.processor.structures.ProcessingOptions;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLogger;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.jboss.pressgang.ccms.contentspec.processor.TestUtil.makeAValidContentSpec;
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
        ContentSpec contentSpec = makeAValidContentSpec();

        // When
        boolean result = validator.preValidateContentSpec(contentSpec);

        // Then
        assertThat(result, is(true));
        assertThat(logger.getLogMessages().toString(), containsString("[]"));
    }

}
