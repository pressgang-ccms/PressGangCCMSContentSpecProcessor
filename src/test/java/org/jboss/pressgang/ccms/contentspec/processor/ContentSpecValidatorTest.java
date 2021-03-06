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

package org.jboss.pressgang.ccms.contentspec.processor;

import static org.mockito.Mockito.when;

import org.jboss.pressgang.ccms.contentspec.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.processor.structures.ProcessingOptions;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLogger;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.provider.ServerSettingsProvider;
import org.jboss.pressgang.ccms.wrapper.LocaleWrapper;
import org.jboss.pressgang.ccms.wrapper.ServerEntitiesWrapper;
import org.jboss.pressgang.ccms.wrapper.ServerSettingsWrapper;
import org.jboss.pressgang.ccms.wrapper.mocks.UpdateableCollectionWrapperMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.mockito.Mock;
import org.powermock.modules.junit4.rule.PowerMockRule;

@Ignore
public class ContentSpecValidatorTest extends BaseUnitTest {
    protected static Integer ADDED_BY_PROPERTY_TAG_ID = 14;
    protected static Integer CSP_PROPERTY_ID = 15;
    protected static Integer READ_ONLY_PROPERTY_TAG_ID = 25;
    protected static Integer TYPE_CATEGORY_ID = 4;
    protected static Integer WRITER_CATEGORY_ID = 12;
    protected static Integer ROCBOOK_DTD_ID = 9;

    @Rule public PowerMockRule rule = new PowerMockRule();

    @Mock DataProviderFactory dataProviderFactory;
    @Mock ServerSettingsProvider serverSettingsProvider;
    @Mock ServerSettingsWrapper serverSettings;
    @Mock ServerEntitiesWrapper serverEntities;
    @Mock ErrorLoggerManager loggerManager;
    @Mock ProcessingOptions processingOptions;
    @Mock LocaleWrapper defaultLocaleWrapper;

    protected ErrorLogger logger;
    protected ContentSpecValidator validator;

    @Before
    public void setUp() {
        this.logger = new ErrorLogger("testLogger");
        when(loggerManager.getLogger(ContentSpecValidator.class)).thenReturn(logger);

        when(dataProviderFactory.getProvider(ServerSettingsProvider.class)).thenReturn(serverSettingsProvider);
        when(serverSettingsProvider.getServerSettings()).thenReturn(serverSettings);
        when(serverSettings.getEntities()).thenReturn(serverEntities);
        when(serverEntities.getCspIdPropertyTagId()).thenReturn(CSP_PROPERTY_ID);
        when(serverEntities.getAddedByPropertyTagId()).thenReturn(ADDED_BY_PROPERTY_TAG_ID);
        when(serverEntities.getReadOnlyPropertyTagId()).thenReturn(READ_ONLY_PROPERTY_TAG_ID);
        when(serverEntities.getTypeCategoryId()).thenReturn(TYPE_CATEGORY_ID);
        when(serverEntities.getWriterCategoryId()).thenReturn(WRITER_CATEGORY_ID);
        when(serverEntities.getRocBook45DTDBlobConstantId()).thenReturn(ROCBOOK_DTD_ID);
        when(serverSettings.getDefaultLocale()).thenReturn(defaultLocaleWrapper);

        final UpdateableCollectionWrapperMock<LocaleWrapper > locales = new UpdateableCollectionWrapperMock<LocaleWrapper>();
        locales.addItem(defaultLocaleWrapper);
        when(serverSettings.getLocales()).thenReturn(locales);
        when(defaultLocaleWrapper.getValue()).thenReturn("en-US");

        validator = new ContentSpecValidator(dataProviderFactory, loggerManager, processingOptions);
    }
}
