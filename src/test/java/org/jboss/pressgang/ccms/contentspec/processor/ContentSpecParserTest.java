package org.jboss.pressgang.ccms.contentspec.processor;

import static org.mockito.Mockito.when;

import org.jboss.pressgang.ccms.contentspec.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLogger;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.provider.DataProviderFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.mockito.Mock;
import org.powermock.modules.junit4.rule.PowerMockRule;

@Ignore
public class ContentSpecParserTest extends BaseUnitTest {
    @Rule public PowerMockRule rule = new PowerMockRule();

    @Mock DataProviderFactory dataProviderFactory;
    @Mock ErrorLoggerManager loggerManager;

    protected final String MISSING_PARSING_EXCEPTION = "ParsingException not thrown";
    protected ErrorLogger logger;
    protected ContentSpecParser parser;
    protected ContentSpecParser.ParserData parserData;

    @Before
    public void setUp() throws Exception {
        logger = new ErrorLogger("testLogger");
        when(loggerManager.getLogger(ContentSpecParser.class)).thenReturn(logger);
        parser = new ContentSpecParser(dataProviderFactory, loggerManager);
        parserData = new ContentSpecParser.ParserData();
    }
}
