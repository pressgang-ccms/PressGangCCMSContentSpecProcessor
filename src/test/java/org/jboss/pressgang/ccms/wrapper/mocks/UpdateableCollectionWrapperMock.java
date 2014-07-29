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

package org.jboss.pressgang.ccms.wrapper.mocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.pressgang.ccms.wrapper.base.EntityWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.UpdateableCollectionWrapper;

public class UpdateableCollectionWrapperMock<T extends EntityWrapper<T>> extends CollectionWrapperMock<T> implements
        UpdateableCollectionWrapper<T> {
    private static final Integer UPDATE_STATE = 3;

    @Override
    public void addUpdateItem(T entity) {
        getCollection().put(entity, UPDATE_STATE);
    }

    @Override
    public List<T> getUpdateItems() {
        final List<T> updateItems = new ArrayList<T>();
        for (final Map.Entry<T, Integer> entry : getCollection().entrySet()) {
            if (entry.getValue().equals(UPDATE_STATE)) {
                updateItems.add(entry.getKey());
            }
        }
        return updateItems;
    }
}
