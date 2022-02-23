#!/bin/bash

set -e

IMAGEMAGICK_VERSION=7.1.0-16

#Installing SRPM package. For future proof builds the SRPM package is uploaded to Alfresco's nexus repository.
rpm -i https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/imagemagick/imagemagick-distribution/$IMAGEMAGICK_VERSION/imagemagick-distribution-$IMAGEMAGICK_VERSION-src.rpm

#liblqr is not available on CentOS 7. Removing it from the spec.
sed -i '/lqr/d' ~/rpmbuild/SPECS/ImageMagick.spec

#We need some libraries from the epel repo.
yum -y install https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm

#We are going to compile the imagemagick so we need development tools.
yum -y group install "Development Tools"

#We are building the imagemagick from the official SRPM package. It allows us to be as compatible with the official RPM packages as possible.
#  Installing direct build time dependencies (excluding liblqr-1-devel)
yum -y install bzip2-devel freetype-devel libjpeg-devel libpng-devel libtiff-devel giflib-devel \
    zlib-devel perl-devel perl-generators ghostscript-devel djvulibre-devel libwmf-devel jasper-devel \
    libtool-ltdl-devel libX11-devel libXext-devel libXt-devel lcms2-devel libxml2-devel librsvg2-devel \
    fftw-devel ilmbase-devel OpenEXR-devel libwebp-devel jbigkit-devel openjpeg2-devel graphviz-devel \
    libraqm-devel LibRaw-devel 
#  Installing indirect dependencies
#    ImageMagick tests requite non default fonts  
yum groupinstall -y "fonts"
#    ImageMagick spec requires ldconfig_scriptlets macro
yum -y install epel-rpm-macros

#Building binary RPM packages
rpmbuild -bb ~/rpmbuild/SPECS/ImageMagick.spec

#Copy RPMs to well known directory.
mkdir -p /opt/alfresco/imagemagick/rpms
cp ~/rpmbuild/RPMS/x86_64/ImageMagick-$IMAGEMAGICK_VERSION.x86_64.rpm /opt/alfresco/imagemagick/rpms/ImageMagick-$IMAGEMAGICK_VERSION.x86_64.centos7.rpm
cp ~/rpmbuild/RPMS/x86_64/ImageMagick-libs-$IMAGEMAGICK_VERSION.x86_64.rpm /opt/alfresco/imagemagick/rpms/ImageMagick-libs-$IMAGEMAGICK_VERSION.x86_64.centos7.rpm
