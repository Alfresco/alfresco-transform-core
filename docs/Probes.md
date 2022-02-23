# Transformer k8s liveness and readiness probes

>**Note:** The transform-specific liveness probes are currently disabled by default in the 
Alfresco Docker Transformers **2.0.0-RC3** release. They can be enabled through the 
"**livenessTransformEnabled**" environment variable.
>
> The T-Engine liveness probes will be reevaluated/changed/improved as part of the ATS-138 story.
>
> Without the transform-specific liveness probees, calls to the "/live" endpoint of the 
T-Engines only check if the JVM is alive.

The transformer's liveness and readiness probes perform small test transformations to check that a pod has fully started up and that it is still healthy.

Initial test transforms are performed by both probes on start up. After a successful transform, the readiness probe always returns a success message and does no more transformations. As a result requests will not be suspended to the pod unless there is a network issue.

The liveness probe gathers the average time of 5 test transformation after start up (the initial transform is ignored as it may take longer). It will then do a test transform occasionally (see livenessTransformPeriodSeconds) to ensure the system is still healthy. If any of these transforms take longer that a specified percentage, the probe will return an error status code to terminate the pod. The liveness probe may optionally also be configured to return an error if a specified number of transformations have been performed or if any transform takes longer than some specified limit.
Environment variables

### Configuration
The actions of the probes are controlled by environment variables

    livenessPercent - The percentage slower the small test transform must be to indicate there is a problem. Generally
        set to 150 so the test transform may be two and a half times as long.

    livenessTransformPeriodSeconds - As liveness probes should be frequent, not every request should result in a test
        transformation. This value defines the gap between transformations. Generally set to 10 minutes.

    maxTransforms - the maximum number of transformation to be performed before a restart. Generally quite a high number
        but does allow for the pod to be recycled occationally. A value of 0 disables this check.

    maxTransformSeconds - the maximum time for a transformation, including failed ones. Quite large values needs to be
        used to avoid recycling pods if there are large files that take a long time. Generally transforms that do take
        a long time indicate that the transformer is no longer healthy. A value of 0 disables this check.

The rate and frequency of the probes are controlled by standard k8s fields. See [K8s Liveness and Readiness Probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-probes/#configure-probes)

    initialDelaySeconds - The maximum time that the probe waits before being called. Generally configured so that the
        live probe has a shorter value has a chance to initialise the pod beore the first request come in.

    periodSeconds - The frequence that the probe is called. Noteboth probes are called throughout the lieftime of the
        pod.

    timeoutSeconds - Generally overrides the default of 1 second, to allow the probe long enough to check slow test
        transforms.
    failureThreshold - set to 1 in the case of the liveness probe, so that any failure terminates the pod sright away.
        In the case of readiness probe this is left as the default 3, to give the pod a chance to start.


## Helm chart use of these variables and fields

#### Values.yaml

~~~yaml
 imagemagick:
 # ...
  readinessProbe:
    initialDelaySeconds: 20
    periodSeconds: 60
    timeoutSeconds: 10
  livenessProbe:
    initialDelaySeconds: 10
    periodSeconds: 20
    timeoutSeconds: 10
    livenessPercent: 150
    livenessTransformPeriodSeconds: 600
    maxTransforms: 10000
    maxTransformSeconds: 900
~~~

#### deployment-imagemagick.yaml

~~~yaml
apiVersion: extensions/v1beta1
kind: Deployment
# ...
spec:
    # ...
    spec:
      containers:
          envFrom:
          - configMapRef:
              # config map to use, defined in config-imagemagick.yaml
              name: {{ template "content-services.fullname" . }}-imagemagick-configmap
          readinessProbe:
            httpGet:
              path: /ready
              port: {{ .Values.imagemagick.image.internalPort }}
            initialDelaySeconds: {{ .Values.imagemagick.readinessProbe.initialDelaySeconds }}
            periodSeconds: {{ .Values.imagemagick.readinessProbe.periodSeconds }}
            timeoutSeconds: {{ .Values.imagemagick.readinessProbe.timeoutSeconds }}
          livenessProbe:
            httpGet:
              path: /live
              port: {{ .Values.imagemagick.image.internalPort }}
            initialDelaySeconds: {{ .Values.imagemagick.livenessProbe.initialDelaySeconds }}
            periodSeconds: {{ .Values.imagemagick.livenessProbe.periodSeconds }}
            failureThreshold: 1
            timeoutSeconds: {{ .Values.imagemagick.livenessProbe.timeoutSeconds }}
    # ...
~~~

#### config-imagemagick.yaml

~~~yaml
# Defines the properties required by the imagemagick container
apiVersion: v1
kind: ConfigMap
# ...
data:
  livenessPercent: "{{ .Values.imagemagick.livenessProbe.livenessPercent }}"
  livenessTransformPeriodSeconds: "{{ .Values.imagemagick.livenessProbe.livenessTransformPeriodSeconds }}"
  maxTransforms: "{{ .Values.imagemagick.livenessProbe.maxTransforms }}"
  maxTransformSeconds: "{{ .Values.imagemagick.livenessProbe.maxTransformSeconds }}"

~~~
