package org.jboss.pressgang.ccms.contentspec.processor;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.CommonContent;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.enums.BookType;
import org.jboss.pressgang.ccms.contentspec.enums.LevelType;
import org.jboss.pressgang.ccms.contentspec.test.makers.shared.LevelMaker;
import org.junit.Test;

public class ContentSpecValidatorPreValidateCommonContentTest extends ContentSpecValidatorTest {
    @Arbitrary Integer lineNumber;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String randomString;

    @Test
    public void shouldSucceedWhenCommonContentUsedWithoutFilenameExtension() {
        // Given a common content topic
        final CommonContent commonContent = new CommonContent("Legal Notice", lineNumber, "Legal Notice [Common Content]");
        // and a level
        Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.APPENDIX)));
        level.appendChild(commonContent);

        // When prevalidating the topic
        boolean result = validator.preValidateCommonContent(commonContent, BookType.BOOK);

        // Then the result should be true
        assertTrue(result);
        // and there should be no warning in the logs
        assertThat(logger.getLogMessages().toString(), not(containsString(
                "The Common Content specified is not a recognised file. Please check to ensure that the specified title is correct, " +
                        "as this may lead to build errors.")));
    }

    @Test
    public void shouldLogWarningWhenUnknownCommonContentUsed() {
        // Given a common content topic
        final CommonContent commonContent = new CommonContent(randomString, lineNumber, randomString + " [Common Content]");
        // and a level
        Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.APPENDIX)));
        level.appendChild(commonContent);

        // When prevalidating the topic
        boolean result = validator.preValidateCommonContent(commonContent, BookType.BOOK);

        // Then the result should be true
        assertTrue(result);
        // and there should be a warning in the logs
        assertThat(logger.getLogMessages().toString(), containsString(
                "The Common Content specified is not a recognised file. Please check to ensure that the specified title is correct, " +
                        "as this may lead to build errors."));
    }
}
