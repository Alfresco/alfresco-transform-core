# Image provides a container in which to run ImageMagick transformations for Alfresco Enterprise Content Services.

# The container is only intended to be used with the Alfresco Enterprise editon which is covered by https://www.alfresco.com/legal/agreements and https://www.alfresco.com/terms-use.
# ImageMagick is from ImageMagick Studio LLC. See the license at http://www.imagemagick.org/script/license.php or in /ImageMagick-license.txt.

FROM alfresco/alfresco-base-java:11-openjdk-centos-7-95536c8b599f

ENV IMAGEMAGICK_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/imagemagick/imagemagick-distribution/7.0.7-27/imagemagick-distribution-7.0.7-27-linux.rpm
ENV IMAGEMAGICK_LIB_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/imagemagick/imagemagick-distribution/7.0.7-27/imagemagick-distribution-7.0.7-27-libs-linux.rpm
ENV IMAGEMAGICK_LICENSE_FILE=https://github.com/Alfresco/acs-community-packaging/blob/master/distribution/src/main/resources/licenses/3rd-party/ImageMagick-license.txt
ENV JAVA_OPTS=""

# Image comparasion utils
RUN yum -y install epel-release && \
    curl --silent --location https://rpm.nodesource.com/setup_8.x | bash -  && \
    yum -y install nodejs && \
    yum -y install gcc-c++ make && \
    yum -y install libpng12 && \
    node -v && \
    npm -v && \
    #https://www.npmjs.com/package/mz
    npm i mz && \
    #https://www.npmjs.com/package/resemblejs
    npm i resemblejs && \
    mkdir /cmp

# Image comparasion file
COPY target/compare.js /cmp
COPY target/red.png /cmp
COPY target/blue.png /cmp

COPY target/alfresco-docker-imagemagick-2.0.0-RC2-SNAPSHOT.jar /usr/bin

RUN ln /usr/bin/alfresco-docker-imagemagick-2.0.0-RC2-SNAPSHOT.jar /usr/bin/alfresco-docker-imagemagick.jar && \
    yum install -y wget && \
    wget $IMAGEMAGICK_RPM_URL && \
    wget $IMAGEMAGICK_LIB_RPM_URL && \
    wget $IMAGEMAGICK_LICENSE_FILE && \
    yum localinstall -y imagemagick-distribution-*-linux.rpm && \
    rm -f imagemagick-distribution-*-linux.rpm && \
    yum remove -y wget && \
    yum clean all

EXPOSE 8090

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/alfresco-docker-imagemagick.jar
