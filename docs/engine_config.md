## T-Engine configuration

T-Engines provide a */transform/config* end point for clients (e.g. Transform-Router or
Alfresco-Repository) that indicate what is supported. T-Engines store this
configuration as a JSON resource file named *engine_config.json*.

The config can be found under `alfresco-transform-core\<t-engine-name>\src\main\resources
\engine_config.json`; current configuration files are:
* [Pdf-Renderer T-Engine configuration](https://github.com/Alfresco/alfresco-transform-core/blob/master/alfresco-docker-alfresco-pdf-renderer/src/main/resources/engine_config.json).
* [ImageMagick T-Engine configuration](https://github.com/Alfresco/alfresco-transform-core/blob/master/alfresco-docker-imagemagick/src/main/resources/engine_config.json).
* [Libreoffice T-Engine configuration](https://github.com/Alfresco/alfresco-transform-core/blob/master/alfresco-docker-libreoffice/src/main/resources/engine_config.json).
* [Tika T-Engine configuration](https://github.com/Alfresco/alfresco-transform-core/blob/master/alfresco-docker-tika/src/main/resources/engine_config.json).
* [Misc T-Engine configuration](https://github.com/Alfresco/alfresco-transform-core/blob/master/alfresco-docker-transform-misc/src/main/resources/engine_config.json).

*Snippet from Tika T-engine configuration:*
```json
{
  "transformOptions": {
    "tikaOptions": [
      {"value": {"name": "targetEncoding"}}
    ],
    "pdfboxOptions": [
      {"value": {"name": "notExtractBookmarksText"}},
      {"value": {"name": "targetEncoding"}}
    ]
  },
  "transformers": [
    {
      "transformerName": "PdfBox",
      "supportedSourceAndTargetList": [
        {"sourceMediaType": "application/pdf",                                 "targetMediaType": "text/html"},
        {"sourceMediaType": "application/pdf", "maxSourceSizeBytes": 26214400, "targetMediaType": "text/plain"}
      ],
      "transformOptions": [
        "pdfboxOptions"
      ]
    },
    {
      "transformerName": "TikaAuto",
      "supportedSourceAndTargetList": [
        {"sourceMediaType": "application/msword",              "priority": 55, "targetMediaType": "text/xml"}
      ],
      "transformOptions": [
        "tikaOptions"
      ]
    },
    {
      "transformerName": "TextMining",
      "supportedSourceAndTargetList": [
        {"sourceMediaType": "application/msword",                              "targetMediaType": "text/xml"}
      ],
      "transformOptions": [
        "tikaOptions"
      ]
    }
  ]
}
```

### Transform Options
*  **transformOptions** provides a list of transform options that may be
  referenced for use in different transformers. This way common options
  don't need to be repeated for each transformer, they can be shared between
  T-Engines. In this example there are two groups of options called **tikaOptions**
  and **pdfboxOptions** which has a group of options **targetEncoding** and
  **notExtractBookmarksText**. Unless an option has a **"required": true** field it is
  considered to be optional. You don't need to specify *sourceMimetype*,
  *targetMimetype*, *sourceExtension* or *targetExtension* as options as
  these are automatically added.

  *Snippet from ImageMagick T-engine configuration:*
```json
    "transformOptions": {
      "imageMagickOptions": [
        {"value": {"name": "alphaRemove"}},
        {"group": {"transformOptions": [
          {"value": {"name": "cropGravity"}},
          {"value": {"name": "cropWidth"}},
          {"value": {"name": "cropHeight"}},
          {"value": {"name": "cropPercentage"}},
          {"value": {"name": "cropXOffset"}},
          {"value": {"name": "cropYOffset"}}
        ]}},
      ]
    },
```
*  There are two types of transformOptions, *transformOptionsValue* and *transformOptionsGroup*:
   *  _TransformOptionsValue_ is used to represent a single transformation option, it is defined
   by a **name** and an optional **required** field.
   *  _TransformOptionGroup_ represents a group of one or more options, it is used to group
   options that define a
   characteristic. In the above snippet all the options for crop are defined under a group, it is recommended to
   use this approach as it is easier to read. A transformOptionsGroup can contain one or more transformOptionsValue
   and transformOptionsGroup.

  **Limitations**:
  * For a transformOptions to be referenced in a different T-engine, another transformer
  with the complete definition of the transformOptions needs to return the config to the client.
  * In a transformOptions definition it is not allowed to use a reference to another tranformOption.

### Transformers
* **transformers** - A list of transformer definitions.
  Each transformer definition should have a unique **transformerName**,
  specify a **supportedSourceAndTargetList** and indicate which
  options it supports. As it is shown in the Tika snippet, an *engine_config*
  can describe one or more transformers, as a T-engine can have
  multiple transformers (e.g. Tika, Misc). A transformer configuration may
  specify references to 0 or more transformOptions.

### Supported Source and Target List
* **supportedSourceAndTargetList** is simply a list of source and target
  Media Types that may be transformed, optionally specifying a
  **maxSourceSizeBytes** and a **priority** value.
*  *maxSourceSizeBytes* is used to set the upper size limit of a transformation.
   * If not specified, the default value for maxSourceSizeBytes is **unlimited**.
*  *priority* it is used by clients to determine which transfomer to call or by T-engines
    with multiple transformers to determine which one to use. In the above Tika snippet,
    both *TikaAuto* and *TextMining* have the capability to transform *"application/msword"*
    into *"text/xml"*, the transformer containing the source-target media type with higher priority will be chosen by the
    T-engine as the one to execute the transformation, in this case it will be *TextMining*, because:
   * If not specified, the default value for priority is **50**.
   * Note: priority values are like the order in a queue, the **lower** the number the **higher the
    priority** is.

## Transformer selection strategy
The ACS repository will use the T-Engine configuration to choose which T-Engine will perform a transform.
A transformer definition contains a supported list of source and target Media Types. This is used for the
most basic selection. This is further refined by checking that the definition also supports transform options
(parameters) that have been supplied in a transform request or a Rendition Definition used in a rendition request.
Order for selection is:
1. Source->Target Media Types
2. transformOptions
3. maxSourceSizeBytes
4. priority

#### Case 1:
```
Transformer 1 defines options: Op1, Op2
Transformer 2 defines options: Op1, Op2, Op3, Op4
```
```
Rendition provides values for options: Op2, Op3
```
If we assume both transformers support the required source and target Media Types, Transformer 2 will be selected
because it knows about all the supplied options. The definition may also specify that some options are required or grouped.

#### Case 2:
```
Transformer 1 defines options: Op1, Op2, maxSize
Transformer 2 defines options: Op1, Op2, Op3
```
```
Rendition provides values for options: Op1, Op2
```
If we assume both transformers support the required source and target Media Types, and file size is greater than *maxSize*
,Transformer 2 will be selected because if can handle *maxSourceSizeBytes* for this transformation.

#### Case 3:
```
Transformer 1 defines options: Op1, Op2, priorty1
Transformer 2 defines options: Op1, Op2, Op3, priority2
```
```
Rendition provides values for options: Op1, Op2
```
If we assume both transformers support the required source and target Media Types and
 *priority1* < *priority2*, Transformer 1 will be selected because its priority is higher.

