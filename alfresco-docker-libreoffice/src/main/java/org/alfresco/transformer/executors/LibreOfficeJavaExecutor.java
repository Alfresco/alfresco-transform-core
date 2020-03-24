/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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
package org.alfresco.transformer.executors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.alfresco.transform.exceptions.TransformException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.OfficeException;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sun.star.task.ErrorCodeIOException;

/**
 * JavaExecutor implementation for running LibreOffice transformations. It loads the
 * transformation logic in the same JVM (check the {@link JodConverter} implementation).
 */
@Component
public class LibreOfficeJavaExecutor implements JavaExecutor
{
    private static final Logger logger = LoggerFactory.getLogger(LibreOfficeJavaExecutor.class);

    private static final int JODCONVERTER_TRANSFORMATION_ERROR_CODE = 3088;
    private static final String OFFICE_HOME = "/opt/libreoffice6.1";

    private JodConverter jodconverter;

    @PostConstruct
    public void init()
    {
        jodconverter = createJodConverter();
    }

    private static JodConverter createJodConverter()
    {
        final String timeout = "120000000";

        final JodConverterSharedInstance jodconverter = new JodConverterSharedInstance();

        jodconverter.setOfficeHome(OFFICE_HOME);         // jodconverter.officeHome
        jodconverter.setMaxTasksPerProcess("200");       // jodconverter.maxTasksPerProcess
        jodconverter.setTaskExecutionTimeout(timeout);   // jodconverter.maxTaskExecutionTimeout
        jodconverter.setTaskQueueTimeout(timeout);       // jodconverter.taskQueueTimeout
        jodconverter.setConnectTimeout(timeout);         // jodconverter.connectTimeout
        jodconverter.setPortNumbers("8100");             // jodconverter.portNumbers
        jodconverter.setTemplateProfileDir("");          // jodconverter.templateProfileDir
        jodconverter.setEnabled("true");                 // jodconverter.enabled
        jodconverter.afterPropertiesSet();

        return jodconverter;
    }

    @Override
    public void call(File sourceFile, File targetFile, String... args)
    {
        try
        {
            convert(sourceFile, targetFile);
        }
        catch (OfficeException e)
        {
            throw new TransformException(BAD_REQUEST.value(),
                "LibreOffice server conversion failed: \n" +
                "   from file: " + sourceFile + "\n" +
                "   to file: " + targetFile, e);
        }
        catch (Throwable throwable)
        {
            // Because of the known bug with empty Spreadsheets in JodConverter try to catch exception and produce empty pdf file
            if (throwable.getCause() instanceof ErrorCodeIOException &&
                ((ErrorCodeIOException) throwable.getCause()).ErrCode == JODCONVERTER_TRANSFORMATION_ERROR_CODE)
            {
                logger.warn("Transformation failed: \n" +
                            "from file: " + sourceFile + "\n" +
                            "to file: " + targetFile +
                            "Source file " + sourceFile + " has no content");
                produceEmptyPdfFile(targetFile);
            }
            else
            {
                throw throwable;
            }
        }

        if (!targetFile.exists() || targetFile.length() == 0L)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(),
                "Transformer failed to create an output file");
        }
    }

    public void convert(File sourceFile, File targetFile)
    {
        OfficeManager officeManager = jodconverter.getOfficeManager();
        OfficeDocumentConverter converter = new OfficeDocumentConverter(officeManager);
        converter.convert(sourceFile, targetFile);
    }

    /**
     * This method produces an empty PDF file at the specified File location.
     * Apache's PDFBox is used to create the PDF file.
     */
    private static void produceEmptyPdfFile(File targetFile)
    {
        // If improvement PDFBOX-914 is incorporated, we can do this with a straight call to
        // org.apache.pdfbox.TextToPdf.createPDFFromText(new StringReader(""));
        // https://issues.apache.org/jira/browse/PDFBOX-914

        PDPage pdfPage = new PDPage();
        try (PDDocument pdfDoc = new PDDocument();
             PDPageContentStream contentStream = new PDPageContentStream(pdfDoc, pdfPage))
        {
            // Even though, we want an empty PDF, some libs (e.g. PDFRenderer) object to PDFs
            // that have literally nothing in them. So we'll put a content stream in it.
            pdfDoc.addPage(pdfPage);

            // Now write the in-memory PDF document into the temporary file.
            pdfDoc.save(targetFile.getAbsolutePath());
        }
        catch (IOException iox)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(),
                "Error creating empty PDF file", iox);
        }
    }
}
