# Image provides an all-in-one (AIO) container in which to run core transformations for Alfresco Content Services.

# Tika is from Apache. See the license at http://www.apache.org/licenses/LICENSE-2.0.
# LibreOffice is from The Document Foundation. See the license at https://www.libreoffice.org/download/license/ or in /libreoffice.txt.
# ImageMagick is from ImageMagick Studio LLC. See the license at http://www.imagemagick.org/script/license.php or in /ImageMagick-license.txt.
# alfresco-pdf-renderer uses the PDFium library from Google Inc. See the license at https://pdfium.googlesource.com/pdfium/+/master/LICENSE or in /pdfium.txt.

FROM alfresco/alfresco-base-java:jre17-rockylinux8-202301041056

ARG EXIFTOOL_VERSION=12.25
ARG EXIFTOOL_FOLDER=Image-ExifTool-${EXIFTOOL_VERSION}
ARG EXIFTOOL_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/exiftool/image-exiftool/${EXIFTOOL_VERSION}/image-exiftool-${EXIFTOOL_VERSION}.tgz

ARG IMAGEMAGICK_VERSION=7.1.0-16
ENV IMAGEMAGICK_RPM_URL=https://github.com/Alfresco/imagemagick-build/releases/download/v${IMAGEMAGICK_VERSION}/ImageMagick-${IMAGEMAGICK_VERSION}.x86_64.rpm
ENV IMAGEMAGICK_LIB_RPM_URL=https://github.com/Alfresco/imagemagick-build/releases/download/v${IMAGEMAGICK_VERSION}/ImageMagick-libs-${IMAGEMAGICK_VERSION}.x86_64.rpm
ENV IMAGEMAGICK_DEP_RPM_URL=https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm

ARG LIBREOFFICE_VERSION=7.2.5
ENV LIBREOFFICE_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/libreoffice/libreoffice-dist/${LIBREOFFICE_VERSION}/libreoffice-dist-${LIBREOFFICE_VERSION}-linux.gz

ENV ALFRESCO_PDF_RENDERER_LIB_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/releases/content/org/alfresco/alfresco-pdf-renderer/1.1/alfresco-pdf-renderer-1.1-linux.tgz

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
    yum install -y $IMAGEMAGICK_LIB_RPM_URL $IMAGEMAGICK_RPM_URL && \
    rpm -e --nodeps libgs && \
    yum install -y cairo cups-libs libSM libGLU && \
    test -f libreoffice-dist-${LIBREOFFICE_VERSION}-linux.gz && \
    ln -s libreoffice-dist-${LIBREOFFICE_VERSION}-linux.gz libreoffice-dist-linux.gz || \
    curl -s -S $LIBREOFFICE_RPM_URL      -o libreoffice-dist-linux.gz && \
    tar xzf libreoffice-dist-linux.gz && \
    yum localinstall -y LibreOffice*/RPMS/*.rpm && \
    rm -rf libreoffice-dist-*linux.gz LibreOffice_*_Linux_x86-64_rpm && \
    curl -s -S $ALFRESCO_PDF_RENDERER_LIB_RPM_URL -o alfresco-pdf-renderer-linux.tgz && \
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

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${AIOUSERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/${env.project_artifactId}.jar

EXPOSE 8090

USER ${AIOUSERNAME}

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/${env.project_artifactId}.jar
