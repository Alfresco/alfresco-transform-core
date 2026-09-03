# Image provides an all-in-one (AIO) container in which to run core transformations for Alfresco Content Services.

# Tika is from Apache. See the license at http://www.apache.org/licenses/LICENSE-2.0.
# LibreOffice is from The Document Foundation. See the license at https://www.libreoffice.org/download/license/ or in /libreoffice.txt.
# GraphicsMagick is from the GraphicsMagick Group. See http://www.graphicsmagick.org/ .
# JasPer is from Michael D. Adams / the JasPer project. See https://github.com/jasper-software/jasper .
# alfresco-pdf-renderer uses the PDFium library from Google Inc. See the license at https://pdfium.googlesource.com/pdfium/+/master/LICENSE or in /pdfium.txt.

# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-java
ARG JAVA_BASE_IMAGE=alfresco/alfresco-base-java:jre17-rockylinux9@sha256:f98833508b7be8c4b44a25450f9faac44cacfdc075f2295e02836b93fd05bb9c
FROM ${JAVA_BASE_IMAGE}

ARG EXIFTOOL_VERSION=12.25
ARG EXIFTOOL_FOLDER=Image-ExifTool-${EXIFTOOL_VERSION}
ARG EXIFTOOL_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/org/exiftool/image-exiftool/${EXIFTOOL_VERSION}/image-exiftool-${EXIFTOOL_VERSION}.tgz

# ImageMagick has been replaced by GraphicsMagick - see engines/imagemagick/Dockerfile for the full
# rationale. The build block below is kept byte-for-byte identical to that file so the two stay in
# step; if you change one, change both.
ARG GRAPHICSMAGICK_VERSION=1.3.48
ARG JASPER_VERSION=4.2.9
ENV GRAPHICSMAGICK_VERSION=${GRAPHICSMAGICK_VERSION}
ENV JASPER_VERSION=${JASPER_VERSION}
ENV GRAPHICSMAGICK_SRC_URL=https://downloads.sourceforge.net/project/graphicsmagick/graphicsmagick/${GRAPHICSMAGICK_VERSION}/GraphicsMagick-${GRAPHICSMAGICK_VERSION}.tar.xz
ENV JASPER_SRC_URL=https://github.com/jasper-software/jasper/archive/refs/tags/version-${JASPER_VERSION}.tar.gz
ENV EPEL_RPM_URL=https://dl.fedoraproject.org/pub/epel/epel-release-latest-9.noarch.rpm

