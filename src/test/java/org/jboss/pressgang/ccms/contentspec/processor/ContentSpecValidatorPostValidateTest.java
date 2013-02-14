package org.jboss.pressgang.ccms.contentspec.processor;

import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.test.makers.ContentSpecMaker;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
public class ContentSpecValidatorPostValidateTest extends ContentSpecValidatorTest {

    @ArbitraryString(type = StringType.ALPHANUMERIC) String username;
    @Mock UserWrapper user;

    @Before
    public void setUp() {
        when(user.getUsername()).thenReturn(username);
        super.setUp();
    }

    @Test
    public void shouldPostValidateValidContentSpec() {
        // Given a valid content spec
        ContentSpec contentSpec = make(a(ContentSpecMaker.ContentSpec));

        // When the spec is postvalidated
        boolean result = validator.postValidateContentSpec(contentSpec, user);

        // Then the result should be a success
        assertThat(result, is(true));
        // And no error messages should be output
        assertThat(logger.getLogMessages().toString(), containsString("[]"));
    }
}
