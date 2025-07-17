# T-Engine Scaling

The T-Engine can be scaled both horizontally and vertically. For either approach, we recommend keeping the `TRANSFORMER_ENGINE_PROTOCOL`
at its default value of `jms`. This setting enables the use of a JMS messaging with the ActiveMQ broker.

## Horizontal Scaling
T-Engine is intended to be run as a Docker image. Horizontal Scaling could be achieved through creating multiple Docker containers.

T-Engine relies on JMS queues, which provide built-in load balancing. This design allows you to safely run multiple instances
of the T-Engine service. Reliable messaging ensures that each message is delivered once and only once to a consumer. In point-to-point
messaging, while many consumers may listen on a queue, each message is consumed by only one instance.

### Example
```yaml
transform-core-aio:
  image: quay.io/alfresco/alfresco-transform-core-aio:5.1.7
  environment:
    ACTIVEMQ_URL: nio://activemq:61616
    FILE_STORE_URL: >-
      http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file
  ports:
    - "8090-8091:8090" # Host ports 8090 and 8091 will be used
  deploy:
    replicas: 2 # Two instances of t-engine will be created
```

### Scaling of individual T-Engines
There are options to use five separate T-Engines instead of one single `all-in-one` T-Engine. These options are:
  1. LibreOffice
  2. ImageMagick
  3. PdfRenderer
  4. Tika
  5. Misc

Horizontal Scaling could be achieved for these T-Engines as well - by creating multiple Docker containers for each of the T-Engines.

### Example
```yaml
libre-office:
  image: quay.io/alfresco/alfresco-libreoffice:5.1.7
  environment:
    ACTIVEMQ_URL: nio://activemq:61616
    FILE_STORE_URL: >-
      http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file
  ports:
    - "8201-8202:8090" # Host ports 8201 and 8202 will be used
  deploy:
    replicas: 2 # Two instances of libre-office t-engine will be created

image-magick:
  image: quay.io/alfresco/alfresco-imagemagick:5.1.7
  environment:
    ACTIVEMQ_URL: nio://activemq:61616
    FILE_STORE_URL: >-
      http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file
  ports:
    - "8203-8204:8090" # Host ports 8203 and 8204 will be used
  deploy:
    replicas: 2 # Two instances of image-magick t-engine will be created

pdf-renderer:
  image: quay.io/alfresco/alfresco-pdf-renderer:5.1.7
  environment:
    ACTIVEMQ_URL: nio://activemq:61616
    FILE_STORE_URL: >-
      http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file
  ports:
  - "8205-8206:8090" # Host ports 8205 and 8206 will be used
  deploy:
  replicas: 2 # Two instances of pdf-renderer t-engine will be created

tika:
  image: quay.io/alfresco/alfresco-tika:5.1.7
  environment:
    ACTIVEMQ_URL: nio://activemq:61616
    FILE_STORE_URL: >-
      http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file
  ports:
  - "8207-8208:8090" # Host ports 8207 and 8208 will be used
  deploy:
  replicas: 2 # Two instances of tika t-engine will be created

transform-misc:
  image: quay.io/alfresco/alfresco-transform-misc:5.1.7
  environment:
    ACTIVEMQ_URL: nio://activemq:61616
    FILE_STORE_URL: >-
      http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file
  ports:
  - "8209-8210:8090" # Host ports 8209 and 8210 will be used
  deploy:
  replicas: 2 # Two instances of transform-misc t-engine will be created
```

### Limitations

-  Alfresco Content Services (ACS) Repository can only be configured with a single T-Engine service URL.
   ```
   localTransform.core-aio.url=http://transform-core-aio:8090/
   ```
-  T-Router can only be configured with a single T-Engine service URL.
   ```
   CORE_AIO_URL: http://transform-core-aio:8090
   ```
-  Search and Search Reindexing has a dependency on a single T-Engine service URL.
   ```
   ALFRESCO_ACCEPTED_CONTENT_MEDIA_TYPES_CACHE_BASE_URL: >-
      http://transform-core-aio:8090/transform/config
   ```
-  T-Engine depends on ActiveMQ and shared-file-store. When running multiple T-Engine instances (nodes),
same URL for ActiveMQ and shared file store must be provided to all of the T-Engine nodes.

