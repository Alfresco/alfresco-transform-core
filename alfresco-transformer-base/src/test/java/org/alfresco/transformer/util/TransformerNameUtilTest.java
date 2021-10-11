/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * -
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 * -
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * -
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * -
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transformer.util;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.transform.client.registry.TransformServiceRegistry;
import org.alfresco.transform.exceptions.TransformException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.alfresco.transformer.util.RequestParamMap.SOURCE_ENCODING;
import static org.alfresco.transformer.util.TransformerNameUtil.getTransformerName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;


class TransformerNameUtilTest
{
    private static final long FILESIZE = 100L;
    private static final String SOURCE_MIMETYPE = "SOURCE_MIME_TYPE";
    private static final String TARGET_MIMETYPE = "TARGET_MIME_TYPE";
    private static final String REQUEST_TRANSFORM_NAME = "REQUEST_TRANSFORM_NAME";
    private static final String REGISTRY_TRANSFORM_NAME = "REGISTRY_TRANSFORM_NAME";
    private static final String ENCODING = "ENCODING";
    private static final String TRANSFORM_OPTION = "TRANSFORM_OPTION";
    private Map<String, String> transformOptions = new HashMap<>();
    @Mock
    private TransformServiceRegistry registryMock;
    @Captor
    private ArgumentCaptor<Map<String, String>> captorMap;

    @BeforeEach
    public void setUp()
    {
        MockitoAnnotations.openMocks(this);
        transformOptions.put(SOURCE_ENCODING, ENCODING);
        transformOptions.put(TRANSFORM_OPTION, TRANSFORM_OPTION);
        when(registryMock.findTransformerName(eq(SOURCE_MIMETYPE), eq(FILESIZE), eq(TARGET_MIMETYPE), anyMap(), isNull())).thenReturn(REGISTRY_TRANSFORM_NAME);
    }

    @Test
    public void returnsProvidedTransformName()
    {
        assertThat(getTransformerName(registryMock, SOURCE_MIMETYPE, FILESIZE, TARGET_MIMETYPE, REQUEST_TRANSFORM_NAME, transformOptions),
                   is(REQUEST_TRANSFORM_NAME));
    }

    @Test
    public void usesRegistryOnNullTransformNameParameter()
    {
        assertThat(getTransformerName(registryMock, SOURCE_MIMETYPE, FILESIZE, TARGET_MIMETYPE, null, transformOptions),
                   is(REGISTRY_TRANSFORM_NAME));
    }

    @Test
    public void usesRegistryOnBlankTransformNameParameter()
    {
        assertThat(getTransformerName(registryMock, SOURCE_MIMETYPE, FILESIZE, TARGET_MIMETYPE, "", transformOptions),
                   is(REGISTRY_TRANSFORM_NAME));
    }

    @Test
    public void throwsExceptionWhenCantFindTransformer()
    {
        when(registryMock.findTransformerName(eq(SOURCE_MIMETYPE), eq(FILESIZE), eq(TARGET_MIMETYPE), anyMap(), isNull())).thenReturn(null);
        TransformException transformException = assertThrows(TransformException.class,
                                                             () -> getTransformerName(registryMock, SOURCE_MIMETYPE, FILESIZE, TARGET_MIMETYPE, null, transformOptions));
        assertThat(transformException.getStatusCode(), is(BAD_REQUEST.value()));
    }

    @Test
    public void passesOptionsWithoutEncoding()
    {
        getTransformerName(registryMock, SOURCE_MIMETYPE, FILESIZE, TARGET_MIMETYPE, null, transformOptions);
        verify(registryMock).findTransformerName(eq(SOURCE_MIMETYPE), eq(FILESIZE), eq(TARGET_MIMETYPE), captorMap.capture(), isNull());
        assertThat(captorMap.getValue().size(), is(1));
        assertThat(captorMap.getValue().get(SOURCE_ENCODING), is(nullValue()));
    }

}