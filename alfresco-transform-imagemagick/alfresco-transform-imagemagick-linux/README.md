# How to create ImageMagick RPM packages for Linux (eg. CentOS 8 / Rocky Linux 8)
1. `cd` to this directory
2. Use Rocky Linux 8 base image and execute the [build-rpms.sh](scripts/build-rpms.sh) script

```bash
docker run -it --rm -v `pwd`:/opt/alfresco/imagemagick alfresco/alfresco-base-java:jre11-rockylinux8-202203101229 /bin/bash /opt/alfresco/imagemagick/build-rpms.sh
```

> **Note:** If you want to know more details or need to upgrade the ImageMagick version please check the comments in the [build-rpms.sh](scripts/build-rpms.sh) file

3. RPMs will be created in the `rpms` directory

```bash
bash-3.2$ ls rpms
ImageMagick-7.1.0-16.x86_64.linux.rpm	ImageMagick-libs-7.1.0-10.x86_64.linux.rpm
```