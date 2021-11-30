#!/bin/bash

set -e

#We need some libraries from the epel repo.
yum -y install https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm

#We are going to compile the imagemagick so we need development tools.
yum -y group install "Development Tools"

#We are building the imagemagick from the official SRPM package. It allows us to be as compatible with the official RPM packages as possible.
#We need to install all required build time dependencies.
yum -y install bzip2-devel freetype-devel libjpeg-devel libpng-devel libtiff-devel \
    giflib-devel zlib-devel perl-devel perl-generators ghostscript-devel djvulibre-devel \
    libwmf-devel libX11-devel libXext-devel libXt-devel lcms2-devel libxml2-devel librsvg2-devel \
    OpenEXR-devel fftw-devel libwebp-devel jbigkit-devel openjpeg2-devel libtool-ltdl-devel

#Building the RPMs. For future proof builds the SRPM package is uploaded to Alfresco's nexus repository.
rpmbuild --rebuild https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/imagemagick/imagemagick-distribution/7.0.10-59/imagemagick-distribution-7.0.10-59-src.rpm

#Copy RPMs to well known directory.
mkdir -p /opt/alfresco/imagemagick/rpms
cp ~/rpmbuild/RPMS/x86_64/ImageMagick-7.0.10-59.x86_64.rpm /opt/alfresco/imagemagick/rpms/ImageMagick-7.0.10-59.x86_64.centos7.rpm
cp ~/rpmbuild/RPMS/x86_64/ImageMagick-libs-7.0.10-59.x86_64.rpm /opt/alfresco/imagemagick/rpms/ImageMagick-libs-7.0.10-59.x86_64.centos7.rpm