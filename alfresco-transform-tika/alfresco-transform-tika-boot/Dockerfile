# Image provides a container in which to run Tika transformations for Alfresco Content Services.

# Tika is from Apache. See the license at http://www.apache.org/licenses/LICENSE-2.0.

FROM alfresco/alfresco-base-java:11.0.10-openjdk-centos-8@sha256:7d4177162080b8f18b3c7b9f59fb86a3c74c8e82aa329ab4e3878d660bc16195

ARG EXIFTOOL_VERSION=12.25
ARG EXIFTOOL_FOLDER=Image-ExifTool-${EXIFTOOL_VERSION}
ARG EXIFTOOL_URL=https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/exiftool/image-exiftool/${EXIFTOOL_VERSION}/image-exiftool-${EXIFTOOL_VERSION}.tgz

ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG TIKAUSERNAME=tika
ARG USERID=33004

COPY target/${env.project_artifactId}-${env.project_version}.jar /usr/bin

RUN ln /usr/bin/${env.project_artifactId}-${env.project_version}.jar /usr/bin/${env.project_artifactId}.jar && \
    curl -s -S $EXIFTOOL_URL -o ${EXIFTOOL_FOLDER}.tgz && \
    tar xzf ${EXIFTOOL_FOLDER}.tgz && \
    yum -y install perl && \
    (cd ./${EXIFTOOL_FOLDER} && \
    perl Makefile.PL && \
    make && \
    make test && \
    make install) && \
    yum clean all

ADD target/generated-resources/licenses              /licenses
ADD target/generated-resources/licenses.xml          /licenses/
ADD target/generated-sources/license/THIRD-PARTY.txt /licenses/
COPY src/main/resources/licenses/3rd-party/ /

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${TIKAUSERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/${env.project_artifactId}.jar

EXPOSE 8090

USER ${TIKAUSERNAME}

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/${env.project_artifactId}.jar
