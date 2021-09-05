## Creating custom resources

### Custom resource definition
More often than not, your application logic dictates to work with higher-level objects than the ones already exposed by Kubernetes. We already defined [a resource](../README.md#terminology) as *the use of a kind* or *an endpoint* in the Kubernetes API. A **custom resource** is an extension of that API. A **custom resource definition** (`CRD` for short) defines a custom resource; it specifies its name, group, version, schema and more. To define a new resource type, all you need to do is post a `CustomResourceDefinition` object to the Kubernetes API server describing the resource you want to create. Once the CRD is posted, users can then create instances of the custom resource by posting JSON or YAML manifests to the API server, the same as with any other Kubernetes resource.
```bash
$ kubectl get customresourcedefinitions.apiextensions.k8s.io
No resources found
$ kubectl apply -f custom-resource/crd.yaml
customresourcedefinition.apiextensions.k8s.io/persons.extensions.helanto.co created
$ kubectl get customresourcedefinitions.apiextensions.k8s.io
NAME                            CREATED AT
persons.extensions.helanto.co   2021-XX-XXTXX:XX:XXZ
```

Let’s go over everything you have done. By creating a `CustomResourceDefinition` object, you can now store, retrieve, and delete custom objects through the Kubernetes API server. These objects don’t do anything yet. You’ll need to create a controller to make them do something meaningful.
```bash
$ kubectl apply -f custom-resource/person1.yaml
person.extensions.helanto.co/person-helias created
$ kubectl get persons
NAME            AGE
person-helias   4s
```

Creating a CRD so that users can create objects of the new type is not a useful feature if those objects don’t make something tangible happen in the cluster. Each CRD will usually also have an associated **controller**. It is the active component that takes application-specific actions to make reality match the desired state described by those custom resources. It usually watches those high-level objects and create low-level ones.

### Controller
Before moving forward we will clean up the progress made so far. That is because we will deploy everything from scratch as part of the controller deployment.
```bash
$ kubectl delete persons.extensions.helanto.co person-helias
$ kubectl delete customresourcedefinitions.apiextensions.k8s.io persons.extensions.helanto.co
```

As part of this example we create a controller that acts upon the creation of a `Person` resource, by creating the corresponding configmap. The controller is just a `Scala` application, deployed as any other Kubernetes application: using pods and deployments. The operator requires elevated access to the API server, since it needs to watch changes and apply them to the cluster. We will use a [Helm chart](https://helm.sh/) to package and manage the controller. Notice that the chart contains the custom resource definition.

```bash
$ helm install person-operator ./custom-resource/app/chart/person-operator
```

You will notice that the controller is deployed as a pod.
```bash
$ kubectl get pods
NAME                               READY   STATUS    RESTARTS   AGE
person-operator-56958b646f-jzffm   1/1     Running   0          5s
$ ...
$ stern person-operator
person-operator-56958b646f-jzffm person-operator API Server: https://kubernetes:443
person-operator-56958b646f-jzffm person-operator Header: 'Authorization: Bearer eyJhbGciOiJS...hOGgmA'
person-operator-56958b646f-jzffm person-operator Building new HTTP client for https://kubernetes:443
```

Now every time we deploy a `Person` resource, a corresponding configmap is being created.
```bash
$ kubectl apply -f custom-resource/person1.yaml
$ kubectl get persons
NAME            AGE
person-helias   16s
$ kubectl get configmaps
NAME                  DATA   AGE
person-helias-cfmap   1      9s
$ kubectl get configmap person-helias-cfmap -o yaml
apiVersion: v1
data:
  person.json: '{"first-name":"Helias","last-name":"Antoniou","age":30}'
kind: ConfigMap
metadata:
  creationTimestamp: "2021-10-17T13:20:18Z"
  name: person-helias-cfmap
  namespace: default
  ownerReferences:
  - apiVersion: extensions.helanto.co/v1
    kind: Person
    name: person-helias
    uid: 31ae8465-356e-4dc5-ad82-ead221a12dc8
  resourceVersion: "126449"
  uid: 55675d54-aef1-412c-b967-412c9ccddcbd
```

Since the configmap is owned by the `Person` resource, it will be auto-removed when the owner is deleted.
```bash
$ kubectl delete person person-helias
$ kubectl get configmaps
No resources found in default namespace.
```