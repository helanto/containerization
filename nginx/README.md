### A stateless web service

#### Replica sets

Let's spin a stand-alone nginx instance.
```bash
$ kubectl apply -f nginx/nginx-pod.yaml
```

After the pod is created we can spin an nginx replica set with replication factor 2.
```bash
$ kubectl apply -f nginx/nginx-rs.yaml
```

It is important to notice that only one additional instance of the application is initialized. The
replica set acquired the already existing nginx application.
```bash
$ kubectl get pods
NAME                            READY   STATUS    RESTARTS   AGE
nginx-server                    1/1     Running   0          67s
nginx-server-replicaset-wswfj   1/1     Running   0          4s
```

We can see that the existing `nginx-server` pod is now owned by the replica set. As already
discussed, this is because the pod selector of the replica set matched the labels of the existing
nginx application, and the application did not have any owner at the time.
```bash
$ kubectl get pods nginx-server -o yaml
metadata:
  ownerReferences:
  - apiVersion: apps/v1
    blockOwnerDeletion: true
    controller: true
    kind: ReplicaSet
    name: nginx-server-replicaset
    uid: 98944c82-7297-438a-928d-64a2205ce7fc
$
$ kubectl describe pods nginx-server
Name:           nginx-server
Controlled By:  ReplicaSet/nginx-server-replicaset
```

The two nginx pods are not identical to each other (notice the difference in CPU resources). This is
because replica sets only use pod templates when creating new pods. As far as the replica set is
concerned, the existing pod it adopted could have been running a MySQL installation.
```bash
$ kubectl describe nodes
Non-terminated Pods:          (11 in total)
Namespace                   Name                                CPU Requests  CPU Limits  Memory Requests  Memory Limits  AGE
---------                   ----                                ------------  ----------  ---------------  -------------  ---
default                     nginx-server                        200m (10%)    500m (25%)  128Mi (1%)       256Mi (3%)     2m59s
default                     nginx-server-replicaset-wswfj       100m (5%)     100m (5%)   128Mi (1%)       256Mi (3%)     116s
```

If we manually delete the original nginx pod, a new pod will be spawn, so to match replication
criteria.
```bash
$ kubectl delete pods nginx-server
$ kubectl get pods
NAME                            READY   STATUS    RESTARTS   AGE
nginx-server-replicaset-kwcfr   1/1     Running   0          10s
nginx-server-replicaset-wswfj   1/1     Running   0          19m
```

#### Upgrading the pod specification
Assume now that a new version of nginx is available and we want to upgrade our application. We need
to update the pod template in the replica set manifest and deploy the new specification.
Unfortunately nothing happens. When we update the pod specification, the replica set is updated but
no new pods are deployed. This is because replica sets are only responsible to _make sure that there are N pods available at any given time_.
When we manually bring down old pods, the new pods deployed will have the upgraded nginx version.

#### Enter the deployment
This is where deployments come into play. When updating the pod specification of a deployment, a new
replica set is created accordingly to the specification. The old replica set is progressively scaled
down until becoming idle.

Let's apply the deployment while the replica set is still active.
```bash
$ kubectl get rs
NAME               DESIRED   CURRENT   READY   AGE
nginx-replicaset   3         3         3       2m2s
$
$ kubectl get rs -o=jsonpath='{.items[*].metadata.labels}'
map[app:nginx env:development]
$
$ kubectl apply -f nginx/nginx-deployment.yaml
```

The deployment acquires the existing replica set because of the replica set selector criteria
```yaml
spec:
  selector:
    matchLabels:
      app: nginx
```

Because the pod templates between the existing replica set and the upcoming deployment are
equivalent, there is no need to rollout a new deployment.
```bash
$ kubectl get rs nginx-replicaset -o yaml
metadata:
  ownerReferences:
  - apiVersion: apps/v1
    blockOwnerDeletion: true
    controller: true
    kind: Deployment
    name: nginx-deployment
    uid: ed56a49d-0e13-48fd-8775-fdc9c485c14c
```

When we update a deployment and adjust the number of replicas, it passes that update down to the
replica set, withour creating a new revision.
```bash
$ kubectl scale deployments nginx-deployment --replicas=4
```

When we update the pod specification, the deployment creates a new replica set with the updated pod
specification. Assume we update the nginx image and apply the change:
```bash
kubectl set image deployments nginx-deployment nginx-container=nginx:1.19.0 --record
```

Because of the `RollingUpdate` rollout strategy:
  - 2/4 existing pods continue operating and the other 2 are deleted immediatelly (50% unavailability).
  - 3 new pods are created (2 replacement pods + 1 surge pod).

```bash
$ kubectl get deployments nginx-deployment
NAME               READY   UP-TO-DATE   AVAILABLE   AGE
nginx-deployment   2/4     3            2           67s
$
$ kubectl get rs
NAME                          DESIRED   CURRENT   READY   AGE
nginx-deployment-7bf7576f44   3         3         0       36s
nginx-replicaset              2         2         2       109s
$
$ kubectl get pods
nginx-deployment-7bf7576f44-c8v5l   0/1     ContainerCreating   0          64s
nginx-deployment-7bf7576f44-n7v72   0/1     ContainerCreating   0          64s
nginx-deployment-7bf7576f44-rd779   0/1     ContainerCreating   0          64s
nginx-replicaset-qlvgn              1/1     Running             0          2m18s
nginx-replicaset-t9qrl              1/1     Running             0          2m18s
$
$ kubectl rollout status deployments nginx-deployment
Waiting for deployment "nginx-deployment" rollout to finish: 3 out of 4 new replicas have been updated...
Waiting for deployment "nginx-deployment" rollout to finish: 3 out of 4 new replicas have been updated...
Waiting for deployment "nginx-deployment" rollout to finish: 3 out of 4 new replicas have been updated...
Waiting for deployment "nginx-deployment" rollout to finish: 1 old replicas are pending termination...
Waiting for deployment "nginx-deployment" rollout to finish: 1 old replicas are pending termination...
Waiting for deployment "nginx-deployment" rollout to finish: 1 old replicas are pending termination...
Waiting for deployment "nginx-deployment" rollout to finish: 2 of 4 updated replicas are available...
Waiting for deployment "nginx-deployment" rollout to finish: 3 of 4 updated replicas are available...
deployment "nginx-deployment" successfully rolled out
```

We can observe the rollout history.
```bash
$ kubectl rollout history deployments nginx-deployment
deployment.apps/nginx-deployment
REVISION  CHANGE-CAUSE
1         nginx:1.19.9
2         kubectl set image deployments nginx-deployment nginx-container=nginx:1.19.0 --record=true
```

We can rollback to the first revision.
```bash
$ kubectl rollout undo deployments nginx-deployment --to-revision=1
$
$ kubectl rollout history deployments nginx-deployment
deployment.apps/nginx-deployment
REVISION  CHANGE-CAUSE
2         kubectl set image deployments nginx-deployment nginx-container=nginx:1.19.0 --record=true
3         nginx:1.19.9
```