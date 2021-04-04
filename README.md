# Up and running with K8s
Kubernetes is an open source orchestrator for deploying containerized applications. It provides the
software necessary to successfully build and deploy reliable, scalable distributed systems.

Kubernetes strongly believes in *declarative configuration*; everything in Kubernetes is a
declarative configuration object that represents the desired state of the system. The key idea is
having configuration files or manifests describing the desired state of the world. Those manifests
are submitted to a service that ensures this desired state becomes the actual state.

Declarative configuration is an alternative to imperative configuration, where the state of the
world is defined by the execution of a series of instructions rather than a declaration of the
desired state of the world. While imperative commands define actions, declarative configurations
define state. It is the difference between **doing** and **describing**.

With declarative state rollbacks become trivially easy; it is simply restating the previous
declarative state of the system. With imperative systems this is usually impossible, since while the
imperative instructions describe how to get you from point A to point B, they rarely include the
reverse instructions that can get you back.

## Lifecycle

### Set up
In this example we will setup a minikube single-node cluster.
```bash
$ minikube start --namespace='default' --mount=true --mount-string="$PWD/minikube-data:/minikube-host" --memory=6g
$ minikube dashboard
```

Your cluster should be up and running. A cluster is a collection of cooperating nodes In Kubernetes,
nodes are seperated into:
1. Master nodes that contain containers like the API server, scheduler, etc., which manage the cluster.
1. Worker nodes where your containers will run.

Kubernetes won’t generally schedule work onto master nodes to ensure that user workloads don’t harm
the overall operation of the cluster. Because we are running Kubernetes locally, we have an one-node
cluster.
```bash
$ kubectl get nodes
NAME       STATUS   ROLES                  AGE   VERSION
minikube   Ready    control-plane,master   24h   v1.20.2
$ kubectl describe nodes minikube
```

In production environments multi-node Kubernetes clusters are installed.
```bash
$ kubectl get nodes
NAME                            STATUS   ROLES                                  AGE   VERSION
ip-172-16-80-146.ec2.internal   Ready    kafka-connect-nodes,node,spot-worker   42h   v1.18.10
ip-172-16-80-252.ec2.internal   Ready    master                                 23d   v1.18.10
ip-172-16-81-23.ec2.internal    Ready    node                                   23d   v1.18.10
ip-172-16-81-84.ec2.internal    Ready    kafka-connect-nodes,node,spot-worker   34h   v1.18.10
ip-172-16-82-14.ec2.internal    Ready    kafka-connect-nodes,node,spot-worker   23d   v1.18.10
ip-172-16-82-198.ec2.internal   Ready    node                                   23d   v1.18.10
ip-172-16-82-223.ec2.internal   Ready    node                                   23d   v1.18.10
ip-172-16-82-35.ec2.internal    Ready    kafka-connect-nodes,node,spot-worker   23d   v1.18.10
ip-172-16-83-220.ec2.internal   Ready    master                                 23d   v1.18.10
ip-172-16-84-17.ec2.internal    Ready    kafka-connect-nodes,node,spot-worker   42h   v1.18.10
ip-172-16-84-199.ec2.internal   Ready    node                                   23d   v1.18.10
ip-172-16-84-201.ec2.internal   Ready    node                                   23d   v1.18.10
ip-172-16-85-10.ec2.internal    Ready    master                                 23d   v1.18.10
```

### Tear down
We can check the status and delete a cluster:
```bash
$ minikube status
$ minikube delete
```

## The K8s resources
Everything contained in Kubernetes is represented by a *RESTful resource* or *Kubernetes object*.
Each Kubernetes object exists at a unique HTTP path, for example https://your-k8s.com/api/v1/namespaces/default/pods/my-pod
leads to the representation of a pod in the     default namespace named my-pod. The **kubectl command**
makes HTTP requests to these URLs to access the Kubernetes objects that reside at these paths.
```bash
$ kubectl cluster-info
Kubernetes master is running at https://127.0.0.1:53953
KubeDNS is running at https://127.0.0.1:53953/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy
```

