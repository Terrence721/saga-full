<!-- Status (Oct 2026): Login/auth entrypoints and the register/POS login flow were removed; the public site is order/reservation. Test totals: 314 backend, 69 Vitest/RTL + 4 Playwright E2E frontend. Historical sections below may predate these changes. -->
# 🚀 Kubernetes (Minikube) Runbook — saga-full

Manifests live under [`manifests/`](../manifests). This runbook builds the images *inside* Minikube (no registry required)
and applies the stack. Use a **local Minikube only** — there is no public registry target wired in yet (see
[`todo.md`](../todo.md) for the open cloud-deploy decision).

## Prerequisites

- Minikube + kubectl installed and working
- Docker (host side, to populate the Minikube image store)

## 1. Start a cluster

```bash
minikube delete
minikube start --driver=docker --memory=6144 --cpus=4
```

## 2. Point your shell at Minikube's Docker daemon

```bash
# bash/zsh:
eval $(minikube docker-env)

# PowerShell:
& minikube docker-env | Invoke-Expression
```

After this, `docker build` lands images inside the node, which is what the manifests reference with
`imagePullPolicy: IfNotPresent`.

## 3. Build every image (from repo root)

```bash
for svc in user-service order-service payment-service restaurant-service reservation-service api-gateway-service; do
  docker build -f $svc/Dockerfile -t saga-full/$svc:latest .
done
```

## 4. Real secret

Edit `manifests/ecosystem-secrets.yaml` (replace the `CHANGEME` values, ideally sourced from a Secret store in a real
cluster) before applying. Do **not** commit real secrets (`ecosystem-secrets.yaml` is intentionally a template).

## 5. Apply in order

```bash
cd manifests
kubectl apply -f namespace-setup.yaml
kubectl apply -f cluster-configmap.yaml
kubectl apply -f ecosystem-secrets.yaml
kubectl apply -f postgres-manifest.yaml
# wait for postgres
kubectl wait -n saga-ecosystem --for=condition=ready pod -l app=postgres-db --timeout=120s
kubectl apply -R -f apps/
```

## 6. Verify

```bash
kubectl get pods -n saga-ecosystem
kubectl get svc -n saga-ecosystem
kubectl port-forward -n saga-ecosystem svc/api-gateway-service 8090:8080
curl -s http://localhost:8090/actuator/health
```

## Caveats

- `manifests/monitoring/*` and `network-policies/*` are imported verbatim from the source platform; the Cilium
  network policies assume Cilium is installed as the cluster CNI. On a default Minikube (Docker driver + kindnet)
  they will be rejected unless Cilium is installed — skip them with `kubectl apply -f manifests/apps/` if you don't
  need them.
- `user-service` is gRPC-only; its `9090` port is the gRPC listener.
- Images use `imagePullPolicy: IfNotPresent`; if you rebuild an image with the same tag, delete the pod to pick it up.
