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