/*
  Copyright 2011-2014 Red Hat, Inc

  This file is part of PressGang CCMS.

  PressGang CCMS is free software: you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  PressGang CCMS is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with PressGang CCMS.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.jboss.pressgang.ccms.contentspec.test.makers.validator;

import static com.natpryce.makeiteasy.Property.newProperty;
import static java.lang.String.valueOf;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextInt;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.enums.BookType;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
public class ContentSpecMaker {
    public static final Property<ContentSpec, String> title = newProperty();
    public static final Property<ContentSpec, String> product = newProperty();
    public static final Property<ContentSpec, String> version = newProperty();
    public static final Property<ContentSpec, String> subtitle = newProperty();
    public static final Property<ContentSpec, String> description = newProperty();
    public static final Property<ContentSpec, String> copyrightHolder = newProperty();
    public static final Property<ContentSpec, String> copyrightYear = newProperty();
    public static final Property<ContentSpec, String> dtd = newProperty();
    public static final Property<ContentSpec, BookType> bookType = newProperty();
    public static final Property<ContentSpec, String> bookVersion = newProperty();
    public static final Property<ContentSpec, String> pomVersion = newProperty();
    public static final Property<ContentSpec, String> edition = newProperty();
    public static final Property<ContentSpec, Integer> id = newProperty();
    public static final Property<ContentSpec, String> checksum = newProperty();

    public static final Instantiator<ContentSpec> ContentSpec = new Instantiator<org.jboss.pressgang.ccms.contentspec.ContentSpec>() {
        @Override
        public ContentSpec instantiate(PropertyLookup<ContentSpec> lookup) {
            ContentSpec contentSpec = new ContentSpec(lookup.valueOf(title, randomAlphanumeric(10)));
            contentSpec.setProduct(lookup.valueOf(product, randomAlphanumeric(10)));
            contentSpec.setVersion(lookup.valueOf(version, "1-A"));
            contentSpec.setSubtitle(lookup.valueOf(subtitle, (String) null));
            contentSpec.setAbstract(lookup.valueOf(description, (String) null));
            contentSpec.setCopyrightHolder(lookup.valueOf(copyrightHolder, randomAlphanumeric(10)));
            contentSpec.setCopyrightYear(lookup.valueOf(copyrightYear, (String) null));
            contentSpec.setFormat(lookup.valueOf(dtd, "Docbook 4.5"));
            contentSpec.setBookType(lookup.valueOf(bookType, BookType.BOOK));
            contentSpec.setBookVersion(lookup.valueOf(bookVersion, valueOf(nextInt())));
            contentSpec.setPOMVersion(lookup.valueOf(pomVersion, valueOf(nextInt())));
            contentSpec.setEdition(lookup.valueOf(edition, valueOf(nextInt())));
            contentSpec.setId(lookup.valueOf(id, nextInt()));
            contentSpec.setChecksum(lookup.valueOf(checksum, randomAlphanumeric(10)));
            return contentSpec;
        }
    };
}
