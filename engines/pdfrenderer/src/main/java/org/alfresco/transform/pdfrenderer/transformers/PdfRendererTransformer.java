/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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
package org.alfresco.transform.pdfrenderer.transformers;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.executors.AbstractCommandExecutor;
import org.alfresco.transform.base.executors.RuntimeExec;
import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;
import org.alfresco.transform.common.TransformException;
import org.alfresco.transform.pdfrenderer.PdfRendererOptionsBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.alfresco.transform.base.util.Util.stringToLong;
import static org.alfresco.transform.common.RequestParamMap.ALLOW_PDF_ENLARGEMENT;
import static org.alfresco.transform.common.RequestParamMap.HEIGHT_REQUEST_PARAM;
import static org.alfresco.transform.common.RequestParamMap.MAINTAIN_PDF_ASPECT_RATIO;
import static org.alfresco.transform.common.RequestParamMap.PAGE_REQUEST_PARAM;
import static org.alfresco.transform.common.RequestParamMap.TIMEOUT;
import static org.alfresco.transform.common.RequestParamMap.WIDTH_REQUEST_PARAM;

/**
 * CommandExecutor implementation for running PDF Renderer transformations. It runs the
 * transformation logic as a separate Shell process.
 */
@Component
public class PdfRendererTransformer extends AbstractCommandExecutor implements CustomTransformerFileAdaptor
{
    @Value("${transform.core.pdfrenderer.exe}")
    private String exe;

    @PostConstruct
    private void createCommands()
    {
        if (exe == null || exe.isEmpty())
        {
            throw new IllegalArgumentException("PdfRendererTransformer PDFRENDERER_EXE variable cannot be null or empty");
        }
        super.transformCommand = createTransformCommand();
        super.checkCommand = createCheckCommand();
    }

    @Override public String getTransformerName()
    {
        return "pdfrenderer";
    }

    @Override
    protected RuntimeExec createTransformCommand()
    {
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();
        commandsAndArguments.put(".*", new String[]{exe, "SPLIT:${options}", "${source}", "${target}"});
        runtimeExec.setCommandsAndArguments(commandsAndArguments);

        Map<String, String> defaultProperties = new HashMap<>();
        defaultProperties.put("key", null);
        runtimeExec.setDefaultProperties(defaultProperties);

        runtimeExec.setErrorCodes("1");

        return runtimeExec;
    }

    @Override
    protected RuntimeExec createCheckCommand()
    {
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();
        commandsAndArguments.put(".*", new String[]{exe, "--version"});
        runtimeExec.setCommandsAndArguments(commandsAndArguments);
        return runtimeExec;
    }

    @Override
    public void transform(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
                          File sourceFile, File targetFile) throws TransformException
    {
        final String options = PdfRendererOptionsBuilder
                .builder()
                .withPage(transformOptions.get(PAGE_REQUEST_PARAM))
                .withWidth(transformOptions.get(WIDTH_REQUEST_PARAM))
                .withHeight(transformOptions.get(HEIGHT_REQUEST_PARAM))
                .withAllowPdfEnlargement(transformOptions.get(ALLOW_PDF_ENLARGEMENT))
                .withMaintainPdfAspectRatio(transformOptions.get(MAINTAIN_PDF_ASPECT_RATIO))
                .build();

        Long timeout = stringToLong(transformOptions.get(TIMEOUT));

        run(options, sourceFile, targetFile, timeout);
    }
}
