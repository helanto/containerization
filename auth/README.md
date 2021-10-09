### Authentication / Authorization in practice
In this example we are going to create a pod, and query API server taking care of authentication / authorization. The only requirement is to use a container image that contains the `curl` binary for the pod. First we need to apply the manifests in the cluster.
```bash
$ cd auth
$ kubectl kustomize . | kubectl apply -f -
```

We will attach to the pod and query the API server using `curl`.
```bash
$ kubectl exec -it ubuntu-dev -- /bin/bash
root@ubuntu:/# ls /var/run/secrets/kubernetes.io/serviceaccount/
ca.crt  namespace  token
root@ubuntu:/# export CURL_CA_BUNDLE=/var/run/secrets/kubernetes.io/serviceaccount/ca.crt
root@ubuntu:/# NS=$(cat /var/run/secrets/kubernetes.io/serviceaccount/namespace)
root@ubuntu:/# TOKEN=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)
root@ubuntu:/# curl -H "Authorization: Bearer $TOKEN" https://kubernetes:443/api/v1/namespaces/$NS/pods
{
  "kind": "PodList",
  "apiVersion": "v1",
  ...
root@ubuntu:/# curl -H "Authorization: Bearer $TOKEN" https://kubernetes:443/api/v1/namespaces/$NS/configmaps
{
  "kind": "Status",
  "apiVersion": "v1",
  "message": "configmaps is forbidden: User \"system:serviceaccount:default:serviceaccount-name-dev\" cannot list resource \"configmaps\" in API group \"\" in the namespace \"default\"",
  "code": 403
}
```