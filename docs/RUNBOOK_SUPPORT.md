# 24x7 Global Support Runbook (sample)

## Follow-the-sun
| UTC | Pod |
|-----|-----|
| 00–12 | APAC |
| 06–18 | EMEA (overlap) |
| 12–24 | AMER |

## P1
1. Page bridge on-call
2. Check multi-region status page
3. Prefer canary rollback before full DR
4. If SQL primary down → `infra/vm/dr_failover.sh`

## Known SME gaps
Platform history lives with former CloudPeak engineers. See TRIBAL_KNOWLEDGE.md.

## HA targets
- API: 99.95% monthly
- Zero-downtime via blue-green (k8s) + VIP flip (VM)
