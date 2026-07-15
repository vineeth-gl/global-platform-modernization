# Global Platform Modernization — DEMb Sample

> Intentionally complex ~5K LOC enterprise sample that mirrors real-world technical and business complexity (scaled down from a ~2M LOC estate).  
> **Not** a best-practice reference architecture — debt, duplication, and brittle ops are deliberate for training and assessment.

**Remote:** [vineeth-gl/global-platform-modernization](https://github.com/vineeth-gl/global-platform-modernization)

---

## What this project is

A hybrid **monolith + microservices** commerce/platform sample with:

| Area | What you get |
|------|----------------|
| **Orders & customers** | Java 8 SparkJava monolith (`apps/monolith-core`) |
| **Billing** | Acquired AcmeBill service (`apps/svc-billing`) |
| **Tax** | Sidecar service (`apps/svc-tax`) |
| **Inventory** | Node.js Express service (`apps/svc-inventory`) |
| **Notifications** | Node.js service with Kafka/Rabbit stubs (`apps/svc-notify`) |
| **UI** | Legacy jQuery portal + modern React portal |
| **Infra** | Multi-cloud Terraform, Kubernetes, VM layouts, CI/CD pipelines |
| **Integrations** | CRM / ERP / Payments / Marketing / Analytics adapters |
| **Security & ops** | OWASP backlog, Zero Trust WIP, runbooks, SLA notes |

Use it to explore modernization scenarios: strangler fig, service extraction, CI/CD hardening, security remediation, and knowledge transfer.

---

## Complexity map (sample vs production-scale)

| Dimension | This sample | Mirrors |
|-----------|-------------|---------|
| LOC | ~5,000 (`scripts/count_loc.ps1`) | ~2M |
| Product modules | 5 | 25+ |
| Microservices | 5 | 180+ |
| APIs | ~40 | 3,500+ |
| Databases | 2 (SQL + Mongo) | 30+ |
| Third-party integrations | 5 | 200+ |
| CI/CD pipelines | 6 | 120+ |
| Releases | Simulated weekly + hotfix | 50+/month |
| Environments | AWS + Azure, k8s + VM | Multi-region, multi-cloud |

Full mapping: [`docs/COMPLEXITY.md`](docs/COMPLEXITY.md)

---

## Repository layout

```
apps/
  monolith-core/     # Java 8 SparkJava — orders, customers, workflows
  svc-billing/       # Java 8 — AcmeBill billing / rating
  svc-tax/           # Java 8 — tax sidecar
  svc-inventory/     # Node.js — inventory + forecast
  svc-notify/        # Node.js — notifications (Kafka/Rabbit stubs)
  legacy-portal/     # jQuery / static legacy UI
  modern-portal/     # React portal
data/                # Sample SQL / Mongo fixtures
docs/                # Complexity, runbooks, SLA, tribal knowledge
infra/               # terraform-aws, terraform-azure, k8s, vm
integrations/        # CRM / ERP / payments / marketing / analytics notes
pipelines/           # Weekly, hotfix, canary, and service CD YAML
security/            # OWASP backlog, compliance, Zero Trust policy
shared/              # Shared validators, feature flags, duplication examples
scripts/             # LOC counting, smoke examples
docker-compose.yml   # Local hybrid stack
pom.xml              # Maven parent (Java 8 modules)
```

---

## Technology stack

| Layer | Choices (intentional legacy mix) |
|-------|----------------------------------|
| Backend | **Java 8** (SparkJava), **Node.js** (Express) |
| Frontend | Legacy portal (jQuery) + React (`modern-portal`) |
| Data | SQL + MongoDB VIP split |
| Messaging | Kafka / RabbitMQ stubs in notify |
| Cloud | AWS + Azure Terraform under `infra/` |
| Deploy | Kubernetes manifests + VM layouts |
| CI/CD | Pipelines under `pipelines/` |

Changelog dating to 2010 and acquired code paths (`AcmeBill`, `ShopForge`) live under [`docs/CHANGELOG_LEGACY.md`](docs/CHANGELOG_LEGACY.md).

---

## Prerequisites

- **JDK 8+** (Java 17 works for local runs) and **Maven 3.8+**
- **Node.js 18+** for inventory and notify
- **Docker** (optional) for `docker compose`

---

## Quick start

### Option A — Docker Compose (all services)

```bash
docker compose up --build
```

| Service | Port |
|---------|------|
| Monolith | `8080` |
| Inventory | `3001` |
| Billing | `5001` |
| Notify | `3002` |
| Tax | `5002` |
| Legacy portal | `8088` |
| Modern portal | `8089` |

### Option B — Local build & run

```bash
# Build Java 8 modules
mvn -q package -DskipTests

# Monolith
java -jar apps/monolith-core/target/monolith-core-*-LEGACY.jar

# Billing (AcmeBill)
java -jar apps/svc-billing/target/svc-billing-*-LEGACY.jar

# Tax sidecar
java -jar apps/svc-tax/target/svc-tax-*-LEGACY.jar

# Inventory
cd apps/svc-inventory && npm install && node server.js

# Notify
cd apps/svc-notify && npm install && node index.js
```

**Auth token for local APIs:** `Authorization: Bearer dev-admin`

Step-by-step start/test guide: [`docs/START_TEST_AND_ISSUES.md`](docs/START_TEST_AND_ISSUES.md)

---

## Product modules

1. **Orders** — monolith order service + workflow  
2. **Customers** — SQL + Mongo VIP split  
3. **Inventory** — `svc-inventory` (+ forecast)  
4. **Billing** — AcmeBill `svc-billing` (+ rating)  
5. **Notifications** — `svc-notify` Kafka/Rabbit dual  

Supporting: tax sidecar, returns, shipping, support tickets, merchandising, cart.  
Details: [`docs/PRODUCT_MODULES.md`](docs/PRODUCT_MODULES.md)

---

## Documentation

| Doc | Contents |
|-----|----------|
| [`docs/COMPLEXITY.md`](docs/COMPLEXITY.md) | Technical & business complexity mapping |
| [`docs/START_TEST_AND_ISSUES.md`](docs/START_TEST_AND_ISSUES.md) | How to start, test APIs, known issues |
| [`docs/OPS_HANDBOOK.md`](docs/OPS_HANDBOOK.md) | Operations handbook |
| [`docs/RUNBOOK_SUPPORT.md`](docs/RUNBOOK_SUPPORT.md) | Support runbook |
| [`docs/SLA.md`](docs/SLA.md) | SLA targets |
| [`docs/TRIBAL_KNOWLEDGE.md`](docs/TRIBAL_KNOWLEDGE.md) | Incomplete tribal / SME notes |
| [`security/COMPLIANCE.md`](security/COMPLIANCE.md) | Compliance checklist |
| [`security/OWASP_BACKLOG.md`](security/OWASP_BACKLOG.md) | Security remediation backlog |
| [`scripts/smoke_examples.md`](scripts/smoke_examples.md) | Smoke / API examples |

---

## Intentional debt (do not “fix” blindly)

- God-objects, global singletons, sync REST chains  
- Duplicated validators across Java and Node (`shared/`)  
- Hardcoded service URLs and tight coupling  
- Secrets in code, missing OWASP fixes, brittle SLAs  

These exist for **training / assessment** scenarios. See [`docs/TRIBAL_KNOWLEDGE.md`](docs/TRIBAL_KNOWLEDGE.md) and [`security/`](security/).

---

## Disclaimer

Secrets in repositories, incomplete OWASP fixes, and brittle SLAs are **deliberate**. Treat this codebase as a modernization playground, not production software.
