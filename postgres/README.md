## Kustomize
An application is a group of Kubernetes resources / objects related by some common purpose, e.g. a
load balancer in front of a web-server backed by a database. You want to deploy and manage your
application as a whole and not having to apply, update or delete every resource separately. Resource
labelling, naming and metadata schemes have historically served to group resources together for
collective operations like list and remove. [Kustomize](https://kubectl.docs.kubernetes.io/references/kustomize/)
configures and manages Kubernetes resources as a whole.

In its simplest form, kustomize groups a set of Kubernetes resources together. For example, in a
database application, we could have a database definition along with a service and some configuration
values.
> ```
> kustomization/postgres
> ├── database.yaml
> ├── postgres-configmap.yaml
> └── service.yaml
> ```

Using kustomize we can group all resources together, in a `kustomization.yaml` file. The resources
declared in the kustomization file need to be relative to the file itself.
```
# postgres/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - database.yaml
  - service.yaml
  - postgres-configmap.yaml
```

Kustomize does more than simply grouping resources together into a coherent group: it provides a set
of instructions for modifying or customizing the resources. For example we can apply common labels,
name prefixes and more. The key idea is never to update the resource files directly; but instead
dynamically change the behaviour by applying customization to create new resource definitions.
```bash
$ cd postgres
$ kustomize create
$ kubectl kustomize . | kubectl apply -f -
$ kustomize build . | kubectl apply -f -
```

```bash
$ kubectl run postgres-client --rm -i --tty --image=jbergknoff/postgresql-client -- postgresql://helias:helias@postgres-headless-dev:5432/db
db=# \dt;
db=# CREATE TABLE pair (key INTEGER PRIMARY KEY, value INTEGER NOT NULL);
db=# INSERT INTO pair VALUES (1, 100), (2, 200), (3, 300);
```

```bash
$ kubectl run postgres-client -i --tty --image=bitnami/postgresql:12.7.0 --rm --namespace default --env="PGPASSWORD=helias" --command -- psql --host postgres-headless-dev -U helias -d db -p 5432
db=# UPDATE pair SET value = value + 1 WHERE key = 1;
```

Kustomize provides a mechanism to use an existing Kustomize project as a base project to extend on.
It’s generally intended for customising deployments per target environment. This way you can import / re-use
applications and resource definitions and customize them as you see fit. For example, the `patchesStrategicMerge`
directive allows partial YAML files to be provided, which are then patched on top of resources of
the same group, version, kind and name.