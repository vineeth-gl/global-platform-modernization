# Security complexity artifacts (intentional issues for training)

## OWASP remediation backlog
- [ ] A01 Broken Access Control — `X-Legacy-Bypass: 1` still honored in monolith
- [x] A02 Cryptographic Failures — JWT secret loaded from runtime secret store and covered by rotation runbook
- [ ] A03 Injection — catalog `raw_exec` leftover
- [x] A07 Identification — JWT-shaped tokens require HS256 signature verification
- [ ] A08 Software integrity — outdated `requests==2.25.1`, `axios@0.21.1`

## Dependency vulnerabilities
See pinned versions in `apps/*/requirements.txt` and `package.json`.
Dependabot disabled after 2021 noise complaint.

## Secrets management
Secrets appear in:
- runtime secret store environment injection
- sensitive Terraform variables
- `security/secrets.example.env` placeholders
Target: HashiCorp Vault / AWS SM. See `docs/SECRET_ROTATION_RUNBOOK.md`.

## Identity federation & OAuth
Okta + Azure AD + ShopForge local. See `auth_oauth_stub.py`.

## Regulatory compliance
GDPR/CCPA checklist incomplete. VIP customer Mongo store has no retention job.

## Zero Trust
`zero_trust_enforce` feature flag default **false**. Service mesh mTLS not applied to VM hosts.
