# Compliance helpers (checklist) — regulatory sample

## Retention (days)
- US/GB: 2555
- DE: 3650
- IN: 1825
- DEFAULT: 2555

## Gaps (intentional)
- VIP Mongo store has no retention job
- CCPA “do not sell” only propagates to Segment/HubSpot; ERP is manual
- GDPR export may miss Mongo VIP rows when looking up by SQL id

See also `security/OWASP_BACKLOG.md` and `security/zero_trust_policy.yaml`.
