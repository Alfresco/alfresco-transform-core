# Common base code for T-Engines

This project provides a common base for T-Engines and supersedes the
[original base](https://github.com/Alfresco/alfresco-transform-core/blob/master/deprecated/alfresco-transformer-base). 

This project provides a base Spring Boot application (as a jar) to which transform
specific code may be added. It includes actions such as communication between
components and logging.

For more details on build a custom T-Engine and T-Config, please refer to the docs in ACS Packaging, including:

* [ATS Configuration](https://github.com/Alfresco/acs-packaging/blob/master/docs/custom-transforms-and-renditions.md#ats-configuration)
* [Creating a T-Engine](https://github.com/Alfresco/acs-packaging/blob/master/docs/creating-a-t-engine.md)

## Overview

A T-Engine project which extends this base is expected to provide the following:

* An implementation of the [TransformEngine](https://github.com/Alfresco/alfresco-transform-core/blob/master/engines/base/src/main/java/org/alfresco/transform/base/TransformEngine.java)
  interface to describe the T-Engine. 
* Implementations of the [CustomTransformer](engines/base/src/main/java/org/alfresco/transform/base/CustomTransformer.java)
  interface with the actual transform code.
* An `application-default.yaml` file to define a unique name for the message queue to the T-Engine.

The `TransformEngine` and `CustomTransformer` implementations should have an
`@Component` annotation and be in or below the`org.alfresco.transform` package, so
that they will be discovered by the base T-Engine.

The `TransformEngine.getTransformConfig()` method typically reads a `json` file.
The names in the config should match the names returned by the `CustomTransformer`
implementations.


**Example TransformEngine**

The `TransformEngineName` is important if the config from multiple T-Engines is being
combined as they are sorted by name.
```
package org.alfresco.transform.example;

import com.google.common.collect.ImmutableMap;
import org.alfresco.transform.base.TransformEngine;
import org.alfresco.transform.base.probes.ProbeTransform;
import org.alfresco.transform.config.reader.TransformConfigResourceReader;
import org.alfresco.transform.config.TransformConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HelloTransformEngine implements TransformEngine
{
    @Autowired
    private TransformConfigResourceReader transformConfigResourceReader;

    @Override
    public String getTransformEngineName()
    {
        return "0200_hello";
    }

    @Override
    public String getStartupMessage()
    {
        return "Startup "+getTransformEngineName()+"\nNo 3rd party licenses";
    }

    @Override
    public TransformConfig getTransformConfig()
    {
        return transformConfigResourceReader.read("classpath:hello_engine_config.json");
    }

    @Override
    public ProbeTransform getProbeTransform()
    {
        return new ProbeTransform("probe.txt", "text/plain", "text/plain",
            ImmutableMap.of("sourceEncoding", "UTF-8", "language", "English"),
            11, 10, 150, 1024, 1, 60 * 2);
    }
}
```

**Example CustomTransformer**
```
package org.alfresco.transform.example;

import org.alfresco.transform.base.CustomTransformer;
import org.alfresco.transform.base.TransformManager;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

@Component
public class HelloTransformer implements CustomTransformer
{
    @Override
    public String getTransformerName()
    {
        return "hello";
    }

    @Override
    public void transform(String sourceMimetype, InputStream inputStream, String targetMimetype,
            OutputStream outputStream, Map<String, String> transformOptions, TransformManager transformManager)
            throws Exception
    {
        String name = new String(inputStream.readAllBytes(), transformOptions.get("sourceEncoding"));
        String greeting = String.format(getGreeting(transformOptions.get("language")), name);
        byte[] bytes = greeting.getBytes(transformOptions.get("sourceEncoding"));
        outputStream.write(bytes, 0, bytes.length);
    }

    private String getGreeting(String language)
    {
        return "Hello %s";
    }
}
```

**Example T-Config** `resources/hello_engine_config.json`
```json
{
  "transformOptions": {
    "helloOptions": [
      {"value": {"name": "language"}},
      {"value": {"name": "sourceEncoding"}}
    ]
  },
  "transformers": [
    {
      "transformerName": "hello",
      "supportedSourceAndTargetList": [
        {"sourceMediaType": "text/plain", "targetMediaType": "text/plain" }
      ],
      "transformOptions": [
        "helloOptions"
      ]
    }
  ]
}
```

**Example properties** `resources/application-default.yaml`

As can be seen the following defines a default which can be overridden by an environment variable.
```yaml
queue:
  engineRequestQueue: ${TRANSFORM_ENGINE_REQUEST_QUEUE:org.alfresco.transform.engine.libreoffice.acs}
```

**Example ProbeTransform test file** `resources/probe.txt`
```json
Jane
```