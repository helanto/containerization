### A multi-container application

First, you need to point your terminal to use the docker daemon inside minikube. This way the docker
commands you run in this terminal will run against the docker engine inside minikube cluster.
```bash
$ eval $(minikube docker-env)
```

Now, if you run `docker ps -a`, it will show you the containers inside the minikube container
itself. It is time to build the two docker files.
```bash
$ docker build -f web-server/server/Dockerfile -t k8s/pods-server:latest .
$ 
$ cd web-server/multi-container/statichtml
$ docker build -t k8s/pods-static:latest .
```

In this example we serve html files which are baked into the k8s/pods-static:latest image. We create
a volume inside the pod which is used for file sharing between the containers. Then we mount the
volume into the two containers. Finally, we apply the pod manifest and expose port `9000`.
```bash
$ kubectl apply -f web-server/multi-container/pod.yaml
$ kubectl port-forward webserver-multicontainer 9000:9000
$ curl 127.0.0.1:9000
```