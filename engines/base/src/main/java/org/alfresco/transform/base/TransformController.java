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
package org.alfresco.transform.base;

import org.alfresco.transform.base.logging.LogEntry;
import org.alfresco.transform.base.probes.ProbeTestTransform;
import org.alfresco.transform.common.TransformException;
import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.registry.TransformServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.text.MessageFormat.format;
import static org.alfresco.transform.common.RequestParamMap.CONFIG_VERSION;
import static org.alfresco.transform.common.RequestParamMap.CONFIG_VERSION_DEFAULT;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_ERROR;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_LOG;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_ROOT;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TEST;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM_CONFIG;
import static org.alfresco.transform.common.RequestParamMap.FILE;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_MIMETYPE;
import static org.alfresco.transform.common.RequestParamMap.TARGET_MIMETYPE;
import static org.alfresco.transform.config.CoreVersionDecorator.setOrClearCoreVersion;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

/**
 * Provides the main endpoints into the t-engine.
 */
@Controller
public class TransformController
{
    private static final Logger logger = LoggerFactory.getLogger(TransformController.class);

    @Autowired(required = false)
    private List<TransformEngine> transformEngines;
    @Autowired
    private TransformServiceRegistry transformRegistry;
    @Autowired
    private TransformHandler transformHandler;
    @Value("${transform.core.version}")
    private String coreVersion;

    private TransformEngine transformEngine;
    ProbeTestTransform probeTestTransform;

