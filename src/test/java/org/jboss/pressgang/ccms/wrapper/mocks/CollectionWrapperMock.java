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

package org.jboss.pressgang.ccms.wrapper.mocks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.pressgang.ccms.wrapper.base.EntityWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.CollectionWrapper;

public class CollectionWrapperMock<T extends EntityWrapper<T>> implements CollectionWrapper<T> {
    private static final Integer UNCHANGED_STATE = 0;
    private static final Integer ADD_STATE = 1;
    private static final Integer REMOVE_STATE = 2;

    private final Map<T, Integer> items = new LinkedHashMap<T, Integer>();

    protected Map<T, Integer> getCollection() {
        return items;
    }

    @Override
    public void addItem(T entity) {
        getCollection().put(entity, UNCHANGED_STATE);
    }

    @Override
    public void addNewItem(T entity) {
        getCollection().put(entity, ADD_STATE);
    }

    @Override
    public void addRemoveItem(T entity) {
        getCollection().put(entity, REMOVE_STATE);
    }

    @Override
    public void remove(T entity) {
        getCollection().remove(entity);
    }

    @Override
    public List<T> getItems() {
        return new ArrayList<T>(getCollection().keySet());
    }

    @Override
    public List<T> getUnchangedItems() {
        final List<T> unchangedItems = new ArrayList<T>();
        for (final Map.Entry<T, Integer> entry : getCollection().entrySet()) {
            if (entry.getValue().equals(UNCHANGED_STATE)) {
                unchangedItems.add(entry.getKey());
            }
        }
        return unchangedItems;
    }

    @Override
    public List<T> getAddItems() {
        final List<T> addItems = new ArrayList<T>();
        for (final Map.Entry<T, Integer> entry : getCollection().entrySet()) {
            if (entry.getValue().equals(ADD_STATE)) {
                addItems.add(entry.getKey());
            }
        }
        return addItems;
    }

    @Override
    public List<T> getRemoveItems() {
        final List<T> removeItems = new ArrayList<T>();
        for (final Map.Entry<T, Integer> entry : getCollection().entrySet()) {
            if (entry.getValue().equals(REMOVE_STATE)) {
                removeItems.add(entry.getKey());
            }
        }
        return removeItems;
    }

    @Override
    public Object unwrap() {
        // TODO
        return null;
    }

    @Override
    public int size() {
        return getCollection().size();
    }

    @Override
    public boolean isEmpty() {
        return getCollection().isEmpty();
    }
}
