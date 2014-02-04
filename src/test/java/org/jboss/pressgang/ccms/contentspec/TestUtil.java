package org.jboss.pressgang.ccms.contentspec;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.math.RandomUtils.nextInt;

import java.util.ArrayList;
import java.util.List;

import org.jboss.pressgang.ccms.contentspec.enums.LevelType;

/**
 * Shared utilities methods to assist with testing.
 *
 * @author kamiller@redhat.com (Katie Miller)
 */
public class TestUtil {

    public static <T> T selectRandomListItem(List<T> list) {
        return list.get(nextInt(list.size()));
    }

    public static LevelType selectRandomLevelType() {
        ArrayList<LevelType> levelTypes = new ArrayList<LevelType>(asList(LevelType.values()));
        levelTypes.remove(LevelType.BASE);
        return selectRandomListItem(levelTypes);
    }
}
