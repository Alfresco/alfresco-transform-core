/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2023 Alfresco Software Limited
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

import static java.text.MessageFormat.format;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import static org.alfresco.transform.base.html.OptionsHelper.getOptionNames;
import static org.alfresco.transform.common.RequestParamMap.CONFIG_VERSION;
import static org.alfresco.transform.common.RequestParamMap.CONFIG_VERSION_DEFAULT;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_ERROR;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_LIVE;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_LOG;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_READY;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_ROOT;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TEST;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM_CONFIG;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_VERSION;
import static org.alfresco.transform.common.RequestParamMap.FILE;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_MIMETYPE;
import static org.alfresco.transform.common.RequestParamMap.TARGET_MIMETYPE;
import static org.alfresco.transform.config.CoreVersionDecorator.setOrClearCoreVersion;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.annotation.PostConstruct;
import jakarta.jms.Destination;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import org.alfresco.transform.base.logging.LogEntry;
import org.alfresco.transform.base.probes.ProbeTransform;
import org.alfresco.transform.base.registry.TransformRegistry;
import org.alfresco.transform.base.transform.TransformHandler;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.exceptions.TransformException;

/**
 * Provides the main endpoints into the t-engine.
 */
@Controller
public class TransformController
{
    private static final Logger logger = LoggerFactory.getLogger(TransformController.class);

    private static final String MODEL_TITLE = "title";
    private static final String MODEL_PROXY_PATH_PREFIX = "proxyPathPrefix";
    private static final String MODEL_MESSAGE = "message";
    private static final String X_ALFRESCO_RETRY_NEEDED_HEADER = "X-Alfresco-Retry-Needed";

    @Autowired(required = false)
    private List<TransformEngine> transformEngines;
    @Autowired
    private TransformRegistry transformRegistry;
    @Autowired
    TransformHandler transformHandler;
    @Autowired
    private String coreVersion;
    @Value("${container.behind-ingres}")
    private boolean behindIngres;

    TransformEngine transformEngine;
    private final AtomicReference<ProbeTransform> probeTransform = new AtomicReference<>();

    @PostConstruct
    private void initTransformEngine()
    {
        if (transformEngines != null)
        {
            transformEngine = getTransformEngine();
            logger.info("TransformEngine: {}", transformEngine.getTransformEngineName());
            transformEngines.stream()
                    .filter(transformEngineFromStream -> transformEngineFromStream != transformEngine)
                    .sorted(Comparator.comparing(TransformEngine::getTransformEngineName))
                    .map(sortedTransformEngine -> "  " + sortedTransformEngine.getTransformEngineName()).forEach(logger::info);
        }
    }