### Getting information
The most basic command for viewing Kubernetes objects via **kubectl** is **get**. It gets a listing
of all resources in the current namespace (e.g. all pods, deployments, configMaps, etc.).
```bash
$ kubectl get <resource-name>
$ kubectl get <resource-name> <object-name>
```
By default, **kubectl** uses a human-readable printer for viewing the responses from the API server,
but this human-readable printer removes many of the details of the objects to fit each object on one
terminal line. One way to get slightly more information is to add the `-o wide` flag, which gives
more details, on a longer line. If you want to view the complete object, you can also view the
objects as raw JSON or YAML using the `-o json` or `-o yaml` flags, respectively.

If you are interested in more detailed information about a particular object, use the **describe**
command:
```bash
$ kubectl describe <resource-name> <object-name>
```

### Creating and deleting resources
Objects in the Kubernetes API are represented as *JSON* or *YAML* files. Let’s assume that you have
a simple object stored in `obj.yaml`. You can use **kubectl** to create this object in Kubernetes by
running:
```bash
$ kubectl apply -f obj.yaml
```
Notice that you don’t need to specify the resource type of the object; it’s obtained from the object
file itself. Similarly, after you make changes to the object, you can use the apply command again to
update the object.

When you want to delete an object, you can simply run:
```bash
$ kubectl delete -f obj.yaml
$ kubectl delete <resource-name> <object-name>
```

### Debugging
You can see the logs of a resource using the **log** command. In case multiple containers are
running to a single pod you can specify the container of interest.
```bash
$ kubectl log <log-name> -c container-name
```

You can also use the **exec** command to execute a command in a running container:
```bash
$ kubectl exec -it <pod-name> -- /bin/bash
```
This will provide you with an interactive shell inside the running container so that you can perform
more debugging.

## Pods
Many times containerized applications need to colocate into a single atomic unit, scheduled onto a
single machine. Such containerized applications need to work together symbiotically, forming a
single cohesive unit of service; for example, one container serving data stored in a shared volume
to the public, while a separate sidecar container refreshes or updates those files.

A pod represents a collection of application containers and volumes running in the same execution
environment. Pods, not containers, are the smallest deployable unit in a Kubernetes cluster. This
implies that all of the containers in a pod always land on the same machine. It might seem tempting
to wrap up all containers into a single *"uber container"* and deploy the latter instead. However,
there are good reasons for keeping the containers separate. First, the containers might have
significantly **different requirements** in terms of resource usage (e.g. memory, cpu). Secondly, we
want to ensure **resource isolation**; a problem in one container (e.g. a memory leak) cannot affect
other containers running in the same pod.

Applications running in the same pod share the same IP address and port space (network namespace),
have the same hostname (UTS namespace), and can communicate using native interprocess communication
channels.

Pods are described by pod manifests; a text-file representation of the Kubernetes object. The
manifests are generally written in `YAML` format, but `JSON` is also supported.
```bash
$ kubectl apply -f postgres/database.yaml
$ kubectl delete pods <pod-name>
```

When you run your application as a container in Kubernetes, it is automatically kept alive for you
using a process health check. This health check simply ensures that the main process of your
application is always running. If it is not, Kubernetes is responsible to restart it so to keep the
state of the world consistent. However, in many cases, a simple process check is insufficient. For
example, if your process has deadlocked and is unable to serve requests, a process health check will
still believe that your application is healthy since its process is still running. Kubernetes
introduced health checks for application liveness. Liveness health checks run application-specific
logic and need to be described in the pod manifest. Liveness probes are defined per container, which
means each container inside a pod is health-checked separately.

In addition to `httpGet` checks, Kubernetes also supports `tcpSocket` health checks that open a TCP
socket; if the connection is successful, the probe succeeds. This style of probe is useful for
non-HTTP applications; for example, databases or other non–HTTP-based APIs.

```yaml
# Custom pod HTTP health-check definition.
livenessProbe:
  httpGet:
    path: /healthy
    port: 8080
  initialDelaySeconds: 5
  timeoutSeconds: 1
  periodSeconds: 10
  failureThreshold: 3
```

With Kubernetes a pod can specify two different resource (typically memory and CPU) metrics:
- Resource **requests** specify the minimum amount of a resource required to run the application. Kubernetes guarantees that these resources are available to the pod.
- Resource **limits** specify the maximum amount of a resource that an application can consume.

```yaml
resources:
  requests:
    cpu: "500m"
    memory: "128Mi"
```

