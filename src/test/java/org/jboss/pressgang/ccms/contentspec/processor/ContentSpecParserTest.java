package org.jboss.pressgang.ccms.contentspec.processor;

import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLogger;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.mockito.Mock;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.mockito.Mockito.when;

@Ignore
public class ContentSpecParserTest extends BaseUnitTest {
    @Rule public PowerMockRule rule = new PowerMockRule();

    @Mock DataProviderFactory dataProviderFactory;
    @Mock ErrorLoggerManager loggerManager;

    protected final String MISSING_PARSING_EXCEPTION = "ParsingException not thrown";
    protected ErrorLogger logger;
    protected ContentSpecParser parser;

    @Before
    public void setUp() throws Exception {
        this.logger = new ErrorLogger("testLogger");
        when(loggerManager.getLogger(ContentSpecParser.class)).thenReturn(logger);
        this.parser = new ContentSpecParser(dataProviderFactory, loggerManager);
    }
}
