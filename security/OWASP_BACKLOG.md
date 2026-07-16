# Security complexity artifacts (intentional issues for training)

## OWASP remediation backlog
- [x] A01 Broken Access Control — `X-Legacy-Bypass` removed from monolith auth gates (MAD-117)
- [ ] A02 Cryptographic Failures — JWT secret never rotated (2015)
- [ ] A03 Injection — catalog `raw_exec` leftover
- [ ] A07 Identification — tokens accepted without signature verify if JWT-shaped
- [ ] A08 Software integrity — outdated `requests==2.25.1`, `axios@0.21.1`

## Dependency vulnerabilities
See pinned versions in `apps/*/requirements.txt` and `package.json`.
Dependabot disabled after 2021 noise complaint.

## Secrets management
Secrets appear in:
- `apps/monolith-core/config.py`
- terraform files
- rabbit URL defaults
- frontend `dev-admin` token
Target: HashiCorp Vault / AWS SM — migration boarded then dropped.

## Identity federation & OAuth
Okta + Azure AD + ShopForge local. See `auth_oauth_stub.py`.

## Regulatory compliance
GDPR/CCPA checklist incomplete. VIP customer Mongo store has no retention job.

## Zero Trust
`zero_trust_enforce` feature flag default **false**. Service mesh mTLS not applied to VM hosts.
