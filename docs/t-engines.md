# T-Engines

The t-engines provide the basic transform operations. The Transform Service
provides a common base for the communication with other components. It is
this base that is described in this section. The base is a Spring Boot
application to which transform specific code is added and then wrapped
in a Docker image with any programs that the transforms need. The base
does not need to be used as long as there appears to be a process responding
endpoints and messages.

A t-engine groups together one of more Transformers. Each Transformer
(provided by transform specific code) knows how to perform a set of
transformations from one MIME Type to another with a common set of
t-options.

~~~yaml
0010 my-t-engine
  Transformer 1
    mimetype A -> mimetype B
    mimetype A -> mimetype C
    mimetype B -> mimetype C
    option1
    option2
  Transformer 2
    mimetype A -> mimetype B
    mimetype D -> mimetype C
    option2
    option3
0020 another-t-engine
  ...
0030 yet-another-t-engine
  ...
~~~

## Endpoints

* `POST /transform` to perform a transform. There are two forms:
  * For asynchronous transforms: Perform a transform using a
    `TransformRequest` received from the t-router via a message queue. The
    `TransformReply` is sent back via the queue.
  * For synchronous transforms: Performs a transform on content uploaded as
    a Multipart File and provides the resulting content as a download.
    Transform options are extracted from the request properties. The
    following are not added as transform options, but are used to select the
    transformer: `sourceMimetype` & `targetMimetype`.
* `GET /transform/config` to obtain t-config about what the t-engine supports.
  It has a parameter `configVersion` to allow a caller and the t-engine to
  negotiate down to a common format. The value is an integer which indicate
  which elements may to be added to the config. These elements reflect
  functionality supported by the base (such as pre-signed URLs). The
  `CoreVersionDecorator` adds to the Config returned by the transform
  specific code.
* `GET /` provides an html test page to upload a source file, enter transform
  options and issue a synchronous transform request. Useful in testing.
* `GET /log` provides a page with basic log information. Useful in testing.
* `GET /error` provides an error page when testing.
* `GET /version` provides a String message to be included in client debug
  messages.
* `GET /ready` used by Kubernetes as a ready probe.
* `GET /live` used by Kubernetes as a ready probe.
