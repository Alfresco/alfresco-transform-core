# T-engine properties can be configured externally

In order to configure an external property it needs to be set as ENV property.

The following externalized T-engines properties are available:

## Tika

| Property | Description                                                                                          | Default value |
|----------|------------------------------------------------------------------------------------------------------|---------------|
| SERVER_PORT | T-Engine Port.                                                                                       | 8090 |
| HOSTNAME | T-Engine Name.                                                                                       | t-engine |
| ACTIVEMQ_URL | ActiveMQ URL.                                                                                        | nio://localhost:61616 |
| ACTIVEMQ_USER | ActiveMQ User.                                                                                       | admin |
| ACTIVEMQ_PASSWORD | ActiveMQ Password.                                                                                   | admin |
| ACTIVEMQ_URL_PARAMS | ActiveMQ connection options.                                                                         | ?jms.watchTopicAdvisories=false |
| FILE_STORE_URL | T-Engine Port.                                                                                       | <http://localhost:8099/alfresco/api/-default-/private/sfs/versions/1/file> |
| TEST_ENDPOINT_ENABLED | Enable /Disable **/test** endpoint                                                                   | false
| PDFBOX_NOTEXTRACTBOOKMARKS_DEFAULT | The default behaviour for notExtractBookmarksText when this request param is omitted from a request. | false |
| TRANSFORM_ENGINE_REQUEST_QUEUE | T-Engine queue used for receiving async requests.                                                    | org.alfresco.transform.engine.tika.acs |

## Pdf-renderer

| Property | Description | Default value |
|----------|-------------|---------------|
| SERVER_PORT | T-Engine Port | 8090 |
| HOSTNAME | T-Engine Name. | t-engine |
| ACTIVEMQ_URL | ActiveMQ URL. | nio://localhost:61616 |
| ACTIVEMQ_USER | ActiveMQ User. | admin |
| ACTIVEMQ_PASSWORD | ActiveMQ Password. | admin |
| ACTIVEMQ_URL_PARAMS | ActiveMQ connection options. | ?jms.watchTopicAdvisories=false |
| FILE_STORE_URL | T-Engine Port. | <http://localhost:8099/alfresco/api/-default-/private/sfs/versions/1/file> |
| TEST_ENDPOINT_ENABLED | Enable /Disable **/test** endpoint                                                                   | false
| TRANSFORM_ENGINE_REQUEST_QUEUE | T-Engine queue used for async requests. | org.alfresco.transform.engine.alfresco-pdf-renderer.acs |
| PDFRENDERER_EXE | Path to Pdf-renderer EXE. | /usr/bin/alfresco-pdf-renderer |

## Misc

| Property | Description | Default value |
|----------|-------------|---------------|
| SERVER_PORT | T-Engine Port | 8090 |
| HOSTNAME | T-Engine Name. | t-engine |
| ACTIVEMQ_URL | ActiveMQ URL. | nio://localhost:61616 |
| ACTIVEMQ_USER | ActiveMQ User. | admin |
| ACTIVEMQ_PASSWORD | ActiveMQ Password. | admin |
| ACTIVEMQ_URL_PARAMS | ActiveMQ connection options. | ?jms.watchTopicAdvisories=false |
| FILE_STORE_URL | T-Engine Port. | <http://localhost:8099/alfresco/api/-default-/private/sfs/versions/1/file> |
| TEST_ENDPOINT_ENABLED | Enable /Disable **/test** endpoint                                                                   | false
| TRANSFORM_ENGINE_REQUEST_QUEUE | T-Engine queue used for async requests. | org.alfresco.transform.engine.misc.acs |
| MISC_PDFBOX_DEFAULT_FONT | Default font used by PdfBox | NotoSans-Regular |
| MISC_HTML_COLLAPSE | Html Collasping Option for HTML to TXT transformation | true |

## Libreoffice