-  In Kubernetes environments, horizontal scaling is typically handled automatically via deployments and built-in load balancing.
In other environments, own load balancer is required in front of T-Engine to distribute requests.

## Vertical Scaling

Vertical scaling can be achieved through Docker, JVM, Spring Boot, or ActiveMQ configurations.

- **`mem_limit` (Docker):**
  Sets the maximum amount of memory the container can use. Increasing this allows the T-Engine to handle more concurrent processing
  and larger workloads.  
  **Default:** Not set (unlimited, but typically limited by orchestrator or host).


- **`JAVA_OPTS` (JVM):**  
  Sets Java Virtual Machine options. It is recommended to set JVM memory using `-XX:MinRAMPercentage` and `-XX:MaxRAMPercentage`
  in combination with the container's `mem_limit` parameter. This allows the JVM to dynamically adjust its heap size based on
  the memory available to the container. This is important for handling more messages or larger payloads.  
  **Default:** Not set (JVM uses its own default heap sizing).


- **`SPRING_ACTIVEMQ_POOL_MAXCONNECTIONS` (Spring Boot):**  
  Configures the maximum number of pooled connections to ActiveMQ. Increasing this value allows more simultaneous connections to
  the message broker, which can improve throughput under heavy load.  
  **Default:** 1 set by Spring Autoconfiguration, 20 set by base engine's application.yaml


- **`JMS_LISTENER_CONCURRENCY` (Spring Boot):**  
  The number of concurrent sessions/consumers to start for each listener. Can either be a simple number indicating the maximum
  number (e.g. "5") or a range indicating the lower as well as the upper limit (e.g. "3-5"). Note that a specified minimum is
  just a hint and might be ignored at runtime. Default is 1; keep concurrency limited to 1 in case of a topic listener or if queue
  ordering is important; consider raising it for general queues. Raising the upper limit allows more messages to be processed in
  parallel, increasing throughput.  
  **Default:** 1 set by Spring Autoconfiguration, 1-10 set by base engine's application.yaml


- **`ACTIVEMQ_CONN_OPTIONS` with `jms.prefetchPolicy.all` (Spring Boot/ActiveMQ):**  
  Overrides the default ActiveMQ connection options of broker URL by including prefetch policy settings.
  It controls how many messages are prefetched from the queue by each consumer before processing. A higher prefetch value can
  improve throughput but may increase memory usage. Note that raising this number might lead to starvation of concurrent consumers!  
  **Default:** `?jms.watchTopicAdvisories=false` (prefetch policy default is 1000 for queues)

  **Note:** The T-Engines consumers should be considered as slow consumers. Processing each message can take a significant amount of time.
  So, the prefetch size should be correctly set (based on performance tests) to distribute the messages equally across the nodes
  and consumers. The default prefetch size of 1000 messages should be decreased, otherwise a single consumer will fetch all the
  messages, and it will lead to performance degradation.


### Comprehensive Example

Below is a single example that combines memory limits, JVM options, concurrency, and message prefetch settings for vertical scaling:

```yaml
  transform-core-aio:
    image: quay.io/alfresco/alfresco-transform-core-aio:5.1.7
    mem_limit: 2048m # Sets the container memory limit
    environment:
      JAVA_OPTS: >- # Sets the JVM heap size on container memory limit
        -XX:MinRAMPercentage=50
        -XX:MaxRAMPercentage=80
      ACTIVEMQ_URL: nio://activemq:61616
      FILE_STORE_URL: >-
        http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file
      ACTIVEMQ_CONN_OPTIONS: ?jms.watchTopicAdvisories=false&jms.prefetchPolicy.all=100
      SPRING_ACTIVEMQ_POOL_MAXCONNECTIONS: 100 # Increases the ActiveMQ connection pool 
      JMS_LISTENER_CONCURRENCY: 1-100 # Increases the JMS listener concurrency
    ports:
      - "8090:8090"
```

### Limitations

- **`mem_limit` (Docker):**
  The maximum memory assigned to the container should be carefully decided. It depends on the host machine’s RAM size, total
  memory assigned/required to the other containers (if present) in the same host machine.


- **`JAVA_OPTS` (JVM):**
  JVM maximum RAM percentage to 100% of `mem_limit` is not recommended, as this can cause the JVM to use all available
  container memory, leaving no room for other processes and potentially leading to container restarts due to out-of-memory (OOM)
  errors.
