### Estimating Pi

```bash
$ kubectl apply -f pi/pi-job.yaml
$ kubectl get jobs
NAME     COMPLETIONS   DURATION   AGE
pi-job   0/5           6s         6s
```

When we apply a job that does not have the `spec.selector` field specified, Kubernetes automatically
generates appropriate labels for us.
```bash
$ kubectl get jobs pi-job -o yaml
apiVersion: batch/v1
kind: Job
spec:
  selector:
    matchLabels:
      controller-uid: 7c2e083b-f1c7-4925-bf4d-d78245858281
$
$ kubectl get pods <pod-name> -o yaml
apiVersion: v1
kind: Pod
metadata:
  labels:
    controller-uid: 7c2e083b-f1c7-4925-bf4d-d78245858281
  ownerReferences:
  - apiVersion: batch/v1
    blockOwnerDeletion: true
    controller: true
    kind: Job
    name: pi-job
    uid: 7c2e083b-f1c7-4925-bf4d-d78245858281
```

If we want to take ownership of the pod selection process (which is not adviced), we need to set
`spec.manualSelector: true` in the manifest. This flag tells the system that you know what you are
doing and to allow this behaviour.

```bash
$ kubectl delete jobs pi-job
```