# Transform specific code

To create a new t-engine an author uses a base t-engine (a Spring Boot
application) and implements the following interfaces. An implementation of
the `CustomTransformer` provides the actual transformation code and the
implementation of the `TransformEngine` says what it is capable of
transforming. The `TransformConfig` is normally read from a json file on the
classpath. Multiple `CustomTransformer` implementations may be in a singe
t-engine. As a result the author can concentrate on the code that transforms
one format to another without really worrying about all the plumbing.
Typically, the transform specific code uses a 3rd party library or an
external executable which needs to be added to the Docker image.

~~~java
package org.alfresco.transform;

import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transformer.probes.ProbeTestTransform;

import java.util.Set;

/**
 * Interface to be implemented by transform specific code. Provides information
 * about the t-engine as a whole. So that it is automatically picked up, it must
 * exist in a package under {@code org.alfresco.transform} and have the Spring
 * {@code @Component} annotation.
 */
public interface TransformEngine
{
    /**
      * @return the name of the t-engine. The t-router reads config from t-engines
      *         in name order.
      */
    String getTransformEngineName();

    /**
     * @return a definition of what the t-engine supports. Normally read from a json
     *         Resource on the classpath.
     */
    TransformConfig getTransformConfig();

    /**
     * @return a ProbeTestTransform (will do a quick transform) for k8 liveness and
     *         readiness probes.
     */
    ProbeTransform getProbeTransform();
}
~~~

implementations of the following interface provide the actual transform code.

~~~java
package org.alfresco.transform;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Interface to be implemented by transform specific code. The
 * {@code transformerName} should match the transformerName in the
 * {@link TransformConfig} returned by the {@link TransformEngine}. So that it is
 * automatically picked up, it must exist in a package under
 * {@code org.alfresco.transform} and have the Spring {@code @Component} annotation.
 *
 * Implementations may also use the {@link TransformManager} if they wish to
 * interact with the base t-engine.
 */
public interface CustomTransformer
{
    String getTransformerName();

    void transform(String sourceMimetype, InputStream inputStream,
                   String targetMimetype, OutputStream outputStream,
                   Map<String, String> transformOptions,
                   TransformManager transformManager) throws Exception;
}
~~~

The implementation of the following interface is provided by the t-base,
allows the `CustomTransformer` to interact with the base t-engine. The
creation of Files is discouraged as it is better not to leave files on disk.

~~~java
package org.alfresco.transform.base;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Allows {@link CustomTransformer} implementations to interact with the base
 * t-engine.
 */
public interface TransformManager
{
    /**
     * Allows a CustomTransformer to use a local source File rather than the
     * supplied InputStream. To avoid creating extra files, if a File has already
     * been created by the base t-engine, it is returned.
     */
    File createSourceFile();

    /**
     * Allows a CustomTransformer to use a local target File rather than the
     * supplied OutputStream. To avoid creating extra files, if a File has already
     * been created by the base t-engine, it is returned.
     */
    File createTargetFile();

    /**
     * Allows a single transform request to have multiple transform responses. For
     * example, images from a video at different time offsets or different pages of
     * a document. Following a call to this method a transform response is made with
     * the data sent to the current {@code OutputStream}. If this method has been
     * called, there will not be another response when {@link CustomTransformer#
     * transform(String, InputStream, String, OutputStream, Map, TransformManager)}
     * returns and any data written to the final {@code OutputStream} will be
     * ignored.
     * @param index    returned with the response, so that the fragment may be
     *                 distinguished from other responses. Renditions use the index
     *                 as an offset into elements. A {@code null} value indicates
     *                 that there is no more output and any data sent to the current
     *                 {@code outputStream} will be ignored.
     * @param finished indicates this is the final fragment. {@code False} indicates
     *                 that it is expected there will be more fragments. There need
     *                 not be a call with this parameter set to {@code true}.
     * @return a new {@code OutputStream} for the next fragment. A {@code null} will
     *                 be returned if {@code index} was {@code null} or {@code
     *                 finished} was {@code true}.
     * @throws TransformException if a synchronous (http) request has been made as
     *                 this only works with requests on queues, or the first call to
     *                 this method indicated there was no output, or another call is
     *                 made after it has been indicated that there should be no more
     *                 fragments.
     * @throws IOException if there was a problem sending the response.
    OutputStream respondWithFragment(Integer index);
}
~~~
