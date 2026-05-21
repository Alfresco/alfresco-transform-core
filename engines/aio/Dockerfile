# Image provides an all-in-one (AIO) container in which to run core transformations for Alfresco Content Services.

# Tika is from Apache. See the license at http://www.apache.org/licenses/LICENSE-2.0.
# LibreOffice is from The Document Foundation. See the license at https://www.libreoffice.org/download/license/ or in /libreoffice.txt.
# ImageMagick is from ImageMagick Studio LLC. See the license at http://www.imagemagick.org/script/license.php or in /ImageMagick-license.txt.
# alfresco-pdf-renderer uses the PDFium library from Google Inc. See the license at https://pdfium.googlesource.com/pdfium/+/master/LICENSE or in /pdfium.txt.

# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-java
ARG JAVA_BASE_IMAGE=alfresco/alfresco-base-java:jre17-rockylinux9@sha256:f98833508b7be8c4b44a25450f9faac44cacfdc075f2295e02836b93fd05bb9c
FROM ${JAVA_BASE_IMAGE}

ARG EXIFTOOL_VERSION=12.25
ARG EXIFTOOL_FOLDER=Image-ExifTool-${EXIFTOOL_VERSION}
ARG EXIFTOOL_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/org/exiftool/image-exiftool/${EXIFTOOL_VERSION}/image-exiftool-${EXIFTOOL_VERSION}.tgz

ARG IMAGEMAGICK_VERSION=7.1.2-13-ci-1
ENV IMAGEMAGICK_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/org/imagemagick/imagemagick-distribution/${IMAGEMAGICK_VERSION}/imagemagick-distribution-${IMAGEMAGICK_VERSION}
ENV IMAGEMAGICK_DEP_RPM_URL=https://dl.fedoraproject.org/pub/epel/epel-release-latest-9.noarch.rpm

ARG LIBREOFFICE_VERSION=26.2.3
ARG PDF_RENDERER_VERSION=1.2
ENV ALFRESCO_PDF_RENDERER_LIB_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/releases/org/alfresco/alfresco-pdf-renderer/${PDF_RENDERER_VERSION}/alfresco-pdf-renderer-${PDF_RENDERER_VERSION}-linux.tgz
ENV ALFRESCO_PDF_RENDERER_ARM64_LIB_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/releases/org/alfresco/alfresco-pdf-renderer/${PDF_RENDERER_VERSION}/alfresco-pdf-renderer-${PDF_RENDERER_VERSION}-linux-arm.tgz

ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG AIOUSERNAME=transform-all-in-one
ARG USERID=33017

COPY target/${env.project_artifactId}-${env.project_version}.jar /usr/bin

#removing perl-ExtUtils-MakeMaker cascades to remove exiftools
RUN ln /usr/bin/${env.project_artifactId}-${env.project_version}.jar /usr/bin/${env.project_artifactId}.jar && \
    yum install -y $IMAGEMAGICK_DEP_RPM_URL && \
    if [ "$(uname -m)" = "x86_64" ]; then \
      yum install -y ${IMAGEMAGICK_RPM_URL}-el9.rpm ${IMAGEMAGICK_RPM_URL}-libs-el9.rpm; \
    else \
      yum install -y ${IMAGEMAGICK_RPM_URL}-el9-aarch64.rpm ${IMAGEMAGICK_RPM_URL}-libs-el9-aarch64.rpm; \
    fi && \
    yum install -y cairo cups-libs libSM libGLU procps && \
    ARCH=$(uname -m) && \
    if [ "$ARCH" = "x86_64" ]; then LO_ARCH="x86-64"; else LO_ARCH="aarch64"; fi && \
    curl -s -S -L "https://download.documentfoundation.org/libreoffice/stable/${LIBREOFFICE_VERSION}/rpm/${ARCH}/LibreOffice_${LIBREOFFICE_VERSION}_Linux_${LO_ARCH}_rpm.tar.gz" \
      -o libreoffice-dist-linux.tar.gz && \
    tar xzf libreoffice-dist-linux.tar.gz && \
    find LibreOffice*/RPMS -name "*.rpm" ! -name "*kde*" ! -name "*gnome*" \
      | xargs yum localinstall -y && \
    rm -rf libreoffice-dist-linux.tar.gz LibreOffice_*_Linux_* && \
    if [ "$(uname -m)" = "x86_64" ]; then \
      curl -s -S $ALFRESCO_PDF_RENDERER_LIB_RPM_URL -o alfresco-pdf-renderer-linux.tgz; \
    else \
      curl -s -S $ALFRESCO_PDF_RENDERER_ARM64_LIB_RPM_URL -o alfresco-pdf-renderer-linux.tgz; \
    fi && \
    tar xf alfresco-pdf-renderer-linux.tgz -C /usr/bin && \
    rm -f alfresco-pdf-renderer-linux.tgz && \
    curl -s -S $EXIFTOOL_URL -o ${EXIFTOOL_FOLDER}.tgz && \
    tar xzf ${EXIFTOOL_FOLDER}.tgz && \
    yum -y install perl perl-ExtUtils-MakeMaker make && \
    (cd ./${EXIFTOOL_FOLDER} && \
    perl Makefile.PL && \
    make && \
    make test && \
    make install) && \
    yum -y autoremove make &&  \
    rm -rf ${EXIFTOOL_FOLDER} ${EXIFTOOL_FOLDER}.tgz && \
    yum clean all

ADD target/generated-resources/licenses              /licenses
ADD target/generated-resources/licenses.xml          /licenses/
ADD target/generated-sources/license/THIRD-PARTY.txt /licenses/
COPY target/classes/licenses/3rd-party/ /
COPY target/classes/fonts/NotoSans /usr/local/share/fonts/NotoSans

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${AIOUSERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/${env.project_artifactId}.jar

EXPOSE 8090

USER ${AIOUSERNAME}

# Copy the script to set LIBREOFFICE_HOME
COPY docker-entrypoint.sh /docker-entrypoint.sh
ENTRYPOINT ["/docker-entrypoint.sh", "${env.project_artifactId}"]