| Property | Description | Default value                                                            |
|----------|-------------|--------------------------------------------------------------------------|
| SERVER_PORT | T-Engine Port | 8090                                                                     |
| HOSTNAME | T-Engine Name. | t-engine                                                                 |
| ACTIVEMQ_URL | ActiveMQ URL. | nio://localhost:61616                                                    |
| ACTIVEMQ_USER | ActiveMQ User. | admin                                                                    |
| ACTIVEMQ_PASSWORD | ActiveMQ Password. | admin                                                                    |
| ACTIVEMQ_URL_PARAMS | ActiveMQ connection options. | ?jms.watchTopicAdvisories=false                                          |
| FILE_STORE_URL | T-Engine Port. | <http://localhost:8099/alfresco/api/-default-/private/sfs/versions/1/file> |
| TEST_ENDPOINT_ENABLED | Enable /Disable **/test** endpoint                                                                   | false
| TRANSFORM_ENGINE_REQUEST_QUEUE | T-Engine queue used for async requests. | org.alfresco.transform.engine.libreoffice.acs                            |
| LIBREOFFICE_HOME | Path to LibreOffice_Home.  | /opt/libreoffice7.2                                                      |
| LIBREOFFICE_MAX_TASKS_PER_PROCESS | Number of maximum tasks per process. | 200                                                                      |
| LIBREOFFICE_TIMEOUT | Timeout value for LibreOffice `execution timeout`, `queue timeout` and `connection timeout`. | 1200000                                                                  |
| LIBREOFFICE_PORT_NUMBERS | LibreOffice port. | 8100                                                                     |
| LIBREOFFICE_TEMPLATE_PROFILE_DIR | Path to user profile. | alfresco_default                                                                      |
| LIBREOFFICE_IS_ENABLED | Enables Libreoffice executioner. | true                                                                     |

## Imagemagick

| Property | Description                                                           | Default value |
|----------|-----------------------------------------------------------------------|---------------|
| SERVER_PORT | T-Engine Port                                                         | 8090 |
| HOSTNAME | T-Engine Name.                                                        | t-engine |
| ACTIVEMQ_URL | ActiveMQ URL.                                                         | nio://localhost:61616 |
| ACTIVEMQ_USER | ActiveMQ User.                                                        | admin |
| ACTIVEMQ_PASSWORD | ActiveMQ Password.                                                    | admin |
| ACTIVEMQ_URL_PARAMS | ActiveMQ connection options.                                          | ?jms.watchTopicAdvisories=false |
| FILE_STORE_URL | T-Engine Port.                                                        | <http://localhost:8099/alfresco/api/-default-/private/sfs/versions/1/file> |
| TEST_ENDPOINT_ENABLED | Enable /Disable **/test** endpoint                                                                   | false
| TRANSFORM_ENGINE_REQUEST_QUEUE | T-Engine queue used for async requests.                               | org.alfresco.transform.engine.imagemagick.acs |
| IMAGEMAGICK_ROOT | Path to Imagemagick Root.                                             | /usr/lib64/ImageMagick-7.0.10 |
| IMAGEMAGICK_DYN | Path to Imagemagick DYLD.                                             | /usr/lib64/ImageMagick-7.0.10/lib |
| IMAGEMAGICK_EXE | Path to Imagemagick EXE.                                              | /usr/bin/convert |
| IMAGEMAGICK_CODERS | Path to Imagemagick custom coders.                                    |  |
| IMAGEMAGICK_CONFIG | Path to Imagemagick custom config.                                    |  |
| IMAGEMAGICK_COMMAND_OPTIONS_ENABLED | If set to true, enables usage of deprecated commandOptions parameter. |  |

## Core-aio

