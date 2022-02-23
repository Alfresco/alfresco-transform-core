## Additional Alfresco Models (Content Metadata)

IPTC (Photo Metadata) Standard ( https://iptc.org/standards/photo-metadata/iptc-standard/ )

Alfresco provides an IPTC content model that maps the IPTC photo metadata fields to an
Alfresco Content Model.

This IPTC content model used to be part of the Alfresco Media Management product. It is now
provided as part of the core open-source Alfresco Repository. Hence, it will be pre-configured
as part of future core Repository releases (eg. ACS 7.1.0 and related Alfresco Community release).

The latest ("master") source files can also be found here:

- https://github.com/Alfresco/alfresco-community-repo/blob/master/repository/src/main/resources/alfresco/model/iptcModel.xml
- https://github.com/Alfresco/alfresco-community-repo/tree/master/repository/src/main/resources/alfresco/messages (iptc-model*.properties)

In the meantime, for convenience, a copy of the Alfresco IPTC content model (XML + message properties)
is also provided here. These files can be configured to deploy the model into earlier versions of
ACS (eg. 7.0.0) using static bootstrap mechanism.


