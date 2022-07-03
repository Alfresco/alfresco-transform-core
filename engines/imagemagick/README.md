# How to create ImageMagick RPM packages for CentOS 7
1. `cd` to this directory
2. Use CentOS 7 base image and execute the [build-rpms.sh](scripts/build-rpms.sh) script

```bash
docker run -it --rm -v `pwd`:/opt/alfresco/imagemagick alfresco/alfresco-base-java:11.0.13-centos-7@sha256:c1e399d1bbb5d08e0905f1a9ef915ee7c5ea0c0ede11cc9bd7ca98532a9b27fa /bin/bash /opt/alfresco/imagemagick/scripts/build-rpms.sh
```

> **Note:** If you want to know more details or need to upgrade the ImageMagick version please check the comments in the [build-rpms.sh](scripts/build-rpms.sh) file

3. RPMs will be created in the `rpms` directory

```bash
bash-3.2$ ls rpms
ImageMagick-7.0.10-59.x86_64.centos7.rpm	ImageMagick-libs-7.0.10-59.x86_64.centos7.rpm
```