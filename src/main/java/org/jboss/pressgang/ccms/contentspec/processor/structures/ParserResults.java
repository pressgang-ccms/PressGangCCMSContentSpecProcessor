/*
  Copyright 2011-2014 Red Hat

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

package org.jboss.pressgang.ccms.contentspec.processor.structures;

import org.jboss.pressgang.ccms.contentspec.ContentSpec;

public class ParserResults {
    final boolean parsedSuccessfully;
    final ContentSpec contentSpec;

    public ParserResults(final boolean parsedSuccessfully, final ContentSpec contentSpec) {
        this.parsedSuccessfully = parsedSuccessfully;
        this.contentSpec = contentSpec;
    }

    public boolean parsedSuccessfully() {
        return parsedSuccessfully;
    }

    public ContentSpec getContentSpec() {
        return contentSpec;
    }
}
