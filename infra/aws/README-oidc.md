Steps to create an IAM role for GitHub Actions OIDC and configure the repo

1. Create the role using the trust policy

```bash
aws iam create-role \
  --role-name GitHubActionsOIDCRole \
  --assume-role-policy-document file://infra/aws/github-oidc-trust-policy.json
```

2. Attach the minimal permissions policy

```bash
aws iam put-role-policy \
  --role-name GitHubActionsOIDCRole \
  --policy-name OIDCDeployPolicy \
  --policy-document file://infra/aws/permissions-policy.json
```

3. Map the role into the cluster's `aws-auth` ConfigMap (gives Kubernetes RBAC)

Edit the `aws-auth` ConfigMap in the `kube-system` namespace and add:

```yaml
mapRoles: |
  - rolearn: arn:aws:iam::869657602774:role/GitHubActionsOIDCRole
    username: github-actions
    groups:
      - system:masters
```

4. Add the role ARN to GitHub Secrets (repository or organization) as `AWS_ROLE_TO_ASSUME`.

5. Workflow change: the deploy workflow is already patched to use `role-to-assume`, so no further changes required.

Security note: mapping to `system:masters` grants full cluster-admin. Prefer mapping to a limited RBAC group if possible.
