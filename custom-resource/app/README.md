# Person operator
This is an example of creating a scala-based operator for Person CRDs.

### Run the operator locally
Any operator needs elevated access to a Kubernetes cluster. An operator is not a standalone application; it needs information about the environment it is running in. It should
be able to `list`, `get` and `watch` other resources. It is also common for an operator to `create` or `delete` resources.

When running the operator locally, one can simply use `kubectl proxy` command to enable access to K8s API server.
```bash
$ kubectl proxy --port=8080
```

Since the proxy takes care of _authentication / authorization_, one can simply run the operator without providing access tokens or CA root certificates.
```bash
$ sbt clean "run --host 127.0.0.1 --protocol http --port 8080"
```

### Build
As a first step, we will build a base `sbt` image.
```bash
docker build -t sbt:1.5.5 -f ./docker/sbt/Dockerfile .
```

We can simply run our application on the fly, using `sbt run` command. The downside of this approach is that we have to compile the
application code every time we run the image.
```
ENTRYPOINT sbt clean run
CMD --host 127.0.0.1 --protocol http --port 8080
```

As an alternative we will pre-assemble the code and bake it as a fat jar into the image.
```bash
docker build -t person-operator:0.1 -f ./docker/build/Dockerfile .
docker run --rm --env JVM_OPTS=-Xmx1024m heliasantoniou/person-operator:0.1 http://host.docker.internal:8080
docker run --rm --env JVM_OPTS=-Xmx1024m --network="host" person-operator:0.1 http host.docker.internal 8080
```

### Notes
#### A namespaced CRD
Upon close examination, one will notice that the `spec.scope` field in the custom resource definition is set to `Namespaced`. What does it really mean ?
It should be clear that the resource definition itself is **cluster-wide** and **available in all namespaces**. You cannot define a `person` resource in one
namespace and a different `person` resource in another namespace. **Any resource definition should be unique across the cluster**.

However, the resource itself can be either scoped to a namespace (e.g. you can deploy the same pod in different namespaces) or be cluster-wide. Usually
low-level resources, such as *nodes* or *namespaces* do not belong to any namespace. You can check which resources are namespaced and which are not using the `kubectl api-resources` command.
```bash
$ kubectl api-resources --namespaced=true
NAME                        SHORTNAMES   APIVERSION                     NAMESPACED   KIND
persons                                  extensions.helanto.co/v1       true         Person
...
```

#### ClusterRole vs Role
In our example, a **cluster role** and a **cluster role binding** have been used. Using a role with cluster-wide permissions,
enables the operator to watch for events and create resources in all namespaces. One can notice that the operator queries `/apis/extensions.helanto.co/v1/persons` instead of
`/apis/extensions.helanto.co/v1/namespaces/$ns/persons`. Since we are working solely with the `default` namespace using a cluster role is not
necessary. In the vast majority of cases however, operators need to watch for events in all namespaces and require cluster-wide permissions.

#### Debugging Helm
You can inspect the templates, before submitting to API server using `helm template` or `helm install` commands, as in the following.
```bash
$ helm template --debug person-operator ./custom-resource/app/chart/person-operator
$ helm install --dry-run --debug person-operator ./custom-resource/app/chart/person-operator
```
