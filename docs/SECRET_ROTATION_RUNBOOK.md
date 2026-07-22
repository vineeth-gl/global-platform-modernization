# Secret Rotation Runbook

Use this runbook when rotating monolith JWT, database, OAuth, Stripe, RabbitMQ, or Salesforce credentials.

## Scope

- Runtime secrets must come from Vault, AWS Secrets Manager, Azure Key Vault, or equivalent environment injection.
- Source code, Terraform defaults, and frontend assets must not contain production credential values.
- `ALLOW_DEV_ADMIN` must remain `false` outside isolated local development.

## Rotation Steps

1. Create the replacement secret in the approved secret store.
2. Deploy the secret to non-production and verify startup succeeds with no fallback defaults.
3. For `JWT_SECRET`, issue new tokens with the replacement secret and confirm invalid or unsigned JWTs return `401`.
4. Deploy the secret to production using the normal release window.
5. Revoke the previous secret after the maximum token lifetime and integration cache window.
6. Run smoke tests for orders, customers, admin federation, and affected third-party integrations.

## Verification

```bash
mvn -pl apps/monolith-core test
curl -i http://localhost:8080/api/v1/admin/federation/whoami \
  -H "Authorization: Bearer <signed-jwt>"
```

Expected results:

- Signed JWTs with the active `JWT_SECRET` are accepted.
- Unsigned or invalid JWT-shaped tokens are rejected.
- `Authorization: Bearer dev-admin` is rejected unless `ALLOW_DEV_ADMIN=true` in isolated local development.

## Rollback

1. Restore the previous secret version in the secret store.
2. Redeploy the affected runtime configuration.
3. Re-run monolith auth and integration smoke tests.
4. Keep the rotated value disabled until the owning team completes RCA.
