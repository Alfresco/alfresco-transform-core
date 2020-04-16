/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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
package org.alfresco.transformer.transformers;

import static org.alfresco.transformer.util.RequestParamMap.ALLOW_PDF_ENLARGEMENT;
import static org.alfresco.transformer.util.RequestParamMap.HEIGHT_REQUEST_PARAM;
import static org.alfresco.transformer.util.RequestParamMap.MAINTAIN_PDF_ASPECT_RATIO;
import static org.alfresco.transformer.util.RequestParamMap.PAGE_REQUEST_PARAM;
import static org.alfresco.transformer.util.RequestParamMap.TIMEOUT;
import static org.alfresco.transformer.util.RequestParamMap.WIDTH_REQUEST_PARAM;
import static org.alfresco.transformer.util.Util.stringToLong;

import java.io.File;
import java.util.Map;

import org.alfresco.transformer.executors.PdfRendererCommandExecutor;
import org.alfresco.transformer.PdfRendererOptionsBuilder;


public class PdfRendererAdapter implements Transformer
{
    private static String ID = "pdfrenderer";
    private PdfRendererCommandExecutor pdfExecutor;

    public PdfRendererAdapter(String execPath) throws Exception
    {
        super();
        pdfExecutor = new PdfRendererCommandExecutor(execPath);
    }

	@Override
	public void transform(File sourceFile, File targetFile, String sourceMimetype, String targetMimetype,
            Map<String, String> transformOptions)
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

        pdfExecutor.run(options, sourceFile, targetFile, timeout);
	}

    @Override
    public String getTransformerId()
    {
        return ID;
    }

}