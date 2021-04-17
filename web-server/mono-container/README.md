### A mono-container application

First, you need to point your terminal to use the docker daemon inside minikube. This way the docker
commands you run in this terminal will run against the docker engine inside minikube cluster.
```bash
$ eval $(minikube docker-env)
```

Now, if you run `docker ps -a`, it will show you the containers inside the minikube container
itself. It is time to build the server docker image.
```bash
$ docker build -f web-server/server/Dockerfile -t k8s/pods-server:latest .
```

In this example we deploy the static html files using a config map.  The configuration contains two
fields:
```yaml
data:
  port: "9100"
  index.html: |
    <html>...</html>
```

The `data.port` field of the config map will be used as argument when we will spin the container.
Keep in mind that `spec.container[].args` field overrides the default `CMD` in image definition.
Before overriding the container arguments, we need to create an environmental variable in the pod 
manifest.
```yaml
spec:
  containers:
    - image: k8s/pods-server:latest
    name: static-server
    # Injects config map as environmental variable.
    env:
    # The name of the environmental variable
    - name: WEBSERVER_PORT
      valueFrom:
        configMapKeyRef:
          # The ConfigMap this value comes from.
          name: webserver-configuration
          # The key to fetch.
          key: port
    # Overrides CMD in image definition.
    args: ["$(WEBSERVER_PORT)"]
```

Then we need to mount the `data.index.html` field of the config map. This needs to be an html file
located under `/server`.
```yaml
spec:
  containers:
    - image: k8s/pods-server:latest
    name: static-server
    volumeMounts:
    - mountPath: /server
      # The name of the volume to mount into the container.
      # Needs to be specified in the `spec.volumes`
      name: config-volume

  # Set volumes at the pod level, then mount them into containers inside that pod.
  volumes:
  - name: config-volume
    configMap:
      # The name of the ConfigMap the value comes from.
      name: webserver-configuration
      # An array of keys from the ConfigMap to create as files.
      items:
      - key: index.html
        path: index.html
```

Now it is time to deploy the web-server. We deploy first the configuration and then start the pod.
```bash
$ kubectl apply -f web-server/mono-container/server-configmap.yaml
$ kubectl apply -f web-server/mono-container/pod.yaml
```

As expected, the server responds normally at port configured port `9100`.
```bash
$ kubectl port-forward webserver-singlecontainer 9100:9100
$ curl 127.0.0.1:9100
```