An important part of an application is access to persistent disk. The volumes that may be accessed
by containers in a pod are defined in `spec.volumes` section. Then, each container needs to define
the volumes it wants to mount and the respective path that the volumes are mounted. Note that two
different containers in a pod can mount the same volume at different mount paths. Using shared
volumes enables containers running in the same pod to share data between them.

Other applications do not actually need a persistent volume, but they do need some access to the
underlying host filesystem. For example, they may need access to the `/dev` filesystem in order to
perform raw block-level access to a device on the system. For these cases, Kubernetes supports the
`hostPath` volume, which can mount arbitrary locations on the worker node into the container.

```yaml
apiVersion: v1
kind: Pod
metadata:
  # It must be a valid a valid DNS subdomain name as defined in [RFC-1123](https://tools.ietf.org/html/rfc1123)
  name: mount-volume-example
spec:
  containers:
  - image: k8s.gcr.io/test-webserver
    name: test-container
    volumeMounts:
    - mountPath: /mount/path
      name: host-volume
  volumes:
  - name: host-volume
    hostPath:
      # directory location on host
      path: /data
      # this field is optional
      type: Directory
```

### Command and arguments
When you create a pod, you can define a command and arguments for each container that run in the
pod. To define a command, include the `spec.containers[].command` field in the configuration file.
To define arguments for the command, include the `spec.containers[].args` field in the configuration
file. The command and arguments that you define in the configuration file override the default
command and arguments provided by the container image. If you define args, but do not define a
command, the default command is used with your new arguments.

When using docker runtime:
  - The `command` corresponds to dockerfile's `ENTRYPOINT`.
  - The `args` corresponds to dockerfile's `CMD`.

## Labels and annotations
Labels are key-value pairs that are attached to Kubernetes objects, such as pods or replica sets.
Labels provide identifying metadata for Kubernetes objects. They provide the foundation for
searching, grouping and viewing objects. Annotations on the other hand are key-value pairs designed
to hold non-identifying information that can be leveraged by tools and libraries. While labels are
used to identify and group objects, annotations are used to provide extra information about where
an object came from, how to use it, or policy around that object.

