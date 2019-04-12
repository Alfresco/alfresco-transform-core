package org.alfresco.transformer.executors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringJoiner;

import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transformer.logging.LogEntry;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

/**
 * JavaExecutor implementation for running TIKA transformations. It loads the 
 * transformation logic in the same JVM (check {@link Tika}).
 */
@Component
public class TikaJavaExecutor implements JavaExecutor
{
    private final Tika tika;

    @Autowired
    public TikaJavaExecutor() throws TikaException, IOException, SAXException
    {
        tika = new Tika();
    }

    @Override
    public void call(File sourceFile, File targetFile, String... args)
        throws TransformException
    {
        args = buildArgs(sourceFile, targetFile, args);
        try
        {
            tika.transform(args);
        }
        catch (IllegalArgumentException e)
        {
            throw new TransformException(BAD_REQUEST.value(), getMessage(e));
        }
        catch (Exception e)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(), getMessage(e));
        }
        if (!targetFile.exists() || targetFile.length() == 0)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(),
                "Transformer failed to create an output file");
        }
    }

    private static String getMessage(Exception e)
    {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private static String[] buildArgs(File sourceFile, File targetFile, String[] args)
    {
        ArrayList<String> methodArgs = new ArrayList<>(args.length + 2);
        StringJoiner sj = new StringJoiner(" ");
        for (String arg : args)
        {
            addArg(methodArgs, sj, arg);
        }

        addFileArg(methodArgs, sj, sourceFile);
        addFileArg(methodArgs, sj, targetFile);

        LogEntry.setOptions(sj.toString());

        return methodArgs.toArray(new String[0]);
    }

    private static void addArg(ArrayList<String> methodArgs, StringJoiner sj, String arg)
    {
        if (arg != null)
        {
            sj.add(arg);
            methodArgs.add(arg);
        }
    }

    private static void addFileArg(ArrayList<String> methodArgs, StringJoiner sj, File arg)
    {
        if (arg != null)
        {
            String path = arg.getAbsolutePath();
            int i = path.lastIndexOf('.');
            String ext = i == -1 ? "???" : path.substring(i + 1);
            sj.add(ext);
            methodArgs.add(path);
        }
    }
}
