package org.jboss.pressgang.ccms.contentspec.processor;

import static org.mockito.Mockito.when;

import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLogger;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
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
    @Mock ErrorLogger logger;

    protected ContentSpecParser parser;

    @Before
    public void setUp() {
        when(loggerManager.getLogger(ContentSpecParser.class)).thenReturn(logger);
        this.parser = new ContentSpecParser(dataProviderFactory, loggerManager);
    }
}
