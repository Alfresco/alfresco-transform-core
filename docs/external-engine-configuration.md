# T-engine properties can be configured externally

In order to configure an external property it needs to be set as ENV property.

The following externalized T-engines properties are available:

## Tika
| Property | Description | Default value |
|----------|-------------|---------------|
| SERVER_PORT | T-Engine Port. | 8090 |
| HOSTNAME | T-Engine Name. | t-engine |
| ACTIVEMQ_URL | ActiveMQ URL. | nio://localhost:61616 |
| ACTIVEMQ_USER | ActiveMQ User. | admin |
| ACTIVEMQ_PASSWORD | ActiveMQ Password. | admin |
| FILE_STORE_URL | T-Engine Port. | http://localhost:8099/alfresco/api/-default-/private/sfs/versions/1/file |
| PDFBOX_NOTEXTRACTBOOKMARKS_DEFAULT | The default behaviour for notExtractBookmarksText when this request param is omitted from a request. | false |
| TRANSFORM_ENGINE_REQUEST_QUEUE | T-Engine queue used for receiving async requests. | org.alfresco.transform.engine.tika.acs |


## Pdf-renderer
| Property | Description | Default value |
|----------|-------------|---------------|
| SERVER_PORT | T-Engine Port | 8090 |
| HOSTNAME | T-Engine Name. | t-engine |
| ACTIVEMQ_URL | ActiveMQ URL. | nio://localhost:61616 |
| ACTIVEMQ_USER | ActiveMQ User. | admin |
| ACTIVEMQ_PASSWORD | ActiveMQ Password. | admin |
| FILE_STORE_URL | T-Engine Port. | http://localhost:8099/alfresco/api/-default-/private/sfs/versions/1/file |
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
| FILE_STORE_URL | T-Engine Port. | http://localhost:8099/alfresco/api/-default-/private/sfs/versions/1/file |
| TRANSFORM_ENGINE_REQUEST_QUEUE | T-Engine queue used for async requests. | org.alfresco.transform.engine.misc.acs |

## Libreoffice
| Property | Description | Default value |
|----------|-------------|---------------|
| SERVER_PORT | T-Engine Port | 8090 |
| HOSTNAME | T-Engine Name. | t-engine |
| ACTIVEMQ_URL | ActiveMQ URL. | nio://localhost:61616 |
| ACTIVEMQ_USER | ActiveMQ User. | admin |
| ACTIVEMQ_PASSWORD | ActiveMQ Password. | admin |
| FILE_STORE_URL | T-Engine Port. | http://localhost:8099/alfresco/api/-default-/private/sfs/versions/1/file |
| TRANSFORM_ENGINE_REQUEST_QUEUE | T-Engine queue used for async requests. | org.alfresco.transform.engine.libreoffice.acs |
| LIBREOFFICE_HOME | Path to LibreOffice_Home.  | /opt/libreoffice6.3 |
| LIBREOFFICE_MAX_TASKS_PER_PROCESS | Number of maximum tasks per process. | 200 |
| LIBREOFFICE_TIMEOUT | Timeout value for LibreOffice `execution timeout`, `queue timeout` and `connection timeout`. | 1200000 |
| LIBREOFFICE_PORT_NUMBERS | LibreOffice port. | 8100 |
| LIBREOFFICE_TEMPLATE_PROFILE_DIR | Path to user profile. |  |
| LIBREOFFICE_IS_ENABLED | Enables Libreoffice executioner. | true |

## Imagemagick
| Property | Description | Default value |
|----------|-------------|---------------|
| SERVER_PORT | T-Engine Port | 8090 |
| HOSTNAME | T-Engine Name. | t-engine |
| ACTIVEMQ_URL | ActiveMQ URL. | nio://localhost:61616 |
| ACTIVEMQ_USER | ActiveMQ User. | admin |
| ACTIVEMQ_PASSWORD | ActiveMQ Password. | admin |
| FILE_STORE_URL | T-Engine Port. | http://localhost:8099/alfresco/api/-default-/private/sfs/versions/1/file |
| TRANSFORM_ENGINE_REQUEST_QUEUE | T-Engine queue used for async requests. | org.alfresco.transform.engine.imagemagick.acs |
| IMAGEMAGICK_ROOT | Path to Imagemagick Root. | /usr/lib64/ImageMagick-7.0.10 |
| IMAGEMAGICK_DYN | Path to Imagemagick DYLD. | /usr/lib64/ImageMagick-7.0.10/lib |
| IMAGEMAGICK_EXE | Path to Imagemagick EXE. | /usr/bin/convert |
| IMAGEMAGICK_CODERS | Path to Imagemagick custom coders. |  |
| IMAGEMAGICK_CONFIG | Path to Imagemagick custom config. |  |

## Core-aio
| Property | Description | Default value |
|----------|-------------|---------------|
| SERVER_PORT | T-Engine Port | 8090 |
| HOSTNAME | T-Engine Name. | t-engine |
| ACTIVEMQ_URL | ActiveMQ URL. | nio://localhost:61616 |
| ACTIVEMQ_USER | ActiveMQ User. | admin |
| ACTIVEMQ_PASSWORD | ActiveMQ Password. | admin |
| FILE_STORE_URL | T-Engine Port. | http://localhost:8099/alfresco/api/-default-/private/sfs/versions/1/file |
| PDFBOX_NOTEXTRACTBOOKMARKS_DEFAULT | The default behaviour for notExtractBookmarksText when this request param is omitted from a request. | false |
| TRANSFORM_ENGINE_REQUEST_QUEUE | T-Engine queue used for async requests. | org.alfresco.transform.engine.aio.acs |
| PDFRENDERER_EXE | Path to Pdf-renderer EXE. | /usr/bin/alfresco-pdf-renderer |
| TRANSFORM_ENGINE_REQUEST_QUEUE | T-Engine queue used for async requests. | org.alfresco.transform.engine.libreoffice.acs |
| LIBREOFFICE_HOME | Path to LibreOffice_Home.  | /opt/libreoffice6.3 |
| LIBREOFFICE_MAX_TASKS_PER_PROCESS | Number of maximum tasks per process. | 200 |
| LIBREOFFICE_TIMEOUT | Timeout value for LibreOffice `execution timeout`, `queue timeout` and `connection timeout`. | 1200000 |
| LIBREOFFICE_PORT_NUMBERS | LibreOffice port. | 8100 |
| LIBREOFFICE_TEMPLATE_PROFILE_DIR | Path to user profile. |  |
| LIBREOFFICE_IS_ENABLED | Enables Libreoffice executioner. | true |
| IMAGEMAGICK_ROOT | Path to Imagemagick Root. | /usr/lib64/ImageMagick-7.0.10 |
| IMAGEMAGICK_DYN | Path to Imagemagick DYLD. | /usr/lib64/ImageMagick-7.0.10/lib |
| IMAGEMAGICK_EXE | Path to Imagemagick EXE. | /usr/bin/convert |
| IMAGEMAGICK_CODERS | Path to Imagemagick custom coders. |  |
| IMAGEMAGICK_CONFIG | Path to Imagemagick custom config. |  |