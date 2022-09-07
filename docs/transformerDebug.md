# TransformerDebug

In addition to any normal logging, the t-engines, t-router and t-client also
use the `TransformerDebug` class to provide request based logging. The
following is an example from Alfresco after the upload of a `docx` file.

~~~text
163               docx json AGM 2016 - Masters report.docx 14.8 KB -- metadataExtract --  TransformService
163               workspace://SpacesStore/0db3a665-328d-4437-85ed-56b753cf19c8 1563306426
163               docx json  14.8 KB -- metadataExtract -- PoiMetadataExtractor
163                 cm:title=
163                 cm:author=James Dobinson
163               Finished in 664 ms
...
164               docx png  AGM 2016 - Masters report.docx 14.8 KB -- doclib --  TransformService
164               workspace://SpacesStore/0db3a665-328d-4437-85ed-56b753cf19c8 1563306426
164               docx png   14.8 KB -- doclib -- officeToImageViaPdf
164.1             docx pdf   libreoffice
164.2             pdf  png   pdfToImageViaPng
164.2.1           pdf  png   pdfrenderer
164.2.2           png  png   imagemagick
164.2.2             endPage="0"
164.2.2             resizeHeight="100"
164.2.2             thumbnail="true"
164.2.2             startPage="0"
164.2.2             resizeWidth="100"
164.2.2             autoOrient="true"
164.2.2             allowEnlargement="false"
164.2.2             maintainAspectRatio="true"
164               Finished in 725 ms
~~~

This log happens to be from the t-client, but similar log lines exist in the
t-router and individual t-engines.

All lines start with a reference, which starts with the client’s request
number (`163`, `164` if known) and then a nested pipeline or failover
structure. The first request extracts metadata and the second creates a
thumbnail rendition (called `doclib`). The second request is handled by a
pipeline called `officeToImageViaPdf` which uses `libreoffice` to transform 
to `pdf` and then another pipeline to convert to `png`. The last step
(`164.2.2`) in the process resizes the `png` using a number of transform
options.

If requested, log information is passed back in the TransformReply's
clientData.
