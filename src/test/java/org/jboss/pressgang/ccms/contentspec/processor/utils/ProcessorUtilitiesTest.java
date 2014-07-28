/*
  Copyright 2011-2014 Red Hat

  This file is part of PresGang CCMS.

  PresGang CCMS is free software: you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  PresGang CCMS is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with PresGang CCMS.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.jboss.pressgang.ccms.contentspec.processor.utils;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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
    public void shouldThrowExceptionForKeyOnly() {
        // Given a key value pair string with no value
        final String metadata = key;

        // When
        try {
            ProcessorUtilities.getAndValidateKeyValuePair(metadata);

            // Then an exception should have been thrown
            fail("An exception should have been thrown");
        } catch (InvalidKeyValueException e) {

        }
    }

    @Test
    public void shouldParseXMLCharReference() {
        // Given a string with an xml character reference
        String in = "This is an ampersand: &#38; &#x0026; &amp;";

        // When cleaning xml character references
        String out = ProcessorUtilities.cleanXMLCharacterReferences(in);

        // Then the output should have the character references resolved
        assertThat(out, is("This is an ampersand: & & &amp;"));
    }
}