| Property | Description | Default value |
|----------|-------------|---------------|
| SERVER_PORT | T-Engine Port | 8090 |
| HOSTNAME | T-Engine Name. | t-engine |
| ACTIVEMQ_URL | ActiveMQ URL. | nio://localhost:61616 |
| ACTIVEMQ_USER | ActiveMQ User. | admin |
| ACTIVEMQ_PASSWORD | ActiveMQ Password. | admin |
| ACTIVEMQ_URL_PARAMS | ActiveMQ connection options. | ?jms.watchTopicAdvisories=false |
| FILE_STORE_URL | T-Engine Port. | <http://localhost:8099/alfresco/api/-default-/private/sfs/versions/1/file> |
| TEST_ENDPOINT_ENABLED | Enable /Disable **/test** endpoint                                                                   | false
| PDFBOX_NOTEXTRACTBOOKMARKS_DEFAULT | The default behaviour for notExtractBookmarksText when this request param is omitted from a request. | false |
| TRANSFORM_ENGINE_REQUEST_QUEUE | T-Engine queue used for async requests. | org.alfresco.transform.engine.aio.acs |
| PDFRENDERER_EXE | Path to Pdf-renderer EXE. | /usr/bin/alfresco-pdf-renderer |
| TRANSFORM_ENGINE_REQUEST_QUEUE | T-Engine queue used for async requests. | org.alfresco.transform.engine.libreoffice.acs |
| LIBREOFFICE_HOME | Path to LibreOffice_Home.  | /opt/libreoffice7.2 |
| LIBREOFFICE_MAX_TASKS_PER_PROCESS | Number of maximum tasks per process. | 200 |
| LIBREOFFICE_TIMEOUT | Timeout value for LibreOffice `execution timeout`, `queue timeout` and `connection timeout`. | 1200000 |
| LIBREOFFICE_PORT_NUMBERS | LibreOffice port. | 8100 |
| LIBREOFFICE_TEMPLATE_PROFILE_DIR | Path to user profile. | alfresco_default |
| LIBREOFFICE_IS_ENABLED | Enables Libreoffice executioner. | true |
| IMAGEMAGICK_ROOT | Path to Imagemagick Root. | /usr/lib64/ImageMagick-7.0.10 |
| IMAGEMAGICK_DYN | Path to Imagemagick DYLD. | /usr/lib64/ImageMagick-7.0.10/lib |
| IMAGEMAGICK_EXE | Path to Imagemagick EXE. | /usr/bin/convert |
| IMAGEMAGICK_CODERS | Path to Imagemagick custom coders. |  |
| IMAGEMAGICK_CONFIG | Path to Imagemagick custom config. |  |
| IMAGEMAGICK_COMMAND_OPTIONS_ENABLED | If set to true, enables usage of deprecated commandOptions parameter. |  |
| MISC_PDFBOX_DEFAULT_FONT | Default font used by PdfBox | NotoSans-Regular |
| MISC_HTML_COLLAPSE | Html Collapsing Option for HTML to TXT transformation explicitly for Misc Engine | true |

## Failure classification (all T-Engines)

Every T-Engine built on the base engine decides whether a failed transform is worth retrying. A
failure caused by the document itself (corrupt, truncated, password-protected, unsupported format)
is reported as `422 Unprocessable Entity`, which the T-Router and the repository do not retry. Any
other failure stays a retryable `500`. The default lists are in
`engines/base/src/main/resources/transform-failure-classification-defaults.yaml`.

| Property | Description | Default value |
|----------|-------------|---------------|
| TRANSFORM_FAILURE_CLASSIFICATION_ENABLED | Enables failure classification. When `false`, failures the engine infers from an exception are reported as `500`, as before. Failures the engine determines by reading the document itself are still `422`. | true |
| TRANSFORM_FAILURE_CLASSIFICATION_CLASSNAMES | Exception class names (simple, qualified or a superclass) that mean the document could not be parsed. Replaces the default list. | See defaults file |
| TRANSFORM_FAILURE_CLASSIFICATION_MESSAGEFRAGMENTS | Case-insensitive message fragments that mean the document could not be parsed. Only checked on the types in `MESSAGEFRAGMENTCLASSNAMES`. Replaces the default list. | See defaults file |
| TRANSFORM_FAILURE_CLASSIFICATION_MESSAGEFRAGMENTCLASSNAMES | Exception types whose message is checked against `MESSAGEFRAGMENTS`. Replaces the default list. | IOException, SAXException, TikaException |
| TRANSFORM_FAILURE_CLASSIFICATION_INFRASTRUCTURECLASSNAMES | Exception class names that always mean an engine or environment failure. A match anywhere in the cause chain forces `500`. Replaces the default list. | See defaults file |
| TRANSFORM_FAILURE_CLASSIFICATION_INFRASTRUCTUREMESSAGEFRAGMENTS | Case-insensitive message fragments that always mean an engine or environment failure, checked on any exception type. Replaces the default list. | See defaults file |
| `TRANSFORM_FAILURE_CLASSIFICATION_CONTRIBUTIONS_<NAME>_<LIST>` | Adds entries to a list instead of replacing it, e.g. `TRANSFORM_FAILURE_CLASSIFICATION_CONTRIBUTIONS_SITE_CLASSNAMES=MyParserException`. `<NAME>` is any label; `<LIST>` is one of the list names above without the prefix. | |

Lists are comma separated, or set one entry per variable with an index, e.g.
`TRANSFORM_FAILURE_CLASSIFICATION_CLASSNAMES_0_=ZipException`. The same settings can be given as
Spring properties under `transform.failure.classification` (`enabled`, `class-names`,
`message-fragments`, `message-fragment-class-names`, `infrastructure-class-names`,
`infrastructure-message-fragments`, `contributions.<name>.<list>`).