    @PostConstruct
    private void init()
    {
        transformEngine = transformHandler.getTransformEngine();
        probeTestTransform = transformHandler.getProbeTestTransform();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startup()
    {
        logger.info("--------------------------------------------------------------------------------------------------------------------------------------------------------------");
        if (transformEngines != null)
        {
            Arrays.stream(transformEngine.getStartupMessage().split("\\n")).forEach(logger::info);
        }
        logger.info("--------------------------------------------------------------------------------------------------------------------------------------------------------------");

        logger.info("Starting application components... Done");
    }

    /**
     * @return a string that may be used in client debug.
     */
    @RequestMapping("/version")
    @ResponseBody
    public String version()
    {
        return transformEngine.getTransformEngineName() + ' ' + coreVersion +  " available";
    }

    /**
     * Test UI page to perform a transform.
     */
    @GetMapping(ENDPOINT_ROOT)
    public String test(Model model)
    {
        model.addAttribute("title", transformEngine.getTransformEngineName() + " Test Page");
        return "test"; // display test.html
    }

    /**
     * Test UI error page.
     */
    @GetMapping(ENDPOINT_ERROR)
    public String error(Model model)
    {
        model.addAttribute("title", transformEngine.getTransformEngineName() + " Error Page");
        return "error"; // display error.html
    }

    /**
     * Test UI log page.
     */
    @GetMapping(ENDPOINT_LOG)
    String log(Model model)
    {
        model.addAttribute("title", transformEngine.getTransformEngineName() + " Log Entries");
        Collection<LogEntry> log = LogEntry.getLog();
        if (!log.isEmpty())
        {
            model.addAttribute("log", log);
        }
        return "log"; // display log.html
    }

    /**
     * Kubernetes readiness probe.
     */
    @GetMapping("/ready")
    @ResponseBody
    public String ready(HttpServletRequest request)
    {
        return probeTestTransform.doTransformOrNothing(request, false, this);
    }

    /**
     * Kubernetes liveness probe.
     */
    @GetMapping("/live")
    @ResponseBody
    public String live(HttpServletRequest request)
    {
        return probeTestTransform.doTransformOrNothing(request, true, this);
    }

    @GetMapping(value = ENDPOINT_TRANSFORM_CONFIG)
    public ResponseEntity<TransformConfig> transformConfig(
            @RequestParam(value = CONFIG_VERSION, defaultValue = CONFIG_VERSION_DEFAULT) int configVersion)
    {
        logger.info("GET Transform Config version: " + configVersion);
        TransformConfig transformConfig = ((TransformRegistryImpl) transformRegistry).getTransformConfig();
        transformConfig = setOrClearCoreVersion(transformConfig, configVersion);
        return new ResponseEntity<>(transformConfig, OK);
    }

    @PostMapping(value = ENDPOINT_TRANSFORM, consumes = MULTIPART_FORM_DATA_VALUE)
    public StreamingResponseBody transform(HttpServletRequest request,
                                              @RequestParam(value = FILE, required = false) MultipartFile sourceMultipartFile,
                                              @RequestParam(value = SOURCE_MIMETYPE, required = false) String sourceMimetype,
                                              @RequestParam(value = TARGET_MIMETYPE, required = false) String targetMimetype,
                                              @RequestParam Map<String, String> requestParameters)
    {
        return transformHandler.handleHttpRequest(request, sourceMultipartFile, sourceMimetype,
                targetMimetype, requestParameters);
    }

    @PostMapping(value = ENDPOINT_TEST, consumes = MULTIPART_FORM_DATA_VALUE)
    public StreamingResponseBody testTransform(HttpServletRequest request,
            @RequestParam(value = FILE, required = false) MultipartFile sourceMultipartFile,
            @RequestParam(value = SOURCE_MIMETYPE, required = false) String sourceMimetype,
            @RequestParam(value = TARGET_MIMETYPE, required = false) String targetMimetype,
            @RequestParam Map<String, String> origRequestParameters)
    {
        // Remaps request parameters from test.html and hands them off to the normal transform endpoint.
        // There are name<i> and value<i> parameters which allow dynamic names and values to be used.
        Map<String, String> requestParameters = new HashMap<>();
        sourceMimetype = overrideMimetypeFromExtension(origRequestParameters, SOURCE_MIMETYPE, sourceMimetype);
        targetMimetype = overrideMimetypeFromExtension(origRequestParameters, TARGET_MIMETYPE, targetMimetype);
        origRequestParameters.forEach((name, value) ->
        {
            if (name.startsWith("value") == false)
            {
                if (name.startsWith("name"))
                {
                    String suffix = name.substring("name".length());
                    name = value;
                    value = origRequestParameters.get("value" + suffix);
                }
                if (name != null && !name.isBlank() && value != null && !value.isBlank())
                {
                    requestParameters.put(name, value);
                }
            }
        });
        return transform(request, sourceMultipartFile, sourceMimetype, targetMimetype, requestParameters);
    }

    private String overrideMimetypeFromExtension(Map<String, String> origRequestParameters, String name, String value)
    {
        String override = origRequestParameters.remove("_"+ name);
        if (override != null && !override.isBlank())
        {
            value = override;
            origRequestParameters.put(name, value);
        }
        return value;
    }

    @ExceptionHandler(TypeMismatchException.class)
    public void handleParamsTypeMismatch(HttpServletResponse response, MissingServletRequestParameterException e) throws IOException
    {
        final String message = format("Request parameter ''{0}'' is of the wrong type", e.getParameterName());
        final int statusCode = BAD_REQUEST.value();

        logger.error(message, e);
        LogEntry.setStatusCodeAndMessage(statusCode, message);
        response.sendError(statusCode, transformEngine.getTransformEngineName() + " - " + message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public void handleMissingParams(HttpServletResponse response, MissingServletRequestParameterException e) throws IOException
    {
        final String message = format("Request parameter ''{0}'' is missing", e.getParameterName());
        final int statusCode = BAD_REQUEST.value();

        logger.error(message, e);
        LogEntry.setStatusCodeAndMessage(statusCode, message);
        response.sendError(statusCode, message);
    }

    @ExceptionHandler(TransformException.class)
    public ModelAndView transformExceptionWithMessage(HttpServletResponse response, TransformException e)
            throws IOException
    {
        final String message = e.getMessage();
        final int statusCode = e.getStatusCode();

        logger.error(message);
        long time = LogEntry.setStatusCodeAndMessage(statusCode, message);
        probeTestTransform.recordTransformTime(time);
        response.sendError(statusCode, message);

        ModelAndView mav = new ModelAndView();
        mav.addObject("title", transformEngine.getTransformEngineName() + " Error Page");
        mav.addObject("message", message);
        mav.setViewName("error"); // display error.html
        return mav;
    }
}
