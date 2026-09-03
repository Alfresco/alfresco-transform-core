# Image provides a container in which to run GraphicsMagick transformations for Alfresco Content Services.
#
# PRODMAN-916 / ACS-12649 PoC: this engine has been switched from the custom (capability-reduced)
# ImageMagick build to GraphicsMagick, built here FROM SOURCE with a full delegate set, including:
#   * JP2 / JPEG-2000 support via JasPer. jasper-devel is NOT packaged for RHEL/Rocky 9 (JasPer was
#     dropped from RHEL 9 / EPEL 9), so JasPer is also built from source here. GraphicsMagick 1.3.48
#     targets the modern JasPer API (jas_init_library, JasPer 3.0.0+) and links cleanly against 4.x.
#
#   * PSD / PDD (Adobe Photoshop) support via --enable-broken-coders PLUS a small source patch
#     applied below. GraphicsMagick moved its PSD coder into the "broken coders" set in 1.3.24
#     (2016) after a run of AFL fuzzing bugs, and has not maintained it since. In 1.3.47 two
#     internal macros gained a parameter (VerifyColormapIndex 2->3 args,
#     MagickFreeResourceLimitedMemory 1->2 args) but coders/psd.c was never updated, so on
#     1.3.47+ --enable-broken-coders alone FAILS TO COMPILE. Upstream CI never builds this file
#     because the flag is off by default, so the breakage is still unreported/unfixed at HEAD.
#     The sed patch below fixes the 8 stale call sites; 1.3.46 (the last release whose psd.c
#     matched its own macros) was used to confirm the coder logic itself still works.
#
# !! SECURITY CAVEAT - PSD is enabled via --enable-broken-coders. GraphicsMagick deliberately
# !! classifies this coder as broken and security-sensitive: it was disabled after AFL-fuzzing
# !! defects, it carries CVE-2014-1947 (stack overflow in WritePSDImage, CVSS 7.8), it has NO
# !! upstream test coverage, and no mainstream distro ships it enabled. It parses untrusted
# !! user uploads. Enabling it restores the PSD/PDD conversion capability the ATS config
# !! declares; it does NOT provide complete or hardened Photoshop compatibility.
#
# WMF is disabled (--without-wmf) as libwmf is not available on the el9 base.
#
# GraphicsMagick is from the GraphicsMagick Group. See http://www.graphicsmagick.org/ .
# JasPer is from Michael D. Adams / the JasPer project. See https://github.com/jasper-software/jasper .

# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-java
ARG JAVA_BASE_IMAGE=alfresco/alfresco-base-java:jre17-rockylinux9@sha256:f98833508b7be8c4b44a25450f9faac44cacfdc075f2295e02836b93fd05bb9c
FROM ${JAVA_BASE_IMAGE}

# Versions built from source.
ARG GRAPHICSMAGICK_VERSION=1.3.48
ARG JASPER_VERSION=4.2.9
ENV GRAPHICSMAGICK_VERSION=${GRAPHICSMAGICK_VERSION}
ENV JASPER_VERSION=${JASPER_VERSION}
ENV GRAPHICSMAGICK_SRC_URL=https://downloads.sourceforge.net/project/graphicsmagick/graphicsmagick/${GRAPHICSMAGICK_VERSION}/GraphicsMagick-${GRAPHICSMAGICK_VERSION}.tar.xz
ENV JASPER_SRC_URL=https://github.com/jasper-software/jasper/archive/refs/tags/version-${JASPER_VERSION}.tar.gz
ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG IMAGEUSERNAME=imagemagick
ARG USERID=33002

COPY target/${env.project_artifactId}-${env.project_version}.jar /usr/bin

# Build JasPer (JP2) then GraphicsMagick from source.
# NOTE: no '#' comments inside this RUN chain - a comment after a line continuation would swallow
# the rest of the command.
# NOTE: 'curl' is deliberately NOT in the dnf list. The el9 base ships curl-minimal, which already
# provides /usr/bin/curl; asking for the full 'curl' package makes dnf fail with a file conflict.
RUN ln /usr/bin/${env.project_artifactId}-${env.project_version}.jar /usr/bin/${env.project_artifactId}.jar && \
    dnf install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-9.noarch.rpm && \
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
