package org.jboss.pressgang.ccms.contentspec.processor;

import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.provider.TagProvider;
import org.jboss.pressgang.ccms.contentspec.test.makers.LevelMaker;
import org.jboss.pressgang.ccms.contentspec.test.makers.SpecTopicMaker;
import org.jboss.pressgang.ccms.contentspec.wrapper.TagWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.collection.CollectionWrapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static com.natpryce.makeiteasy.MakeItEasy.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.jboss.pressgang.ccms.contentspec.test.makers.LevelMaker.tags;
import static org.jboss.pressgang.ccms.contentspec.test.makers.SpecTopicMaker.id;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
public class ContentSpecValidatorPostValidateLevelTest extends ContentSpecValidatorTest {
    @ArbitraryString(type = StringType.ALPHA) String tagname;
    @Mock TagProvider tagProvider;
    @Mock CollectionWrapper<TagWrapper> tagWrapperCollection;
    @Mock List<TagWrapper> tagWrapperList;
    @Mock TagWrapper tagWrapper;

    @Before
    public void setUp() {
        when(dataProviderFactory.getProvider(TagProvider.class)).thenReturn(tagProvider);
        super.setUp();
    }

    @Test
    public void shouldSucceedIfLevelValid() {
        // Given a valid level
        Level level = make(a(LevelMaker.Level));

        // When the level is postvalidated
        boolean result = validator.postValidateLevel(level);

        // Then the result should be success
        assertThat(result, is(true));
        // And no error messages should be output
        assertThat(logger.getLogMessages().toString(), containsString("[]"));
    }

    @Test
    public void shouldSucceedIfValidLevelAndHasValidChildLevel() {
        // Given a valid level with a valid child level
        Level level = make(a(LevelMaker.Level));
        level.appendChild(make(a(LevelMaker.Level)));

        // When the level is postvalidated
        boolean result = validator.postValidateLevel(level);

        // Then the result should be success
        assertThat(result, is(true));
        // And no error messages should be output
        assertThat(logger.getLogMessages().toString(), containsString("[]"));
    }

    @Test
    public void shouldSucceedIfValidLevelAndHasValidChildSpecTopic() {
        // Given a valid level with a valid child spec topic
        Level level = make(a(LevelMaker.Level));
        level.appendChild(make(a(SpecTopicMaker.SpecTopic)));

        // When the level is postvalidated
        boolean result = validator.postValidateLevel(level);

        // Then the result should be success
        assertThat(result, is(true));
        // And no error messages should be output
        assertThat(logger.getLogMessages().toString(), containsString("[]"));
    }

    @Test
    public void shouldLogErrorAndFailIfTopicTagInvalid() {
        // Given a level with an invalid topic tag
        Level level = createLevelWithInvalidTag();

        // When the level is postvalidated
        boolean result = validator.postValidateLevel(level);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Content Specification! Tag \""
                + tagname + "\" doesn't exist."));
    }

    @Test
    public void shouldLogErrorAndFailIfChildLevelInvalid() {
        // Given a valid level with an invalid child level
        Level level = make(a(LevelMaker.Level));
        level.appendChild(createLevelWithInvalidTag());

        // When the level is postvalidated
        boolean result = validator.postValidateLevel(level);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Content Specification! Tag \""
                + tagname + "\" doesn't exist."));
    }

    @Test
    public void shouldLogErrorAndFailIfChildSpecTopicInvalid() {
        // Given a valid level with an invalid child spec topic that is a new topic
        Level level = make(a(LevelMaker.Level));
        SpecTopic specTopic = make(a(SpecTopicMaker.SpecTopic, with(id, "N123")));
        given(tagProvider.getTagsByName(specTopic.getType())).willReturn(tagWrapperCollection);
        given(tagWrapperCollection.getItems()).willReturn(tagWrapperList);
        given(tagWrapperList.size()).willReturn(0);
        level.appendChild(specTopic);
        // And that the assigned writer is valid
        given(tagProvider.getTagsByName(specTopic.getAssignedWriter(false))).willReturn(tagWrapperCollection);

        // When the level is postvalidated
        boolean result = validator.postValidateLevel(level);

        // Then the result should be a failure
        assertThat(result, is(false));
        // And an error message should be output
        assertThat(logger.getLogMessages().toString(), containsString("Invalid Topic! Type doesn't exist."));
    }

    private Level createLevelWithInvalidTag() {
        Level level = make(a(LevelMaker.Level, with(tags, Arrays.asList(tagname))));
        given(tagProvider.getTagsByName(tagname)).willReturn(tagWrapperCollection);
        given(tagWrapperCollection.getItems()).willReturn(tagWrapperList);
        given(tagWrapperList.size()).willReturn(0);
        return level;
    }
}
