/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transformer;

import com.sun.star.task.ErrorCodeIOException;
import org.alfresco.transformer.base.AbstractTransformerController;
import org.alfresco.transformer.base.TransformException;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.OfficeException;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;

/**
 * Controller for the Docker based LibreOffice transformer.
 *
 *
 * Status Codes:
 *
 *   200 Success
 *   400 Bad Request: Request parameter <name> is missing (missing mandatory parameter)
 *   400 Bad Request: Request parameter <name> is of the wrong type
 *   400 Bad Request: Transformer exit code was not 0 (possible problem with the source file)
 *   400 Bad Request: The source filename was not supplied
 *   500 Internal Server Error: (no message with low level IO problems)
 *   500 Internal Server Error: The target filename was not supplied (should not happen as targetExtension is checked)
 *   500 Internal Server Error: Transformer version check exit code was not 0
 *   500 Internal Server Error: Transformer version check failed to create any output
 *   500 Internal Server Error: Could not read the target file
 *   500 Internal Server Error: The target filename was malformed (should not happen because of other checks)
 *   500 Internal Server Error: Transformer failed to create an output file (the exit code was 0, so there should be some content)
 *   500 Internal Server Error: Filename encoding error
 *   507 Insufficient Storage: Failed to store the source file
 */
@Controller
public class LibreOfficeController extends AbstractTransformerController
{
    private static final String OFFICE_HOME =  "/opt/libreoffice5.4";

    private static final int JODCONVERTER_TRANSFORMATION_ERROR_CODE = 3088;

    private JodConverter jodconverter;

    @Autowired
    public LibreOfficeController() throws Exception
    {
        logger = LogFactory.getLog(LibreOfficeController.class);

        setJodConverter(createJodConverter());
    }

    private static JodConverter createJodConverter() throws Exception
    {
        JodConverterSharedInstance jodconverter = new JodConverterSharedInstance();

        jodconverter.setOfficeHome(OFFICE_HOME);         // jodconverter.officeHome
        jodconverter.setMaxTasksPerProcess("200");       // jodconverter.maxTasksPerProcess
        jodconverter.setTaskExecutionTimeout("120000");  // jodconverter.maxTasksPerProcess
        jodconverter.setTaskQueueTimeout("30000");       // jodconverter.taskQueueTimeout
        jodconverter.setConnectTimeout("28000");         // jodconverter.connectTimeout
        jodconverter.setPortNumbers("8100");             // jodconverter.portNumbers
        jodconverter.setTemplateProfileDir("");          // jodconverter.templateProfileDir
        jodconverter.setEnabled("true");                 // jodconverter.enabled
        jodconverter.afterPropertiesSet();

        return jodconverter;
    }

    public void setJodConverter(JodConverter jodconverter)
    {
        this.jodconverter = jodconverter;
    }

    @PostMapping("/transform")
    public ResponseEntity<Resource> transform(HttpServletRequest request,
                                              @RequestParam("file") MultipartFile sourceMultipartFile,
                                              @RequestParam("targetExtension") String targetExtension,
                                              @RequestParam(value = "timeout", required = false) Long timeout)
    {
        String targetFilename = createTargetFileName(sourceMultipartFile, targetExtension);
        File sourceFile = createSourceFile(request, sourceMultipartFile);
        File targetFile = createTargetFile(request, targetFilename);
        // Both files are deleted by TransformInterceptor.afterCompletion

        executeTransformCommand(sourceFile, targetFile);

        return createAttachment(targetFilename, targetFile);
    }

    protected void executeTransformCommand(File sourceFile, File targetFile)
    {
        try
        {
            OfficeManager officeManager = jodconverter.getOfficeManager();
            OfficeDocumentConverter converter = new OfficeDocumentConverter(officeManager);
            converter.convert(sourceFile, targetFile);
        }
        catch (OfficeException e)
        {
            throw new TransformException(500, "LibreOffice server conversion failed: \n"+
                    "   from file: " + sourceFile + "\n" +
                    "   to file: " + targetFile,
                    e);
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
            throw new TransformException(500, "Transformer failed to create an output file");
        }
    }

    /**
     * This method produces an empty PDF file at the specified File location.
     * Apache's PDFBox is used to create the PDF file.
     */
    private void produceEmptyPdfFile(File targetFile)
    {
        // If improvement PDFBOX-914 is incorporated, we can do this with a straight call to
        // org.apache.pdfbox.TextToPdf.createPDFFromText(new StringReader(""));
        // https://issues.apache.org/jira/browse/PDFBOX-914

        PDDocument pdfDoc = null;
        PDPageContentStream contentStream = null;
        try
        {
            pdfDoc = new PDDocument();
            PDPage pdfPage = new PDPage();
            // Even though, we want an empty PDF, some libs (e.g. PDFRenderer) object to PDFs
            // that have literally nothing in them. So we'll put a content stream in it.
            contentStream = new PDPageContentStream(pdfDoc, pdfPage);
            pdfDoc.addPage(pdfPage);

            // Now write the in-memory PDF document into the temporary file.
            pdfDoc.save(targetFile.getAbsolutePath());

        }
        catch (IOException iox)
        {
            throw new TransformException(500, "Error creating empty PDF file", iox);
        }
        finally
        {
            if (contentStream != null)
            {
                try
                {
                    contentStream.close();
                }
                catch (IOException ignored)
                {
                    // Intentionally empty
                }
            }
            if (pdfDoc != null)
            {
                try
                {
                    pdfDoc.close();
                }
                catch (IOException ignored)
                {
                    // Intentionally empty.
                }
            }
        }
    }
}
