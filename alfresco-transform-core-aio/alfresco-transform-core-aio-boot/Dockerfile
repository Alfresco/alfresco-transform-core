# Image provides an all-in-one (AIO) container in which to run core transformations for Alfresco Content Services.

# Tika is from Apache. See the license at http://www.apache.org/licenses/LICENSE-2.0.
# LibreOffice is from The Document Foundation. See the license at https://www.libreoffice.org/download/license/ or in /libreoffice.txt.
# ImageMagick is from ImageMagick Studio LLC. See the license at http://www.imagemagick.org/script/license.php or in /ImageMagick-license.txt.
# alfresco-pdf-renderer uses the PDFium library from Google Inc. See the license at https://pdfium.googlesource.com/pdfium/+/master/LICENSE or in /pdfium.txt.

FROM alfresco/alfresco-base-java:11.0.10-openjdk-centos-8@sha256:f498c3106940e6fccc5d52e6c3e6e8ff237a3ad689bf737b2ca6fdff1a2109cc

ENV APACHE_LICENSE_FILE=https://github.com/Alfresco/acs-community-packaging/blob/master/distribution/src/main/resources/licenses/3rd-party/Apache%202.0.txt

ARG IMAGEMAGICK_VERSION=7.0.10-11
ENV IMAGEMAGICK_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/imagemagick/imagemagick-distribution/${IMAGEMAGICK_VERSION}/imagemagick-distribution-${IMAGEMAGICK_VERSION}-linux.rpm
ENV IMAGEMAGICK_LIB_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/imagemagick/imagemagick-distribution/${IMAGEMAGICK_VERSION}/imagemagick-distribution-${IMAGEMAGICK_VERSION}-libs-linux.rpm
ENV IMAGEMAGICK_DEP_RPM_URL=https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
ENV IMAGEMAGICK_LICENSE_FILE=https://github.com/Alfresco/acs-community-packaging/blob/master/distribution/src/main/resources/licenses/3rd-party/ImageMagick-license.txt

ARG LIBREOFFICE_VERSION=6.3.5
ENV LIBREOFFICE_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/libreoffice/libreoffice-dist/${LIBREOFFICE_VERSION}/libreoffice-dist-${LIBREOFFICE_VERSION}-linux.gz
ENV LIBREOFFICE_LICENSE_FILE=https://github.com/Alfresco/acs-community-packaging/blob/master/distribution/src/main/resources/licenses/3rd-party/libreoffice.txt

ENV ALFRESCO_PDF_RENDERER_LIB_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/releases/content/org/alfresco/alfresco-pdf-renderer/1.1/alfresco-pdf-renderer-1.1-linux.tgz
ENV PDFIUM_LICENSE_FILE=https://github.com/Alfresco/acs-community-packaging/blob/master/distribution/src/main/resources/licenses/3rd-party/pdfium.txt

ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG AIOUSERNAME=transform-all-in-one
ARG USERID=33017

COPY target/alfresco-transform-core-aio-boot-${env.project_version}.jar /usr/bin

RUN ln /usr/bin/alfresco-transform-core-aio-boot-${env.project_version}.jar /usr/bin/alfresco-transform-core-aio-boot.jar && \
    curl -s -S $IMAGEMAGICK_RPM_URL      -o imagemagick-distribution-linux.rpm && \
    curl -s -S $IMAGEMAGICK_LIB_RPM_URL  -o imagemagick-distribution-libs-linux.rpm && \
    curl -s -S $IMAGEMAGICK_LICENSE_FILE -o ImageMagick-license.txt && \
    curl -s -S $IMAGEMAGICK_DEP_RPM_URL  -o imagemagick-epel-dep.rpm && \
    yum localinstall -y imagemagick-epel-dep.rpm && \
    yum localinstall -y imagemagick-distribution-*linux.rpm && \
    rm -f imagemagick-distribution-*.rpm && \
    yum clean all  && \
    yum install -y cairo cups-libs libSM && \
    test -f libreoffice-dist-${LIBREOFFICE_VERSION}-linux.gz && \
    ln -s libreoffice-dist-${LIBREOFFICE_VERSION}-linux.gz libreoffice-dist-linux.gz || \
    curl -s -S $LIBREOFFICE_RPM_URL      -o libreoffice-dist-linux.gz && \
    curl -s -S $LIBREOFFICE_LICENSE_FILE -o libreoffice.txt && \
    tar xzf libreoffice-dist-linux.gz && \
    yum localinstall -y LibreOffice*/RPMS/*.rpm && \
    rm -rf libreoffice-dist-*linux.gz LibreOffice_*_Linux_x86-64_rpm && \
    yum clean all && \
    curl -s -S $ALFRESCO_PDF_RENDERER_LIB_RPM_URL -o alfresco-pdf-renderer-linux.tgz && \
    curl -s -S $PDFIUM_LICENSE_FILE               -o pdfium.txt && \
    tar xf alfresco-pdf-renderer-linux.tgz -C /usr/bin && \
	rm -f alfresco-pdf-renderer-linux.tgz && \
    yum clean all && \
    curl -s -S $APACHE_LICENSE_FILE -o Apache\ 2.0.txt && \
    yum clean all

ADD target/generated-resources/licenses              /licenses
ADD target/generated-resources/licenses.xml          /licenses/
ADD target/generated-sources/license/THIRD-PARTY.txt /licenses/

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${AIOUSERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/alfresco-transform-core-aio-boot.jar

EXPOSE 8090

USER ${AIOUSERNAME}

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/alfresco-transform-core-aio-boot.jar