    private TransformEngine getTransformEngine()
    {
        // Normally there is just one TransformEngine per t-engine, but we also want to be able to amalgamate the
        // CustomTransform code from many t-engines into a single t-engine. In this case, there should be a wrapper
        // TransformEngine (it has no TransformConfig of its own).
        return transformEngines.stream()
                .filter(transformEngineFromStream -> transformEngineFromStream.getTransformConfig() == null)
                .findFirst()
                .orElse(transformEngines.get(0));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startup()
    {
        logger.info("--------------------------------------------------------------------------------------------------------------------------------------------------------------");
        if (transformEngines != null)
        {
            logSplitMessage(transformEngine.getStartupMessage());
        }
        logger.info("--------------------------------------------------------------------------------------------------------------------------------------------------------------");
        logger.info("Starting application components... Done");
    }

    private void logSplitMessage(String message)
    {
        Arrays.stream(message.split("\\n")).forEach(logger::info);
    }

    /**
     * @return a string that may be used in client debug.
     */
    @RequestMapping(ENDPOINT_VERSION)
    @ResponseBody
    public String version()
    {
        return getSimpleTransformEngineName() + ' ' + coreVersion;
    }

    /**
     * Test UI page to perform a transform.
     */
    @GetMapping(ENDPOINT_ROOT)
    public String test(Model model)
    {
        model.addAttribute(MODEL_TITLE, getSimpleTransformEngineName() + " Test Page");
        model.addAttribute(MODEL_PROXY_PATH_PREFIX, getPathPrefix());
        TransformConfig transformConfig = transformRegistry.getTransformConfig();
        transformConfig = setOrClearCoreVersion(transformConfig, 0);
        model.addAttribute("transformOptions", getOptionNames(transformConfig.getTransformOptions()));
        return "test"; // display test.html
    }

    /**
     * Test UI error page.
     */
    @GetMapping(ENDPOINT_ERROR)
    public String error(Model model)
    {
        model.addAttribute(MODEL_TITLE, getSimpleTransformEngineName() + " Error Page");
        model.addAttribute(MODEL_PROXY_PATH_PREFIX, getPathPrefix());
        return "error"; // display error.html
    }

    /**
     * Test UI log page.
     */
    @GetMapping(ENDPOINT_LOG)
    String log(Model model)
    {
        model.addAttribute(MODEL_TITLE, getSimpleTransformEngineName() + " Log Entries");
        model.addAttribute(MODEL_PROXY_PATH_PREFIX, getPathPrefix());
        Collection<LogEntry> log = LogEntry.getLog();
        if (!log.isEmpty())
        {
            model.addAttribute("log", log);
        }
        return "log"; // display log.html
    }

    private Object getPathPrefix()
    {
        String pathPrefix = "";
        if (behindIngres)
        {
            pathPrefix = "/" + getSimpleTransformEngineName().toLowerCase();
        }
        return pathPrefix;
    }

    private String getSimpleTransformEngineName()
    {
        return transformEngine.getTransformEngineName().replaceFirst("^\\d+ ", "");
    }

    /**
     * Kubernetes readiness probe.
     */
    @GetMapping(ENDPOINT_READY)
    @ResponseBody
    public String ready(HttpServletRequest request)
    {
        // An alternative without transforms might be: ((TransformRegistry)transformRegistry).isReadyForTransformRequests();
        return getProbeTransform().doTransformOrNothing(false, transformHandler);
    }

    /**
     * Kubernetes liveness probe.
     */
    @GetMapping(ENDPOINT_LIVE)
    @ResponseBody
    public String live(HttpServletRequest request)
    {
        return getProbeTransform().doTransformOrNothing(true, transformHandler);
    }

    public ProbeTransform getProbeTransform()
    {
        ProbeTransform probe = probeTransform.get();
        if (probe != null)
        {
            return probe;
        }
        probe = transformEngine.getProbeTransform();
        if (probeTransform.compareAndSet(null, probe))
        {
            return probe;
        }
        return probeTransform.get();
    }

    @GetMapping(value = ENDPOINT_TRANSFORM_CONFIG)
    public ResponseEntity<TransformConfig> transformConfig(
            @RequestParam(value = CONFIG_VERSION, defaultValue = CONFIG_VERSION_DEFAULT) int configVersion)
    {
        logger.info("GET Transform Config version: " + configVersion);
        TransformConfig transformConfig = transformRegistry.getTransformConfig();
        transformConfig = setOrClearCoreVersion(transformConfig, configVersion);

        if (transformRegistry.isRecoveryModeOn())
        {
            return ResponseEntity.ok().header(X_ALFRESCO_RETRY_NEEDED_HEADER, "RecoveryModeOn").body(transformConfig);
        }
        else
        {
            return ResponseEntity.ok().body(transformConfig);
        }
    }

    // Only used for testing, but could be used in place of the /transform endpoint used by Alfresco Repository's
    // 'Local Transforms'. In production, TransformRequests are processed is via a message queue.
    @PostMapping(value = ENDPOINT_TRANSFORM, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<TransformReply> transform(@RequestBody TransformRequest request,
            @RequestParam(value = "timeout", required = false) Long timeout,
            @RequestParam(value = "replyToQueue", required = false) Destination replyToQueue)
    {
        TransformReply reply = transformHandler.handleMessageRequest(request, timeout, replyToQueue, getProbeTransform());
        return new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus()));
    }

    // Used by Alfresco Repository's 'Local Transforms'. Uploads the content and downloads the result.
    @PostMapping(value = ENDPOINT_TRANSFORM, consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> transform(HttpServletRequest request,
            @RequestParam(value = FILE, required = false) MultipartFile sourceMultipartFile,
            @RequestParam(value = SOURCE_MIMETYPE) String sourceMimetype,
            @RequestParam(value = TARGET_MIMETYPE) String targetMimetype,
            @RequestParam Map<String, String> requestParameters)
    {
        return transformHandler.handleHttpRequest(request, sourceMultipartFile, sourceMimetype,
                targetMimetype, requestParameters, getProbeTransform());
    }

    // Used the t-engine's simple html test UI.
    @PostMapping(value = ENDPOINT_TEST, consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> testTransform(HttpServletRequest request,
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
        origRequestParameters.forEach((name, value) -> {
            if (!name.startsWith("value"))
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
        String override = origRequestParameters.remove("_" + name);
        if (override != null && !override.isBlank())
        {
            value = override;
            origRequestParameters.put(name, value);
        }
        return value;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public void handleMissingParams(HttpServletResponse response, MissingServletRequestParameterException e)
            throws IOException
    {
        final String message = format("Request parameter ''{0}'' is missing", e.getParameterName());
        logger.error(message, e);
        response.sendError(BAD_REQUEST.value(), message);
    }

    @ExceptionHandler(TransformException.class)
    public ModelAndView handleTransformException(HttpServletResponse response, TransformException e)
            throws IOException
    {
        final String message = e.getMessage();
        logger.error(message);
        response.sendError(e.getStatus().value(), message);

        ModelAndView mav = new ModelAndView();
        mav.addObject(MODEL_TITLE, getSimpleTransformEngineName() + " Error Page");
        mav.addObject(MODEL_PROXY_PATH_PREFIX, getPathPrefix());
        mav.addObject(MODEL_MESSAGE, message);
        mav.setViewName("error"); // display error.html
        return mav;
    }
}
