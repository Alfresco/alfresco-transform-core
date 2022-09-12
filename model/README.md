# alfresco-transform-model
Alfresco Transform Model - Contains the data model of json configuration files
and messages sent between clients, T-Engines and T-Router. It also contains code to
work out which transform should be used for a combination of configuration files.

## Upgrade to 3.0.0
When upgrading to 3.0.0, you will find that a number of classes in the alfresco-transform-model
have moved. Hopefully they are now located in more logical packages. Most classes will not have been
used in existing t-engines (based on the deprecated alfresco-transform-base), other than possibly for
testing. The following table identifies these moves:

| class                           | original package                           | new package                       |
|---------------------------------|--------------------------------------------|-----------------------------------|
| ExtensionService ±              | org/alfresco/transform/router              | org.alfresco.transform.common     |
| Mimetype ±                      | org/alfresco/transform/client/model        | org.alfresco.transform.common     |
| RepositoryClientData            | org/alfresco/transform/router              | org.alfresco.transform.common     |
| RequestParamMap ±               | org/alfresco/transform/client/util         | org.alfresco.transform.common     |
| TransformerDebug                | org/alfresco/transform/router              | org.alfresco.transform.common     |
|                                 |                                            |                                   |
| AbstractTransformOption         | org/alfresco/transform/client/model/config | org.alfresco.transform.config     |
| AddSupported                    | org/alfresco/transform/client/model/config | org.alfresco.transform.config     |
| CoreFunction                    | org/alfresco/transform/client/model/config | org.alfresco.transform.config     |
| CoreVersionDecorator            | org/alfresco/transform/client/model/config | org.alfresco.transform.config     |
| OverrideSupported               | org/alfresco/transform/client/model/config | org.alfresco.transform.config     |
| RemoveSupported                 | org/alfresco/transform/client/model/config | org.alfresco.transform.config     |
| SupportedDefaults               | org/alfresco/transform/client/model/config | org.alfresco.transform.config     |
| SupportedSourceAndTarget        | org/alfresco/transform/client/model/config | org.alfresco.transform.config     |
| TransformConfig                 | org/alfresco/transform/client/model/config | org.alfresco.transform.config     |
| TransformOption                 | org/alfresco/transform/client/model/config | org.alfresco.transform.config     |
| TransformOptionGroup            | org/alfresco/transform/client/model/config | org.alfresco.transform.config     |
| TransformOptionValue            | org/alfresco/transform/client/model/config | org.alfresco.transform.config     |
| TransformStep                   | org/alfresco/transform/client/model/config | org.alfresco.transform.config     |
| Transformer                     | org/alfresco/transform/client/model/config | org.alfresco.transform.config     |
| TransformerAndTypes             | org/alfresco/transform/client/model/config | org.alfresco.transform.config     |
| TransformerTypesSizeAndPriority | org/alfresco/transform/client/model/config | org.alfresco.transform.config     |
| Types                           | org/alfresco/transform/client/model/config | org.alfresco.transform.config     |
|                                 |                                            |                                   |
| TransformRequestValidator       | org/alfresco/transform/client/model        | org.alfresco.transform.messages   |
| TransformStack                  | org/alfresco/transform/router              | org.alfresco.transform.messages   |
|                                 |                                            |                                   |
| AbstractTransformRegistry       | org/alfresco/transform/client/registry     | org.alfresco.transform.registry   |
| CombinedTransformConfig         | org/alfresco/transform/client/registry     | org.alfresco.transform.registry   |
| Defaults                        | org/alfresco/transform/client/registry     | org.alfresco.transform.registry   |
| Origin                          | org/alfresco/transform/client/registry     | org.alfresco.transform.registry   |
| SupportedTransform              | org/alfresco/transform/client/registry     | org.alfresco.transform.registry   |
| TransformCache                  | org/alfresco/transform/client/registry     | org.alfresco.transform.registry   |
| TransformRegistryHelper         | org/alfresco/transform/client/registry     | org.alfresco.transform.registry   |
| TransformServiceRegistry        | org/alfresco/transform/client/registry     | org.alfresco.transform.registry   |
| TransformerAndSourceType        | org/alfresco/transform/client/registry     | org.alfresco.transform.registry   |

± Classes also have a deprecated class with the same name in the original location as they are
more likely to have been used in existing transformers.