ARG LIBREOFFICE_VERSION=7.2.5
ENV LIBREOFFICE_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/org/libreoffice/libreoffice-dist/${LIBREOFFICE_VERSION}/libreoffice-dist-${LIBREOFFICE_VERSION}-linux.gz
ENV LIBREOFFICE_ARM64_RPM_URL=https://dl.rockylinux.org/pub/rocky/9/devel/aarch64/os/Packages/l/
ENV LIBREOFFICE_ARM64_RPM_VERSION=libreoffice-7.1.8.1
ARG PDF_RENDERER_VERSION=1.3.0-85
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
    dnf install -y $EPEL_RPM_URL && \
    dnf install -y dnf-plugins-core && \
    (dnf config-manager --set-enabled crb || true) && \
    dnf install -y \
        gcc gcc-c++ make cmake tar xz gzip diffutils gawk file \
        libjpeg-turbo-devel libpng-devel libtiff-devel libwebp-devel \
        freetype-devel libxml2-devel lcms2-devel bzip2-devel zlib-devel \
        libX11-devel libXext-devel libXt-devel && \
    cd /tmp && \
    curl -fSL --retry 3 --retry-delay 5 "${JASPER_SRC_URL}" -o jasper.tar.gz && \
    tar -xzf jasper.tar.gz && \
    cmake -S jasper-version-${JASPER_VERSION} -B jasper-build \
        -DCMAKE_INSTALL_PREFIX=/usr/local \
        -DCMAKE_BUILD_TYPE=Release \
        -DJAS_ENABLE_SHARED=ON \
        -DJAS_ENABLE_DOC=OFF \
        -DJAS_ENABLE_PROGRAMS=OFF \
        -DJAS_ENABLE_OPENGL=OFF \
        -DJAS_ENABLE_LIBJPEG=OFF && \
    cmake --build jasper-build --parallel "$(nproc)" && \
    cmake --install jasper-build && \
    printf '/usr/local/lib\n/usr/local/lib64\n' > /etc/ld.so.conf.d/local.conf && \
    ldconfig && \
    cd /tmp && \
    curl -fSL --retry 3 --retry-delay 5 "${GRAPHICSMAGICK_SRC_URL}" -o GraphicsMagick-${GRAPHICSMAGICK_VERSION}.tar.xz && \
    tar -xJf GraphicsMagick-${GRAPHICSMAGICK_VERSION}.tar.xz && \
    cd GraphicsMagick-${GRAPHICSMAGICK_VERSION} && \
    sed -i \
        -e 's/unsigned int colormap_index=(IndexPacket) pixel;/unsigned int colormap_index=(IndexPacket) pixel; unsigned int status=MagickPass;/' \
        -e 's/VerifyColormapIndex(image,colormap_index)/VerifyColormapIndex(status,image,colormap_index)/g' \
        -e 's/MagickFreeResourceLimitedMemory(layer_info)/MagickFreeResourceLimitedMemory(LayerInfo *,layer_info)/g' \
        -e 's/MagickFreeResourceLimitedMemory(data)/MagickFreeResourceLimitedMemory(unsigned char *,data)/g' \
        -e 's/MagickFreeResourceLimitedMemory(scanline)/MagickFreeResourceLimitedMemory(char *,scanline)/g' \
        -e 's/MagickFreeResourceLimitedMemory(pixels)/MagickFreeResourceLimitedMemory(unsigned char *,pixels)/g' \
        coders/psd.c && \
    if grep -nE 'MagickFreeResourceLimitedMemory\((layer_info|data|scanline|pixels)\)|VerifyColormapIndex\(image,' coders/psd.c ; then \
        echo "FATAL: coders/psd.c PSD patch did not apply cleanly - check GRAPHICSMAGICK_VERSION" ; exit 1 ; \
    fi && \
    ./configure \
        --prefix=/usr/local \
        --enable-shared \
        --enable-broken-coders \
        --with-quantum-depth=16 \
        --with-jpeg --with-png --with-tiff --with-webp \
        --with-jp2 \
        --with-ttf --with-lcms2 --with-xml \
        --with-zlib --with-bzlib \
        --with-x \
        --without-wmf \
        CPPFLAGS="-I/usr/local/include" \
        LDFLAGS="-L/usr/local/lib64 -L/usr/local/lib" && \
    make -j"$(nproc)" && \
    make install && \
    ldconfig && \
    /usr/local/bin/gm version && \
    /usr/local/bin/gm convert -list format | grep -qiE '^ *JP2.*rw' && \
    /usr/local/bin/gm convert -list format | grep -qiE '^ *PSD.*rw' && \
    /usr/local/bin/gm convert -list format | grep -qiE '^ *XWD.*rw' && \
    cd /tmp && rm -rf GraphicsMagick-${GRAPHICSMAGICK_VERSION} GraphicsMagick-${GRAPHICSMAGICK_VERSION}.tar.xz jasper-version-${JASPER_VERSION} jasper-build jasper.tar.gz && \
    yum install -y cairo cups-libs libSM libGLU procps && \
    if [ "$(uname -m)" = "x86_64" ]; then \
      test -f libreoffice-dist-${LIBREOFFICE_VERSION}-linux.gz && \
      ln -s libreoffice-dist-${LIBREOFFICE_VERSION}-linux.gz libreoffice-dist-linux.gz || \
      curl -s -S $LIBREOFFICE_RPM_URL      -o libreoffice-dist-linux.gz && \
      tar xzf libreoffice-dist-linux.gz && \
      yum localinstall -y LibreOffice*/RPMS/*.rpm && \
      rm -rf libreoffice-dist-*linux.gz LibreOffice_*_Linux_x86-64_rpm; \
    else \
      LIBREOFFICE_ARM64_RPM_VERSION=$(curl -s $LIBREOFFICE_ARM64_RPM_URL | grep -o "${LIBREOFFICE_ARM64_RPM_VERSION}.*" | cut -d '"' -f 1 | sort | tail -n1) && \
      dnf --enablerepo=devel install -y ${LIBREOFFICE_ARM64_RPM_URL}${LIBREOFFICE_ARM64_RPM_VERSION} && \
      update-alternatives --set java java-17-openjdk.aarch64; \
    fi && \
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
