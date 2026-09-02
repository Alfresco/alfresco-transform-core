# Image provides a container in which to run GraphicsMagick transformations for Alfresco Content Services.
#
# PRODMAN-916 / ACS-12649 PoC: this engine has been switched from the custom (capability-reduced)
# ImageMagick build to GraphicsMagick, built here FROM SOURCE with a full delegate set, including:
#   * JP2 / JPEG-2000 support via JasPer (--with-jp2, jasper-devel)
#   * PSD (Adobe Photoshop) support via --enable-broken-coders
# Building from source (rather than the stock EPEL package) gives GraphicsMagick more capability than
# the distribution package: it adds the PSD coder, which distributions disable by default since
# GraphicsMagick 1.3.24. PDD (Adobe, PSD-structured) is handled by the same PSD coder via content
# (magic-byte) detection.
#
# GraphicsMagick is from the GraphicsMagick Group. See http://www.graphicsmagick.org/ .

# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-java
ARG JAVA_BASE_IMAGE=alfresco/alfresco-base-java:jre17-rockylinux9@sha256:f98833508b7be8c4b44a25450f9faac44cacfdc075f2295e02836b93fd05bb9c
FROM ${JAVA_BASE_IMAGE}

# GraphicsMagick version to build from source.
ARG GRAPHICSMAGICK_VERSION=1.3.48
ENV GRAPHICSMAGICK_SRC_URL=https://downloads.sourceforge.net/project/graphicsmagick/graphicsmagick/${GRAPHICSMAGICK_VERSION}/GraphicsMagick-${GRAPHICSMAGICK_VERSION}.tar.xz
ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG IMAGEUSERNAME=imagemagick
ARG USERID=33002

COPY target/${env.project_artifactId}-${env.project_version}.jar /usr/bin

# Build GraphicsMagick from source with a full delegate set + PSD support (--enable-broken-coders).
RUN ln /usr/bin/${env.project_artifactId}-${env.project_version}.jar /usr/bin/${env.project_artifactId}.jar && \
    dnf install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-9.noarch.rpm && \
    dnf install -y dnf-plugins-core && \
    (dnf config-manager --set-enabled crb || true) && \
    dnf groupinstall -y "Development Tools" && \
    dnf install -y \
        gcc gcc-c++ make tar xz curl diffutils \
        libjpeg-turbo-devel libpng-devel libtiff-devel libwebp-devel \
        freetype-devel libxml2-devel lcms2-devel bzip2-devel zlib-devel \
        jasper-devel && \
    cd /tmp && \
    curl -fSL --retry 3 --retry-delay 5 "${GRAPHICSMAGICK_SRC_URL}" -o GraphicsMagick-${GRAPHICSMAGICK_VERSION}.tar.xz && \
    tar -xJf GraphicsMagick-${GRAPHICSMAGICK_VERSION}.tar.xz && \
    cd GraphicsMagick-${GRAPHICSMAGICK_VERSION} && \
    ./configure \
        --prefix=/usr/local \
        --enable-shared \
        --enable-broken-coders \
        --with-quantum-depth=16 \
        --with-jpeg --with-png --with-tiff --with-webp \
        --with-jp2 \
        --with-ttf --with-lcms2 --with-xml \
        --with-zlib --with-bzlib && \
    make -j"$(nproc)" && \
    make install && \
    echo "/usr/local/lib" > /etc/ld.so.conf.d/graphicsmagick.conf && \
    ldconfig && \
    cd /tmp && rm -rf GraphicsMagick-${GRAPHICSMAGICK_VERSION} GraphicsMagick-${GRAPHICSMAGICK_VERSION}.tar.xz && \
    dnf clean all && rm -rf /var/cache/dnf

ADD target/generated-resources/licenses              /licenses
ADD target/generated-resources/licenses.xml          /licenses/
ADD target/generated-sources/license/THIRD-PARTY.txt /licenses/
COPY target/classes/licenses/3rd-party/ /

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${IMAGEUSERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/${env.project_artifactId}.jar

EXPOSE 8090

USER ${IMAGEUSERNAME}

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/${env.project_artifactId}.jar
