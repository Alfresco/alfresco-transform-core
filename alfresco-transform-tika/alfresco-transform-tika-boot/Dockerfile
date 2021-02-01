# Image provides a container in which to run Tika transformations for Alfresco Content Services.

# Tika is from Apache. See the license at http://www.apache.org/licenses/LICENSE-2.0.

FROM alfresco/alfresco-base-java:11.0.10-openjdk-centos-7-9da90416ab19

ENV APACHE_LICENSE_FILE=https://github.com/Alfresco/acs-community-packaging/blob/master/distribution/src/main/resources/licenses/3rd-party/Apache%202.0.txt
ENV JAVA_OPTS=""

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG TIKAUSERNAME=tika
ARG USERID=33004

COPY target/${env.project_artifactId}-${env.project_version}.jar /usr/bin

RUN ln /usr/bin/${env.project_artifactId}-${env.project_version}.jar /usr/bin/${env.project_artifactId}.jar && \
    curl -s -S $APACHE_LICENSE_FILE -o Apache\ 2.0.txt && \
    yum clean all

ADD target/generated-resources/licenses              /licenses
ADD target/generated-resources/licenses.xml          /licenses/
ADD target/generated-sources/license/THIRD-PARTY.txt /licenses/

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${TIKAUSERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/${env.project_artifactId}.jar

EXPOSE 8090

USER ${TIKAUSERNAME}

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/${env.project_artifactId}.jar
