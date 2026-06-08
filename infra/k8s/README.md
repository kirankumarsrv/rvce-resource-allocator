# Kubernetes Bootstrap for rvce-resource-allocator

> Note: These Kubernetes manifests live in the `deployment` branch. They are not present on `main` until this branch is merged.
>
> If you checkout `main`, the path `infra/k8s/production-frontend-deployment.yaml` will not exist.

This folder contains a minimal Kubernetes bootstrap pack for your EKS cluster. It creates the basic staging and production deployments, services, and ingress rules required for your GitHub Actions deployment workflow.

## What is included

- `staging-backend-deployment.yaml` — staging backend deployment and service
- `staging-frontend-deployment.yaml` — staging frontend deployment and service
- `production-backend-deployment.yaml` — production backend deployment and service
- `production-frontend-deployment.yaml` — production frontend deployment and service
- `staging-ingress.yaml` — host-based ingress for staging
- `production-ingress.yaml` — host-based ingress for production

## Why this is useful

Your current workflow uses `kubectl set image` on existing deployments. These manifests ensure the required resources already exist in the cluster.

The deployments use `strategy.type: Recreate` to avoid rollout deadlocks on small clusters.

## Apply the bootstrap

Run these commands in CloudShell after connecting to the cluster with `aws eks update-kubeconfig`:

```bash
kubectl apply -f infra/k8s/staging-backend-deployment.yaml
kubectl apply -f infra/k8s/staging-frontend-deployment.yaml
kubectl apply -f infra/k8s/production-backend-deployment.yaml
kubectl apply -f infra/k8s/production-frontend-deployment.yaml
```

## Install ingress-nginx

Your cluster currently has no ingress controller. Install nginx ingress with Helm:

```bash
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
helm install ingress-nginx ingress-nginx/ingress-nginx --create-namespace --namespace ingress-nginx
```

Wait until the ingress controller is ready:

```bash
kubectl rollout status deployment/ingress-nginx-controller -n ingress-nginx --timeout=5m
```

## Create the ingress resources

```bash
kubectl apply -f infra/k8s/staging-ingress.yaml
kubectl apply -f infra/k8s/production-ingress.yaml
```

## Find the external hostname

After the ingress controller is installed, get the external address:

```bash
kubectl get svc ingress-nginx-controller -n ingress-nginx
```

The `EXTERNAL-IP` or hostname is the address you will point your DNS records to.

## DNS setup

Create DNS records that point the application hosts to the ingress controller's external load balancer:

- `staging.scas.rvce.edu.in` → ingress controller external hostname
- `scas.rvce.edu.in` → ingress controller external hostname

For a quick test without DNS, use `curl --resolve` with the LB hostname.

## Verify staging

```bash
kubectl get all -n scas-staging
kubectl get ingress -n scas-staging
kubectl get all -n ingress-nginx
```

Then test:

```bash
curl -v --resolve staging.scas.rvce.edu.in:443:<LB_HOST> https://staging.scas.rvce.edu.in/actuator/health
```

## Notes

- These manifests are bootstrap-only. Your GitHub Actions workflow will update the images later.
- If your apps require real secrets or config, add those as Kubernetes `Secret` or `ConfigMap` resources before deploying the real images.
- If you prefer a simpler quick test, you can change `scas-frontend` to `type: LoadBalancer` instead of using ingress.
