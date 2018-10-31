# Common code for Docker based ACS transformers

This project contains code that is common between all the ACS transformers that run within their own
Docker containers. It performs common actions such as logging, throttling requests and handling the
streaming of content to and from the container. It also provides structure and hook points to allow
specific transformers to simply check request parameter and perform the transformation using either
files or a pair of InputStream and OutputStream.

A transformer project is expected to provide the following files:

~~~
src/main/resources/templates/transformForm.html
src/main/java/org/alfresco/transformer/<XXX>Controller.java
src/main/java/org/alfresco/transformer/Application.java
~~~

* transformerForm.html - A simple test page using [thymeleaf](http://www.thymeleaf.org) that gathers request
  parameters so they may be used to test the transformer.

~~~
<html xmlns:th="http://www.thymeleaf.org">
<body>
  <div>
    <h2>Test Transformation</h2>
    <form method="POST" enctype="multipart/form-data" action="/transform">
      <table>
        <tr><td><div style="text-align:right">file *</div></td><td><input type="file" name="file" /></td></tr>
        <tr><td><div style="text-align:right">targetFilename *</div></td><td><input type="text" name="targetFilename" value="" /></td></tr>
        <tr><td><div style="text-align:right">width</div></td><td><input type="text" name="width" value="" /></td></tr>
        <tr><td><div style="text-align:right">height</div></td><td><input type="text" name="height" value="" /></td></tr>
        <tr><td><div style="text-align:right">allowEnlargement</div></td><td><input type="checkbox" name="allowEnlargement" value="true" /></td></tr>
        <tr><td><div style="text-align:right">maintainAspectRatio</div></td><td><input type="checkbox" name="maintainAspectRatio" value="true" /></td></tr>
        <tr><td><div style="text-align:right">page</div></td><td><input type="text" name="page" value="" /></td></tr>
        <tr><td><div style="text-align:right">timeout</div></td><td><input type="text" name="timeout" value="" /></td></tr>
        <tr><td></td><td><input type="submit" value="Transform" /></td></tr>
	  </table>
	</form>
  </div>
  <div>
    <a href="/log">Log entries</a>
  </div>
</body>
</html>
~~~

* *TransformerName*Controller.java - A [Spring Boot](https://projects.spring.io/spring-boot/) Controller that
  extends AbstractTransformerController to handel a POST request to *"/transform"*.

~~~
...
@Controller
public class AlfrescoPdfRendererController extends AbstractTransformerController
{
    ...

    @PostMapping("/transform")
    public ResponseEntity<Resource> transform(HttpServletRequest request,
                                              @RequestParam("file") MultipartFile sourceMultipartFile,
                                              @RequestParam("targetFilename") String targetFilename,
                                              @RequestParam(value = "width", required = false) Integer width,
                                              @RequestParam(value = "height", required = false) Integer height,
                                              @RequestParam(value = "allowEnlargement", required = false) Boolean allowEnlargement,
                                              @RequestParam(value = "maintainAspectRatio", required = false) Boolean maintainAspectRatio,
                                              @RequestParam(value = "page", required = false) Integer page,
                                              @RequestParam(value = "timeout", required = false) Long timeout)
    {
        try
        {
            File sourceFile = createSourceFile(request, sourceMultipartFile);
            File targetFile = createTargetFile(request, targetFilename);
            // Both files are deleted by TransformInterceptor.afterCompletion

            StringJoiner args = new StringJoiner(" ");
            if (width != null)
            {
                args.add("--width=" + width);
            }
            if (height != null)
            {
                args.add("--height=" + height);
            }
            if (allowEnlargement != null && allowEnlargement)
            {
                args.add("--allow-enlargement");
            }
            if (maintainAspectRatio != null && maintainAspectRatio)
            {
                args.add("--maintain-aspect-ratio");
            }
            if (page != null)
            {
                args.add("--page=" + page);
            }
            String options = args.toString();
            LogEntry.setOptions(options);

            Map<String, String> properties = new HashMap<>();
            properties.put("options", options);
            properties.put("source", sourceFile.getAbsolutePath());
            properties.put("target", targetFile.getAbsolutePath());

            executeTransformCommand(properties, targetFile, timeout);

            return createAttachment(targetFilename, targetFile);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new TransformException(500, "Filename encoding error", e);
        }
    }
}
~~~

* *TransformerName*Controller#processTransform(File sourceFile, File targetFile, Map<String, String> transformOptions, Long timeout)

### /transform (Consumes: `application/json`, Produces: `application/json`)
The new *consumes* and *produces* arguments have been specified in order to differentiate this endpoint from the previous one (which **consumes** `multipart/form-data`)

The endpoint should **always** receive a `TransformationRequest` and should **always** respond with a `TransformationReply`.

As specific transformers require specific arguments (e.g. `transform` for the **Tika transformer**) the request body should include this in the `transformRequestOptions` via the `Map<String,String> transformRequestOptions`.

**Example request body**
```javascript
var transformRequest = {
	"requestId": "1",
	"sourceReference": "2f9ed237-c734-4366-8c8b-6001819169a4",
	"sourceMediaType": "pdf",
	"sourceSize": 123456,
	"sourceExtension": "pdf",
	"targetMediaType": "txt",
	"targetExtension": "txt",
	"clientType": "ACS",
	"clientData": "Yo No Soy Marinero, Soy Capitan, Soy Capitan!",
	"schema": 1,
	"transformRequestOptions": {
		"targetMimetype": "text/plain",
		"targetEncoding": "UTF-8",
		"transform": "PdfBox"
	}
}
```

**Example response body**

```javascript
var transformReply = {
    "requestId": "1",
    "status": 201,
    "errorDetails": null,
    "sourceReference": "2f9ed237-c734-4366-8c8b-6001819169a4",
    "targetReference": "34d69ff0-7eaa-4741-8a9f-e1915e6995bf",
    "clientType": "ACS",
    "clientData": "Yo No Soy Marinero, Soy Capitan, Soy Capitan!",
    "schema": 1
}
```

### processTransform method
```java
public abstract class AbstractTransformerController 
{
    void processTransform(File sourceFile, File targetFile, Map<String, String> transformOptions, Long timeout) { /* Perform the transformation*/ }
}
```

The **abstract** method is declared in the *AbstractTransformerController* and must be implemented by the specific controllers.

This method is called by the *AbstractTransformerController* directly in the **new** `/transform` endpoint which **consumes** `application/json` and **produces** `application/json`.

The method is responsible for performing the transformation. Upon a **successful** transformation it updates the `targetFile` parameter.

* Application.java - [Spring Boot](https://projects.spring.io/spring-boot/) expects to find an Application in
 a project's source files. The following may be used:

~~~
package org.alfresco.transformer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application
{
    public static void main(String[] args)
    {
        SpringApplication.run(Application.class, args);
    }
}
~~~

## Building and testing

The project can be built by running the Maven command:

~~~
mvn clean install
~~~

## Artifacts

The artifacts can be obtained by:

* downloading from the [Alfresco repository](https://artifacts.alfresco.com/nexus/content/groups/public/)
* Adding a Maven dependency to your pom file.

~~~
<dependency>
  <groupId>org.alfresco</groupId>
  <artifactId>alfresco-transformer-base</artifactId>
  <version>1.0</version>
</dependency>
~~~

and the Alfresco Maven repository:

~~~
<repository>
  <id>alfresco-maven-repo</id>
  <url>https://artifacts.alfresco.com/nexus/content/groups/public</url>
</repository>
~~~

The build plan in Bamboo is PLAT-TB

## Contributing guide

Please use [this guide](https://github.com/Alfresco/alfresco-jodconverter/blob/master/CONTRIBUTING.md) to make a
contribution to the project.
