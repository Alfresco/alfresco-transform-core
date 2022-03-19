#!/bin/bash

set -e

# TODO ATS-986 - below is an untested & incomplete example - will need fixing !

# Package zlib-devel-1.2.11-17.el8.x86_64 is already installed.
# Error: 
#  Problem: cannot install the best candidate for the job
#   - nothing provides libxml2(x86-64) = 2.9.7-12.el8_5 needed by libxml2-devel-2.9.7-12.el8_5.x86_64
# (try to add '--skip-broken' to skip uninstallable packages or '--nobest' to use not only best candidate packages)

IMAGEMAGICK_VERSION=7.1.0-16

#Installing SRPM package. For future proof builds the SRPM package is uploaded to Alfresco's nexus repository.
rpm -i https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/imagemagick/imagemagick-distribution/$IMAGEMAGICK_VERSION/imagemagick-distribution-$IMAGEMAGICK_VERSION-src.rpm

#liblqr is not available on CentOS 7. Removing it from the spec.
sed -i '/lqr/d' ~/rpmbuild/SPECS/ImageMagick.spec

#We need some libraries from the epel repo.
yum -y install https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm

#We are going to compile the imagemagick so we need development tools.
yum -y group install "Development Tools"

yum install dnf-plugins-core
yum config-manager --set-enabled powertools

#We are building the imagemagick from the official SRPM package. It allows us to be as compatible with the official RPM packages as possible.
#  Installing direct build time dependencies (excluding liblqr-1-devel)
yum -y install freetype-devel libjpeg-devel jasper-devel libpng-devel bzip2-devel libtiff-devel \
    giflib-devel zlib-devel ghostscript-devel libwmf-devel librsvg2-devel \
    libtool-ltdl-devel libX11-devel libXext-devel libXt-devel libxml2-devel OpenEXR-devel php-devel


#  Installing indirect dependencies
#    ImageMagick tests requite non default fonts  
yum groupinstall -y "fonts"
#    ImageMagick spec requires ldconfig_scriptlets macro
yum -y install epel-rpm-macros

#Building binary RPM packages
rpmbuild -bb ~/rpmbuild/SPECS/ImageMagick.spec

#Copy RPMs to well known directory.
mkdir -p /opt/alfresco/imagemagick/rpms
cp ~/rpmbuild/RPMS/x86_64/ImageMagick-$IMAGEMAGICK_VERSION.x86_64.rpm /opt/alfresco/imagemagick/rpms/ImageMagick-$IMAGEMAGICK_VERSION.x86_64.linux.rpm
cp ~/rpmbuild/RPMS/x86_64/ImageMagick-libs-$IMAGEMAGICK_VERSION.x86_64.rpm /opt/alfresco/imagemagick/rpms/ImageMagick-libs-$IMAGEMAGICK_VERSION.x86_64.linux.rpm