Labels are key-value pairs where both the key and value are represented by strings. Keys can be
broken down into two parts: an optional prefix and a name, separated by a slash. The prefix, if
specified, must be a valid DNS subdomain as defined in [RFC-1123](https://tools.ietf.org/html/rfc1123)
with a 253-character limit. The key name is required and must be shorter than 63 characters. Values
are strings with a maximum length of 63 characters.

```bash
$ kubectl get <resource-name> --show-labels
$ kubectl get pods -l app=database,env=development
$ kubectl get pods -l 'app in (database, server)'
```

One common use case of labels is for pods to specify node selection criteria. For example, the
sample pod below selects nodes with the label _"accelerator=nvidia-tesla-p100"_:
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod-name
spec:
  containers:
    ...
  nodeSelector:
    accelerator: nvidia-tesla-p100
```

## Replica sets
More often than not, you want multiple replicas of an application running at a particular time. The
purpose of a ReplicaSet Kubernetes object is to maintain a stable set of replica pods running at any
given time. Of course, you could manually create multiple copies of a pod using multiple different
(though largely similar) pod manifests, but doing so is both tedious and error-prone. With replica
sets you can manage a replicated set of pods as a single entity. Pods managed by replica sets are
automatically rescheduled under certain failure conditions such as node failures and network
partitions.

### Pod acquisition
One of the key themes that runs through Kubernetes is decoupling. In this spirit, the relationship
between replica sets and pods is loosely coupled. Though replica sets create and manage pods, they
do not own the pods they create. Replica sets use labels to identify the set of pods they should be
managing. This implies that __a replica set can adopt existing pods__. A replica set is linked to
its pods via the pod's `metadata.ownerReferences` field, which specifies what resource the current
object is owned by. If there is a pod that matches selector labels of the replica set and does not
have any owner, then it will be will be immediately acquired by the replica set.

A replica set manifest needs to define a pod template specifying the kind of new pods it should
create to meet the number of replicas criteria. The pod template is only used at pod creation time.
Existing pods acquired by the replica set (based on the pod seletion criteria) migth have totally
different templates. It also implies that changing the pod template will not affect already existing
pods.

### Isolating pods
Oftentimes, when a server misbehaves, it is desirable to attach to the pod and interactively debug
it. In case the pod's health checks are incomplete, a pod can be misbehaving but still be part of
the replicated set. In such cases, instead of killing the pod, one can modify the labels of the sick
pod. Doing so, will disassociate it from the replica set. The controller of the replica set will
spawn a new copy, but because the sick pod is still running it is available for interactive
debugging.

### Autoscaling
While there will be times when you want to have explicit control over the number of replicas, often
you simply want to have “enough” replicas. You might want to scale based on memory consumption, CPU
usage, or in response to any custom application metric.

Kubernetes can handle autoscaling via an object called **HorizontalPodAutoscaler**. While the name
is kind of mouthful, Kubernetes makes a distinction between horizontal (adding more replicas of a
pod) and vertical scaling (increasing the resources available to a pod).

```yaml
apiVersion: autoscaling/v1
kind: HorizontalPodAutoscaler
metadata:
  name: ngnix-scaler
spec:
  scaleTargetRef:
    kind: ReplicaSet
    name: nginx-server-replicaset
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 50
```

### Final thoughs on replica sets
Typically, pods part of a replica set are fronted by a Kubernetes service load balancer, which
spreads traffic across the pods that make up the service. Generally speaking, replica sets are
designed for stateles (or nearly stateless) services. When a replica set is scaled down, an
arbitrary pod is selected for deletion. Using the `controller.kubernetes.io/pod-deletion-cost`
annotation, users can set a preference regarding which pods to remove first when downscaling a
replica set.

## Daemon sets
Replica sets are generally about creating a service (e.g., a web server) with multiple replicas for
redundancy. But that is not the only reason you may want to replicate a set of pods within a
cluster. Another reason to replicate a set of pods is to schedule a single pod on every node within
the cluster. Generally, the motivation for replicating a pod to every node is to land some sort of
agent or daemon on each node. Daemon sets are used to deploy system daemons such as *log collectors*
and *monitoring agents*, which typically must run on every node.

### Pod and node selectors
The `spec.selector` field is a pod selector, specifying the pods that are part of the daemon set. If
a pod matching the labels of the pod selector already exists in the node (and is not owned by anyone
else), it will be acquired by the daemon set. If you specify a `spec.template.spec.nodeSelector`,
then the daemon set controller will create pods on nodes which match that node selector.

### Rolling update
DaemonSet has two update strategy types:
- `OnDelete`: After you update a daemon set manifest, new pods will only be created when you manually delete old DaemonSet pods.
- `RollingUpdate`: After you update a daemon set manifest, old pods will be killed, and new pods will be created automatically, in a controlled fashion.

An alternative, is to just delete the entire daemon set and create a new daemon set with the updated
configuration. However, this approach has a major drawback: **downtime**. When a daemon set is
deleted all pods managed by that daemon set will also be deleted. Depending on the size of your
container images, recreating a daemon set may push you outside of your SLA thresholds.

## Jobs
So far we have focused on long-running workloads such as databases and web applications. While
long-running processes make up the large majority of workloads, there is often a need to run
*short-lived*, *one-off* tasks. A job creates one or more pods and will continue to retry execution
of the pods until a specified number of them successfully terminate (i.e., exit with 0). When a
specified number of successful completions is reached, the job is complete. Deleting a Job will
clean up the Pods it created. In contrast, a regular pod will continually restart regardless of its
exit code.

The `spec.selector` field is optional. In almost all cases you should not specify it. The system
defaulting logic adds this field when the job is created. It picks a selector value that will not
overlap with any other jobs. Like with other controllers (daemon sets, replica sets, deployments,
etc.) that use labels to identify a set of pods, unexpected behaviors can happen if a pod is reused
across objects.

### Job patterns
There are three main types of task suitable to run as a job:
  1. `One-shot`: only one pod is started, unless the pod fails. This is suitable for tasks like database migrations.
  2. `Parallel jobs with fixed completion count`: multiple pods processing a fixed set of work in parallel.
  3. `Parallel Jobs pulling tasks from a work queue:`: multiple pods processing from a centralized work queue. This requires running a queue service, and modifications to the existing application to make it use the work queue.

### Completion
It is possible to specify a `spec.completionMode: Indexed`, in case we want to differentiate between
pods of the same job. Each pod gets an associated completion index from `0` to `spec.completions - 1`,
which is available in the annotation `batch.kubernetes.io/job-completion-index`. The value of the
index is exposed in the `JOB_COMPLETION_INDEX` environment variable. The job is considered complete
when there is one successfully completed pod for each index.

After the job has completed, the job object and related pods are still available. This is so that
you can inspect the log output. The job object also remains after it is completed so that you can
view its status. It is up to the user to delete old jobs.
```bash
$ kubectl delete jobs <job-name>
```

## Config Maps
Config maps are used to provide non-confidential configuration information for Kubernetes workloads
in the form of key value pairs. This can either be fine-grained information (a short string) or a
composite value in the form of a file. Pods can consume config maps as environment variables,
command line arguments, or as configuration files in a volume.

A config map is a Kubernetes object that lets you store configuration for other objects to use.
Unlike most Kubernetes objects that have a spec, a config map has `data` and `binaryData` fields.
These fields accept key-value pairs as their values. The `data` field is designed to contain UTF-8
byte sequences while the `binaryData `field is designed to contain binary data as base64-encoded
strings.

### Injecting configuration into pods
There are four different ways that you can use a config map to configure a container inside a pod:
  1. `Command-line arguments`: in the `command` and `args` fields of a container.
  1. `Environment variable`: sets the value of environmental variables of a container.
  1. `Filesystem`: mounts a config map in a read only volume as a file.
  1. `Kubernetes API`: application code that reads directly from the API.

For the first three methods, the Kubernetes uses the data from the config map *when it launches container(s)* 
for a pod. The fourth method means you have to write code to read the config map and its data.
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: config-example
data:
  username: "username"
  password: "password"
  file.config: |
    config.first=one
    config.two=two
```

In the pod definition we can include the aforementioned parameters.
```yaml
apiVersion: v1
kind: Pod
spec:
  volumes:
    # Set volumes at the pod level, then mount them into containers inside that pod
    - name: config-volume
      configMap:
        # Provide the name of the ConfigMap you want to mount.
        name: config-example
        # An array of keys from the ConfigMap to create as files
        # If you not specify anything here, every key under data will be mounted.
        items:
        - key: file.config
          path: file.config
  
  containers:
    - name: some-name
    image: ubuntu:20.04
    command: ["/bin/bash", "-c"]
    args: ["echo $(ENV_VAR_PASSWORD) && while true; do sleep 30; done;"]
    env:
      # Define the environment variables
      - name: ENV_VAR_PASSWORD # Notice that the case is different here from the key name in the ConfigMap.
        valueFrom:
          configMapKeyRef:
            name: config-example # The ConfigMap this value comes from.
            key: password        # The key to fetch.
      - name: ENV_VAR_USERNAME
        valueFrom:
          configMapKeyRef:
            name: config-example # The ConfigMap this value comes from.
            key: username        # The key to fetch.
    volumeMounts:
      # The name must match the name of a pod volume.
      - name: config-volume
        mountPath: "/config"
        readOnly: true
```

When a config map currently consumed in a volume is updated, projected keys are eventually updated
as well. A config map can be either propagated by watch (default), ttl-based, or by redirecting all
requests directly to the API server. As a result, the total delay from the moment when the config
map is updated to the moment when new keys are projected to the pod can be as long as the sync
period + cache propagation delay, where the cache propagation delay depends on the chosen cache type
(it equals to watch propagation delay, ttl of cache, or zero correspondingly).

## Deployments
Deployment objects help manage the release of new versions of your application. With a deployment
object you can simply and reliably rollout new software versions without downtime or errors.

### Replica set selectors
Deployments manage replica sets, the same way replica sets manage pods. A deployment can adopt an
already existing replica set if it matches the `spec.selector` field. At a first glance, the
specification for a deployment looks very much like the one for a replica set. Deployments, however,
don’t create or delete pods directly. They delegate that work to one ore more replica sets. When we
create a deployment, it creates a replica set, using the exact pod specification that we gave it.

The `pod-template-hash` label is added to every replica set that a deployment creates or adopts.
This label ensures that child replica sets of a deployment do not overlap. It is generated by
hashing the pod manifest of the replica set and using the resulting hash as the label
value that is added to the replica set selector, pod template labels, and in any existing pods that
the replica set might have.

### Upgrades
Things get interesting when we need to update the pod specification itself. This is a common use
case due to bug fixes, change in business requirements and so on. For instance, we might want to
change the image to use (because we are releasing a new version), or the application’s parameters
(through command-line arguments, environment variables, or configuration files).

A deployment rollout is triggered if and only if the deployment's pod template (that is,
`spec.template`) is changed. For example if the labels or container images of the template are
updated. Other updates, such as scaling the deployment, do not trigger a rollout.

