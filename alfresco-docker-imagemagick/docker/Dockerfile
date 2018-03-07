# Image provides a container in which to run ImageMagick transformations for Alfresco Content Services.

FROM quay.io/alfresco/alfresco-base-java:9

ENV IMAGEMAGICK_RPM_URL=https://www.imagemagick.org/download/linux/CentOS/x86_64/ImageMagick-7.0.7-25.x86_64.rpm
ENV IMAGEMAGICK_LIB_RPM_URL=https://www.imagemagick.org/download/linux/CentOS/x86_64/ImageMagick-libs-7.0.7-25.x86_64.rpm

COPY target/alfresco-transformer-imagemagick*.jar /usr/bin

RUN ln /usr/bin/alfresco-transformer-imagemagick*.jar /usr/bin/alfresco-transformer-imagemagick.jar && \
    yum install -y wget && \
    wget $IMAGEMAGICK_RPM_URL && \
    wget $IMAGEMAGICK_LIB_RPM_URL && \
    yum localinstall -y ImageMagick-*.x86_64.rpm && \
    rm -f ImageMagick-*.x86_64.rpm && \
    yum remove -y wget && \
    yum clean all

EXPOSE 8090

ENTRYPOINT java -jar /usr/bin/alfresco-transformer-imagemagick.jar
