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