You can add the `--record` flag when applying a deployment to keep track of the commands executed.
The recorded change is useful for future introspection (e.g. when we want to rollback). The `CHANGE-CAUSE`
is copied from the deployment annotation `kubernetes.io/change-cause`.

When a rollout is triggered, the deployment creates a new replica set with the updated pod
specification. That replica set has an initial size of zero. Then, the size of that replica set is
progressively increased, while decreasing the size of the old replica set.

Assume we have deployed a replica set with the following specification:
```yaml
apiVersion: apps/v1
kind: ReplicaSet
metadata:
  labels:
    app: some-app
spec:
  replicas: 2
```

Now if we create a deployment that selects on the labels of the replica set, then the deployment
will acquire the existing replica set. Depending on whether the pod specification is the same or
different a rollout will occur.
```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  replicas: 2
  selector:
    matchLabels:
      app: some-app
```

### Rollout information
During an upgrade you can inspect the status of the rollout using the `rollout status` command.
```bash
$ kubectl rollout status deployments <deployment-name>
Waiting for deployment "<deployment-name>" rollout to finish: 1 out of 3 new replicas have been updated...
Waiting for deployment "<deployment-name>" rollout to finish: 1 out of 3 new replicas have been updated...
Waiting for deployment "<deployment-name>" rollout to finish: 1 out of 3 new replicas have been updated...
Waiting for deployment "<deployment-name>" rollout to finish: 2 out of 3 new replicas have been updated...
Waiting for deployment "<deployment-name>" rollout to finish: 2 out of 3 new replicas have been updated...
Waiting for deployment "<deployment-name>" rollout to finish: 2 out of 3 new replicas have been updated...
Waiting for deployment "<deployment-name>" rollout to finish: 1 old replicas are pending termination...
Waiting for deployment "<deployment-name>" rollout to finish: 1 old replicas are pending termination...
deployment "<deployment-name>" successfully rolled out
```

