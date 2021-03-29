# Image provides a container in which to run FFmpeg transformations for Alfresco Content Services.

# TODO PoC for FFmpeg
# The FFmpeg transformer uses FFmpeg. See license: https://github.com/FFmpeg/FFmpeg/blob/master/LICENSE.md

FROM alfresco/alfresco-base-java:11.0.7-openjdk-centos-7-9448dd21de19
#FROM alfresco/alfresco-base-java:11.0.10-openjdk-centos-8@sha256:343c8f63cf80c7af51785b93d6972b0c00087a1c0b995098cb8285c4d9db74b5

ARG FFMPEG_VERSION=3.4.8-1

# TODO PoC for FFmpeg - update & download from Nexus
ENV FFMPEG_RPM_URL=https://download1.rpmfusion.org/free/el/updates/7/x86_64/f/ffmpeg-${FFMPEG_VERSION}.el7.x86_64.rpm
#ENV FFMPEG_LICENSE_FILE=https://github.com/Alfresco/acs-community-packaging/blob/master/distribution/src/main/resources/licenses/3rd-party/TODO-license.txt
ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000

# TODO PoC for FFmpeg - update w3 page with a specific allocated id in sequence (may not be 33099)
ARG FFMPEGUSERNAME=ffmpeg
ARG USERID=33099

COPY target/${env.project_artifactId}-${env.project_version}.jar /usr/bin

RUN ln /usr/bin/${env.project_artifactId}-${env.project_version}.jar /usr/bin/${env.project_artifactId}.jar && \
#    TODO PoC for FFmpeg - see above
#    curl -s -S $FFMPEG_RPM_URL      -o ffmpeg-distribution-linux.rpm && \
#    curl -s -S $FFMPEG_LICENSE_FILE -o FFmpeg-license.txt && \
    yum install -y epel-release && \
#    yum localinstall -y ffmpeg-distribution-*linux.rpm && \
    yum localinstall -y --nogpgcheck https://download1.rpmfusion.org/free/el/rpmfusion-free-release-7.noarch.rpm && \
    yum install -y ffmpeg && \
#    rm -f ffmpeg-distribution-*.rpm && \
    yum clean all

ADD target/generated-resources/licenses              /licenses
ADD target/generated-resources/licenses.xml          /licenses/
ADD target/generated-sources/license/THIRD-PARTY.txt /licenses/

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${FFMPEGUSERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/${env.project_artifactId}.jar

EXPOSE 8090

USER ${PDFUSERNAME}

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/${env.project_artifactId}.jar

