# T-Engine configuration

Each t-engine provides an endpoint that returns t-config that defines what
it supports. The t-router and t-engines may also have external t-config files.
These are combined in name order. As sorting is alphanumeric, you may wish to
consider using a fixed length numeric prefix in filenames and t-engine names. As will be seen
t-config may reference elements from other components or modify elements
from earlier t-config.

Current configuration files are:
* [Pdf-Renderer T-Engine configuration](https://github.com/Alfresco/alfresco-transform-core/blob/master/engines/pdfrenderer/src/main/resources/pdfrenderer_engine_config.json).
* [ImageMagick T-Engine configuration](https://github.com/Alfresco/alfresco-transform-core/blob/master/engines/imagemagick/src/main/resources/imagemagick_engine_config.json).
* [Libreoffice T-Engine configuration](https://github.com/Alfresco/alfresco-transform-core/blob/master/engines/libreoffice/src/main/resources/libreoffice_engine_config.json).
* [Tika T-Engine configuration](https://github.com/Alfresco/alfresco-transform-core/blob/master/engines/tika/src/main/resources/tika_engine_config.json).
* [Misc T-Engine configuration](https://github.com/Alfresco/alfresco-transform-core/blob/master/engines/misc/src/main/resources/misc_engine_config.json).

Additional config files (which may be resources on the classpath or external
files) are specified in Spring Boot properties or such as
`transform.config.file.<filename>` or environment variables like
`TRANSFORM_CONFIG_FILE_<filename>`.

The following is a simple t-config file from an example Hello World
t-engine.

~~~json
{
  "transformOptions":
  {
    "helloWorldOptions":
    [
      {"value": {"name": "language"}}
    ]
  },
  "transformers":
  [
    {
      "transformerName": "helloWorld",
      "supportedSourceAndTargetList":
      [
        {"sourceMediaType": "text/plain",  "maxSourceSizeBytes": 50, "targetMediaType": "text/html"  }
      ],
      "transformOptions":
      [
        "helloWorldOptions"
      ]
    }
  ]
}
~~~

* **transformOptions** provides a list of transform options (each with its own
  name) that may be referenced for use in different transformers. This way
  common options don't need to be repeated for each transformer. They can
  even be shared between T-Engines. In this example there is only one group
  of options called `helloWorldOptions`, which has just one option the
  `language`. Unless an option has a `"required": true` field it is considered
  to be optional. You don't need to specify _sourceMimetype, sourceExtension,
  sourceEncoding, targetMimetype, targetExtension_ or _timeout_ as options as
  these are available to all transformers.
* **transformers** is a list of transformer definitions. Each transformer
  definition should have a unique `transformerName`, specify a
  `supportedSourceAndTargetList` and indicate which options it supports.
  In this case there is only one transformer called `Hello World` and it
  accepts `helloWorldOptions`. A transformer may specify references to 0
  or more transformOptions.
* **supportedSourceAndTargetList** is simply a list of source and target
  Media Types that may be transformed, optionally specifying
  `maxSourceSizeBytes` and `priority` values. In this case there is only one
  from text to HTML and we have limited the source file size, to avoid
  transforming files that clearly don't contain names.

## Transform pipelines

Transforms may be combined in a pipeline to form a new transformer, where
the output from one becomes the input to the next and so on. The t-config
defines the sequence of transform steps and intermediate Media Types. Like
any other transformer, it specifies a list of supported source and target
Media Types. If you don't supply any, all possible combinations are assumed
to be available. The definition may reuse the `transformOptions` of
transformers in the pipeline, but typically will define its own subset
of these.

The following example begins with the `helloWorld` Transformer, which takes a
text file containing a name and produces an HTML file with `Hello <name>`
message in the body. This is then transformed back into a text file. This
example contains just one pipeline transformer, but many may be defined 
in the same file.

~~~json
{
  "transformers": [
    {
      "transformerName": "helloWorldText",
      "transformerPipeline" : [
        {"transformerName": "helloWorld", "targetMediaType": "text/html"},
        {"transformerName": "html"}
      ],
      "supportedSourceAndTargetList": [
        {"sourceMediaType": "text/plain", "priority": 45,  "targetMediaType": "text/plain" }
      ],
      "transformOptions": [
        "helloWorldOptions"
      ]
    }
  ]
}
~~~

* **transformerName** Try to create a unique name for the transform.
* **transformerPipeline** A list of transformers in the pipeline. The
  `targetMediaType` specifies the intermediate Media Types between
  transformers. There is no final `targetMediaType` as this comes from the
  `supportedSourceAndTargetList`. The `transformerName` may reference a
  transformer that has not been defined yet. A warning is issued if
  it remains undefined after all t-config has been combined. Generally
  it is better for a t-engine rather than the t-router to define pipeline
  transformers as this limits the number of places that have to be changed.
  Normally it is obvious which t-engine should contain the definition. 
* **supportedSourceAndTargetList** The supported source and target Media
  Types, which refer to the Media Types this pipeline transformer can
  transform from and to, additionally you can set the `priority` and the
  `maxSourceSizeBytes`. If blank, this indicates that all possible
  combinations are supported. This is the cartesian product of all source
  types to the first intermediate type and all target types from the last
  intermediate type. Any combinations supported by the first transformer
  are excluded. They will also have the priority from the first transform.
* **transformOptions** A list of references to options required by the
  pipeline transformer.

## Failover transforms

A failover transform, simply provides a list of transforms to be attempted
one after another until one succeeds. For example, you may have a fast
transform that is able to handle a limited set of transforms and another
that is slower but handles all cases.

~~~json
{
  "transformers": [
    {
      "transformerName": "imgExtractOrImgCreate",
      "transformerFailover" : [ "imgExtract", "imgCreate" ],
      "supportedSourceAndTargetList": [
        {"sourceMediaType": "application/vnd.oasis.opendocument.graphics", "priority": 150, "targetMediaType": "image/png" },
        ...
        {"sourceMediaType": "application/vnd.sun.xml.calc.template",       "priority": 150, "targetMediaType": "image/png" }
      ]
    }
  ]
}
~~~

* **transformerName** Try to create a unique name for the transform.
* **transformerFaillover** A list of transformers to try. This may include
  references to transformer that have not been defined yet. Generally it
  is better for the t-engine rather than the t-router to define failover
  transformers as this limits the number of places that have to be changed.
  Normally it is obvious which t-engine should contain the definition. 
* **supportedSourceAndTargetList** The supported source and target Media
  Types, which refer to the Media Types this failover transformer can
  transform from and to, additionally you can set the `priority` and the
  `maxSourceSizeBytes`. Unlike pipelines, it must not be blank.
* **transformOptions** A list of references to options required by the 
  pipeline transformer.

## Overriding transforms

It is possible to override a previously defined transform definition. The
following example removes most of the supported source to target media
types from the standard `"libreoffice"` transform. It also changes the
max size and priority of others. This is not something you would normally
want to do.

~~~json
{
  "transformers": [
    {
      "transformerName": "libreoffice",
      "supportedSourceAndTargetList": [
        {"sourceMediaType": "text/csv", "maxSourceSizeBytes": 1000, "targetMediaType": "text/html" },
        {"sourceMediaType": "text/csv", "targetMediaType": "application/vnd.oasis.opendocument.spreadsheet" },
        {"sourceMediaType": "text/csv", "targetMediaType": "application/vnd.oasis.opendocument.spreadsheet-template" },
        {"sourceMediaType": "text/csv", "targetMediaType": "text/tab-separated-values" },
        {"sourceMediaType": "text/csv", "priority": 45, "targetMediaType": "application/vnd.ms-excel" },
        {"sourceMediaType": "text/csv", "priority": 155, "targetMediaType": "application/pdf" }
      ]
    }
  ]
}
~~~

## Removing a transformer

To discard a previous transformer definition include its name in the
optional `"removeTransformers"` list. You might want to do this if you
have a replacement and wish keep the overall configuration simple (so it
contains no alternatives), or you wish to temporarily remove it. The
following example removes two transformers before processing any other
configuration in the same T-Engine or pipeline file.

~~~json
{
  "removeTransformers" : [
    "libreoffice",
    "Archive"
   ]
  ...
}
~~~

## Overriding the supportedSourceAndTargetList

Rather than totally override an existing transform definition, it is
generally simpler to modify the `"supportedSourceAndTargetList"` by adding
elements to the optional `"addSupported"`, `"removeSupported"` and
`"overrideSupported"` lists. You will need to specify the
`"transformerName"` but you will not need to repeat all the other
`"supportedSourceAndTargetList"` values, which means if there are changes
in the original, the same change is not needed in a second place. The
following example adds one transform, removes two others and changes
the `"priority"` and `"maxSourceSizeBytes"` of another. This is done before
processing any other configuration in the same T-Engine or pipeline file.

~~~json
{
  "addSupported": [
    {
      "transformerName": "Archive",
      "sourceMediaType": "application/zip",
      "targetMediaType": "text/csv",
      "priority": 60,
      "maxSourceSizeBytes": 18874368
    }
  ],
  "removeSupported": [
    {
      "transformerName": "Archive",
      "sourceMediaType": "application/zip",
      "targetMediaType": "text/xml"
    },
    {
      "transformerName": "Archive",
      "sourceMediaType": "application/zip",
      "targetMediaType": "text/plain"
    }
  ],
  "overrideSupported": [
    {
      "transformerName": "Archive",
      "sourceMediaType": "application/zip",
      "targetMediaType": "text/html",
      "priority": 60,
      "maxSourceSizeBytes": 18874368
    }
  ]
  ...
}
~~~

## Default maxSourceSizeBytes and priority values

When defining `"supportedSourceAndTargetList"` elements the `"priority"`
and `"maxSourceSizeBytes"` are optional and normally have the default
values of 50 and -1 (no limit). It is possible to change those defaults.
In precedence order from most specific to most general these are defined
by combinations of `"transformerName"` and `"sourceMediaType"`.

* **transformer and source media type default** both specified
* **transformer** default only the transformer name is specified
* **source media type default** only the source media type is specified
* **system wide default** neither are specified.

Both `"priority"` and `"maxSourceSizeBytes"` may be specified in an element,
but if only one is specified it is only that value that is being defaulted.

Being able to change the defaults is particularly useful once a T-Engine
has been developed as it allows a system administrator to handle
limitations that are only found later. The `system wide defaults` are
generally not used but are included for completeness. The following
example says that the `"Office"` transformer by default should only handle 
zip files up to 18 Mb and by default the maximum size of a `.doc` file to be
transformed is 4 Mb. The third example defaults the priority, possibly
allowing another transformer that has specified a priority of say `50` to
be used in preference.

Defaults values are only applied after all t-config has been read.

~~~json
{
  "supportedDefaults": [
    {
      "transformerName": "Office",             // default for a source type within a transformer
      "sourceMediaType": "application/zip",
      "maxSourceSizeBytes": 18874368
    },
    {
      "sourceMediaType": "application/msword", // defaults for a source type
      "maxSourceSizeBytes": 4194304,
      "priority": 45
    },
    {
      "priority": 60                           // system wide default
    },
    {
      "maxSourceSizeBytes": -1                 // system wide default
    }
  ]
  ...
}
~~~
