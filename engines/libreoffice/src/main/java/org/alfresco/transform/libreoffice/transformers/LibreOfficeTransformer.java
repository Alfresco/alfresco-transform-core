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
package org.alfresco.transform.libreoffice.transformers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.star.task.ErrorCodeIOException;
import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.executors.JavaExecutor;
import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;
import org.alfresco.transform.common.TransformException;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.OfficeException;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * JavaExecutor implementation for running LibreOffice transformations. It loads the
 * transformation logic in the same JVM (check the {@link JodConverter} implementation).
 */
@Component
public class LibreOfficeTransformer implements JavaExecutor, CustomTransformerFileAdaptor
{
    private static final Logger logger = LoggerFactory.getLogger(LibreOfficeTransformer.class);

    private static final int JODCONVERTER_TRANSFORMATION_ERROR_CODE = 3088;

    @Value("${transform.core.libreoffice.path}")
    private String path;
    @Value("${transform.core.libreoffice.maxTasksPerProcess}")
    private String maxTasksPerProcess;
    @Value("${transform.core.libreoffice.timeout}")
    private String timeout;
    @Value("${transform.core.libreoffice.portNumbers}")
    private String portNumbers;
    @Value("${transform.core.libreoffice.templateProfileDir}")
    private String templateProfileDir;
    @Value("${transform.core.libreoffice.isEnabled}")
    private String isEnabled;

    private JodConverter jodconverter;

    private final ObjectMapper jsonObjectMapper = new ObjectMapper();

    @PostConstruct
    private void createJodConverter()
    {
        if (path == null || path.isEmpty())
        {
            throw new IllegalArgumentException("LibreOfficeTransformer LIBREOFFICE_HOME cannot be null or empty");
        }
        if (maxTasksPerProcess == null || maxTasksPerProcess.isEmpty() || !StringUtils.isNumeric(maxTasksPerProcess))
        {
            throw new IllegalArgumentException("LibreOfficeTransformer LIBREOFFICE_MAX_TASKS_PER_PROCESS must have a numeric value");
        }
        if (timeout == null || timeout.isEmpty() || !StringUtils.isNumeric(timeout))
        {
            throw new IllegalArgumentException("LibreOfficeTransformer LIBREOFFICE_TIMEOUT must have a numeric value");
        }
        // value parsed and validated in JodConverterSharedInstance#parsePortNumbers(String s, String sys)
        if (portNumbers == null || portNumbers.isEmpty())
        {
            throw new IllegalArgumentException("LibreOfficeTransformer LIBREOFFICE_PORT_NUMBERS variable cannot be null or empty");
        }
        if (templateProfileDir == null)
        {
            throw new IllegalArgumentException("LibreOfficeTransformer LIBREOFFICE_TEMPLATE_PROFILE_DIR variable cannot be null");
        }
        if (isEnabled == null || isEnabled.isEmpty() || !(isEnabled.equalsIgnoreCase("true")|| isEnabled.equalsIgnoreCase("false")))
        {
            throw new IllegalArgumentException("LibreOfficeTransformer LIBREOFFICE_IS_ENABLED variable must be set to true/false");
        }

        JodConverterSharedInstance sharedInstance = new JodConverterSharedInstance();
        jodconverter = sharedInstance;
        sharedInstance.setOfficeHome(path);
        sharedInstance.setMaxTasksPerProcess(maxTasksPerProcess);
        sharedInstance.setTaskExecutionTimeout(timeout);
        sharedInstance.setTaskQueueTimeout(timeout);
        sharedInstance.setConnectTimeout(timeout);
        sharedInstance.setPortNumbers(portNumbers);
        sharedInstance.setTemplateProfileDir(templateProfileDir);
        sharedInstance.setEnabled(isEnabled);
        sharedInstance.afterPropertiesSet();
    }

    @Override
    public String getTransformerName()
    {
        return "libreoffice";
    }

    @Override
    public void transform(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
                          File sourceFile, File targetFile)
    {
        call(sourceFile, targetFile);
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
            throw new TransformException(BAD_REQUEST,
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
            throw new TransformException(INTERNAL_SERVER_ERROR, "Transformer failed to create an output file");
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
             PDPageContentStream ignore = new PDPageContentStream(pdfDoc, pdfPage))
        {
            // Even though, we want an empty PDF, some libs (e.g. PDFRenderer) object to PDFs
            // that have literally nothing in them. So we'll put a content stream in it.
            pdfDoc.addPage(pdfPage);

            // Now write the in-memory PDF document into the temporary file.
            pdfDoc.save(targetFile.getAbsolutePath());
        }
        catch (IOException iox)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR, "Error creating empty PDF file", iox);
        }
    }

    /**
     * @deprecated The JodConverterMetadataExtracter has not been in use since 6.0.1.
     *             This code exists in case there are custom implementations, that need to be converted to T-Engines.
     *             It is simply a copy and paste from the content repository and has received limited testing.
     */
    public void extractMetadata(String transformName, String sourceMimetype, String targetMimetype,
                                Map<String, String> transformOptions,
                                File sourceFile, File targetFile)
    {
        OfficeManager officeManager = jodconverter.getOfficeManager();
        LibreOfficeExtractMetadataTask extractMetadataTask = new LibreOfficeExtractMetadataTask(sourceFile);
        try
        {
            officeManager.execute(extractMetadataTask);
        }
        catch (OfficeException e)
        {
            throw new TransformException(BAD_REQUEST,
                    "LibreOffice metadata extract failed: \n" +
                            "   from file: " + sourceFile, e);
        }
        Map<String, Serializable> metadata = extractMetadataTask.getMetadata();

        if (logger.isDebugEnabled())
        {
            metadata.forEach((k,v) -> logger.debug(k+"="+v));
        }

        writeMetadataIntoTargetFile(targetFile, metadata);
    }

    private void writeMetadataIntoTargetFile(File targetFile, Map<String, Serializable> results)
    {
        try
        {
            jsonObjectMapper.writeValue(targetFile, results);
        }
        catch (IOException e)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR, "Failed to write metadata to targetFile", e);
        }
    }
}
