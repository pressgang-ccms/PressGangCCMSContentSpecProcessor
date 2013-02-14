package org.jboss.pressgang.ccms.contentspec.processor;

import org.jboss.pressgang.ccms.contentspec.processor.structures.ProcessingOptions;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLogger;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.junit.Before;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

public class ContentSpecValidatorTest extends BaseUnitTest {

    @Mock DataProviderFactory dataProviderFactory;
    @Mock ErrorLoggerManager loggerManager;
    @Mock ProcessingOptions processingOptions;

    protected ErrorLogger logger;
    protected ContentSpecValidator validator;

    @Before
    public void setUp() {
        this.logger = new ErrorLogger("testLogger");
        when(loggerManager.getLogger(ContentSpecValidator.class)).thenReturn(logger);
        this.validator = new ContentSpecValidator(dataProviderFactory, loggerManager, processingOptions);
    }
}
