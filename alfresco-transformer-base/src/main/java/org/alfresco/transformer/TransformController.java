package org.alfresco.transformer;

import static java.text.MessageFormat.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transformer.exceptions.TransformException;
import org.alfresco.transformer.logging.LogEntry;
import org.alfresco.transformer.probes.ProbeTestTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * TransformController interface.
 * <br/>
 * It contains much of the common boilerplate code that each of
 * its concrete implementations need as default methods.
 */
public interface TransformController
{
    Logger logger = LoggerFactory.getLogger(TransformController.class);

    ResponseEntity<TransformReply> transform(TransformRequest transformRequest, Long timeout);

    void processTransform(File sourceFile, File targetFile, Map<String, String> transformOptions,
        Long timeout);

    String getTransformerName();

    ProbeTestTransform getProbeTestTransform();

    default String probe(HttpServletRequest request, boolean isLiveProbe)
    {
        return getProbeTestTransform().doTransformOrNothing(request, isLiveProbe);
    }

    @RequestMapping("/version")
    @ResponseBody
    String version();

    @GetMapping("/")
    default String transformForm(Model model)
    {
        return "transformForm"; // the name of the template
    }

    @GetMapping("/error")
    default String error()
    {
        return "error"; // the name of the template
    }

    @GetMapping("/log")
    default String log(Model model)
    {
        model.addAttribute("title", getTransformerName() + " Log Entries");
        Collection<LogEntry> log = LogEntry.getLog();
        if (!log.isEmpty())
        {
            model.addAttribute("log", log);
        }
        return "log"; // the name of the template
    }

    @GetMapping("/ready")
    @ResponseBody
    default String ready(HttpServletRequest request)
    {
        return probe(request, false);
    }

    @GetMapping("/live")
    @ResponseBody
    default String live(HttpServletRequest request)
    {
        return probe(request, true);
    }

    //region [Exception Handlers]
    @ExceptionHandler(TypeMismatchException.class)
    default void handleParamsTypeMismatch(HttpServletResponse response,
        MissingServletRequestParameterException e) throws IOException
    {
        final String message = format("Request parameter ''{0}'' is of the wrong type", e
            .getParameterName());
        final int statusCode = BAD_REQUEST.value();

        logger.error(message, e);

        LogEntry.setStatusCodeAndMessage(statusCode, message);

        response.sendError(statusCode, getTransformerName() + " - " + message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    default void handleMissingParams(HttpServletResponse response,
        MissingServletRequestParameterException e) throws IOException
    {
        final String message = format("Request parameter ''{0}'' is missing", e.getParameterName());
        final int statusCode = BAD_REQUEST.value();

        logger.error(message, e);

        LogEntry.setStatusCodeAndMessage(statusCode, message);

        response.sendError(statusCode, getTransformerName() + " - " + message);
    }

    @ExceptionHandler(TransformException.class)
    default void transformExceptionWithMessage(HttpServletResponse response,
        TransformException e) throws IOException
    {
        final String message = e.getMessage();
        final int statusCode = e.getStatusCode();

        logger.error(message, e);

        long time = LogEntry.setStatusCodeAndMessage(statusCode, message);
        getProbeTestTransform().recordTransformTime(time);

        response.sendError(statusCode, getTransformerName() + " - " + message);
    }
    //endregion
}