### Rollbacks
Sometimes, you may want to rollback a deployment; for example, when the deployment is not stable. By
default, all of the deployment's rollout history is kept in the system so that you can rollback
anytime you want.

You can check the rollout history using `rollout history` command. You can inspect a specific
revision by using `--revision` flag.
```bash
$ kubectl rollout history deployments <deployment-name> --revision=<revision-num>
```

You can select the revision you want to rollback and apply the change.
```bash
$ kubectl rollout undo deployments <deployment-name> --to-revision=<revision-num>
```

### Strategies
There are two different strategies for rolling out an update:
  - `spec.strategy.type: RollingUpdate`, updates pods in a rolling update fashion.
  - `spec.strategy.type: Recreate`, all existing pods are killed before spawning new ones.

Rolling update strategy ensures that only a certain number of pods are down while they are being
updated. The `spec.strategy.rollingUpdate.maxUnavailable` field specifies the maximum number of pods
that can be unavailable during the update process. The value can be an absolute number (for example,
5) or a percentage of desired pods (for example, 10%). For example, when this value is set to 30%,
the old replica set can be scaled down to 70% of desired pods immediately when the rolling update
starts. Once new pods are ready, old replica set can be scaled down further. On the other side of
the spectrum, the `spec.strategy.rollingUpdate.maxSurge` field specifies the maximum number of pods
that can be created over the desired number of pods. The value can be an absolute number (for
example, 5) or a percentage of desired pods (for example, 10%). For example, when this value is set
to 30%, the new replica set can be scaled up immediately when the rolling update starts, such that
the total number of old and new Pods does not exceed 130% of desired pods.
