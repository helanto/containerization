### Setting up node exporter
```bash
$ kubectl apply -f node-exporter/exporter.yaml
$ kubectl get ds
NAME            DESIRED   CURRENT   READY   UP-TO-DATE   AVAILABLE   NODE SELECTOR            AGE
node-exporter   1         1         1       1            1           kubernetes.io/os=linux   8s
```

Let's expose port 9100 and see the metrics.
```bash
$ kubectl port-forward node-exporter-hash 9000:9000
$ curl 127.0.0.1:9100/metrics
```

The node exporter is up and running! If we update the daemon set manifest and apply it again, we
should be able to observe the rolling update.
```
$ kubectl rollout status ds/node-exporter
Waiting for daemon set "node-exporter" rollout to finish: 0 out of 1 new pods have been updated...
Waiting for daemon set "node-exporter" rollout to finish: 0 of 1 updated pods are available...
daemon set "node-exporter" successfully rolled out
```