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
