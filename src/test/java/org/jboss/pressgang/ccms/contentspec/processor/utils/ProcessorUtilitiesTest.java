package org.jboss.pressgang.ccms.contentspec.processor.utils;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.processor.exceptions.InvalidKeyValueException;
import org.jboss.pressgang.ccms.utils.structures.Pair;
import org.junit.Test;

public class ProcessorUtilitiesTest extends BaseUnitTest {

    @ArbitraryString(type = StringType.ALPHANUMERIC) String key;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String value;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String randomString;

    @Test
    public void shouldGetKeyValuePairWithMultipleEquals() {
        // Given a key value pair string with multiple equals
        final String metadata = key + " = " + value + "=" + randomString;

        // When
        Pair<String, String> keyValue = null;
        try {
            keyValue = ProcessorUtilities.getAndValidateKeyValuePair(metadata);
        } catch (InvalidKeyValueException e) {
            fail("An exception should not have been thrown");
        }

        // Then check the key and value parsed is correct
        assertNotNull(keyValue);
        assertEquals(keyValue.getFirst(), key);
        assertEquals(keyValue.getSecond(), value + "=" + randomString);
    }

    @Test
    public void shouldThrowExceptionForKeyWithNoValue() {
        // Given a key value pair string with no value
        final String metadata = key + " = ";

        // When
        try {
            ProcessorUtilities.getAndValidateKeyValuePair(metadata);

            // Then an exception should have been thrown
            fail("An exception should have been thrown");
        } catch (InvalidKeyValueException e) {

        }
    }
}
