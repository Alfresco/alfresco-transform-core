e# Image provides a container in which to run ImageMagick transformations for Alfresco Content Services.

FROM quay.io/alfresco/alfresco-base-java:9

ENV IMAGEMAGICK_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/imagemagick/imagemagick-distribution/7.0.7-27/imagemagick-distribution-7.0.7-27-linux.rpm
ENV IMAGEMAGICK_LIB_RPM_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/imagemagick/imagemagick-distribution/7.0.7-27/imagemagick-distribution-7.0.7-27-libs-linux.rpm

COPY target/alfresco-docker-imagemagick-${env.project_version}.jar /usr/bin

RUN ln /usr/bin/alfresco-docker-imagemagick-${env.project_version}.jar /usr/bin/alfresco-docker-imagemagick.jar && \
    yum install -y wget && \
    wget $IMAGEMAGICK_RPM_URL && \
    wget $IMAGEMAGICK_LIB_RPM_URL && \
    yum localinstall -y ImageMagick-*.x86_64.rpm && \
    rm -f ImageMagick-*.x86_64.rpm && \
    yum remove -y wget && \
    yum clean all

EXPOSE 8090

ENTRYPOINT java -jar /usr/bin/alfresco-docker-imagemagick.jar
