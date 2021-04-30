### An nginx application
First we need to create two stand-alone nginx instances. The two instances are mirror of each othe
```bash
$ kubectl apply -f services/nginx-pod-1.yaml
$ kubectl apply -f services/nginx-pod-2.yaml
```

We can see that the two pods are up and running.
```bash
$ kubectl get pods -o wide
NAME          READY   STATUS    RESTARTS   AGE   IP           NODE       NOMINATED NODE   READINESS GATES
nginx-pod-1   1/1     Running   0          26s   172.17.0.3   minikube   <none>           <none>
nginx-pod-2   1/1     Running   0          18s   172.17.0.4   minikube   <none>           <none>
```

### In the absense of services
To perform DNS-related actions, we will use the `tutum/dnsutils` container image, which is available
online and contains both the `nslookup` and the `dig` binaries. To run the pod, we can go through
the whole process of creating a YAML manifest for it, and then `exec` a shell on the pod. Luckily,
there is a faster way; doing it in a declerative fashion.
```bash
$ kubectl run dnsutils --image=tutum/dnsutils --command -- sleep infinity
$ kubectl get pods -o wide
NAME          READY   STATUS    RESTARTS   AGE   IP           NODE       NOMINATED NODE   READINESS GATES
dnsutils      1/1     Running   0          5s    172.17.0.5   minikube   <none>           <none>
nginx-pod-1   1/1     Running   0          16m   172.17.0.3   minikube   <none>           <none>
nginx-pod-2   1/1     Running   0          16m   172.17.0.4   minikube   <none>           <none>
```

Now we can connect to the `dnsutil` pod and query the nginx servers. Thankfully, in Kubernetes all
pods can reach each other on their internal IP addresses, no matter on which nodes they are running
(in our case everything is running on the same `minikube` node).
```bash
$ kubectl exec -it dnsutils -- /bin/bash
$ curl 172.17.0.3:80
$ curl 172.17.0.4:80
```
Having to memorize the IP addresses of ephemeral pods is error prone, because pods (and hence their
corresponding IP addresses) can be replaced at any time. Moreover, we have no way to balance the
load between our two identical nginx instances.

### Creating the service
It is time to create a `ClusterIP` service that will redirect and balance inbound traffic to any of
the pods.
```bash
$ kubectl apply -f services/nginx-service-clusterip.yaml
$ kubectl get svc -o wide
NAME         TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)    AGE     SELECTOR
kubernetes   ClusterIP   10.96.0.1       <none>        443/TCP    41h     <none>
nginx        ClusterIP   10.108.216.26   <none>        9000/TCP   3m20s   app=nginx,env=development
```
We can see that the service is exposed in the cluster internal IP `10.108.216.26`.

From within our `dnsutils` we can query nginx service, instead of having to query specific pod.
```bash
$ nslookup nginx
Server:		10.96.0.10
Address:	10.96.0.10#53

Name:	nginx.default.svc.cluster.local
Address: 10.108.216.26
$ curl nginx:9000
$ curl nginx.default.svc.cluster.local:9000
$ curl 10.108.216.26:9000
```

### Exposing the service
By creating a `NodePort` service, you make Kubernetes reserve a port on all its nodes (the same port
number is used across all of them) and forward incoming connections to the pods that are part of the
service. This is similar to a regular service, but a `NodePort` service can be accessed not only
through the service’s internal cluster IP, but also through the IP address and the reserved node
port of any node.
```bash
$ kubectl apply -f services/nginx-service-nodeport.yaml
```

Now the service should be accessible through the internal IP of the node itself.
```bash
$ kubectl get nodes -o wide
NAME       STATUS   ROLES                  AGE   VERSION   INTERNAL-IP    EXTERNAL-IP   OS-IMAGE             KERNEL-VERSION     CONTAINER-RUNTIME
minikube   Ready    control-plane,master   42h   v1.20.2   192.168.49.2   <none>        Ubuntu 20.04.1 LTS   5.10.25-linuxkit   docker://20.10.5
```

We can query the IP `192.168.49.2` at port `30090` and we shoulg get back an answer from our nginx
application. When using minikube you can access the service through your browser using
`minikube service <service-name>`. Notice that the service remains accessible through the same
ClusterIP address: **NodePort services extend on ClusterIP services**.

### A headless service
Finally we will create a headless nginx service. This is a `ClusterIP` service that does not have a
cluster IP address assigned. When resolving the DNS entries of a headless service we should get back
the IP address of all pods backing the service.
```bash
$ kubectl apply -f services/nginx-service-headless.yaml
$ kubectl get svc -o wide
NAME             TYPE        CLUSTER-IP   EXTERNAL-IP   PORT(S)  AGE     SELECTOR
nginx-headless   ClusterIP   None         <none>        80/TCP   4m23s   app=nginx,env=development
```

You can see that the service does not have an underlying cluster IP address allocated. Let's
connect back to our `dnsutils` pod and query the service.
```bash
$ kubectl exec -it dnsutils -- /bin/bash
$ nslookup nginx-headless
Name:	nginx-headless.default.svc.cluster.local
Address: 172.17.0.3
Name:	nginx-headless.default.svc.cluster.local
Address: 172.17.0.4
$
$ curl nginx-headless.default.svc.cluster.local
$ curl nginx-headless:80
```