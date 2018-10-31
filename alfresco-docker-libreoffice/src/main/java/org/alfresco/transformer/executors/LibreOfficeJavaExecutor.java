package org.alfresco.transformer.executors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.alfresco.transformer.exceptions.TransformException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.OfficeException;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.springframework.stereotype.Component;

import com.sun.star.task.ErrorCodeIOException;

/**
 * JavaExecutor implementation for running LibreOffice transformations. It loads the 
 * transformation logic in the same JVM (check the {@link JodConverter} implementation).
 */
@Component
public class LibreOfficeJavaExecutor implements JavaExecutor
{
    private static final Log logger = LogFactory.getLog(LibreOfficeJavaExecutor.class);

    private static final int JODCONVERTER_TRANSFORMATION_ERROR_CODE = 3088;
    private static final String OFFICE_HOME = "/opt/libreoffice5.4";

    private JodConverter jodconverter;

    @PostConstruct
    public void init()
    {
        jodconverter = createJodConverter();
    }

    private static JodConverter createJodConverter()
    {
        final String timeout = "120000";

        JodConverterSharedInstance jodconverter = new JodConverterSharedInstance();

        jodconverter.setOfficeHome(OFFICE_HOME);         // jodconverter.officeHome
        jodconverter.setMaxTasksPerProcess("200");       // jodconverter.maxTasksPerProcess
        jodconverter.setTaskExecutionTimeout(timeout);   // jodconverter.maxTaskExecutionTimeout
        jodconverter.setTaskQueueTimeout("30000");       // jodconverter.taskQueueTimeout
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
