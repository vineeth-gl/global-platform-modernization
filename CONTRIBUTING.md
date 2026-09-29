# Contributing

## Prerequisites
- JDK 8+ (Java 17 works for local runs) and Maven 3.8+
- Node.js 18+ (for `svc-inventory` and `svc-notify`)
- Docker (optional) for `docker compose`

## Run everything with Docker
```bash
docker compose up --build
```
Ports: monolith 8080, billing 5001, tax 5002, inventory 3001, notify 3002, legacy portal 8088, modern portal 8089.

## Build and run locally
```bash
mvn -q package -DskipTests                                   # builds monolith-core, svc-billing, svc-tax
java -jar apps/monolith-core/target/monolith-core-*-LEGACY.jar  # :8080
java -jar apps/svc-billing/target/svc-billing-*-LEGACY.jar      # :5001
java -jar apps/svc-tax/target/svc-tax-*-LEGACY.jar              # :5002
cd apps/svc-inventory && npm install && npm start               # :3001
cd apps/svc-notify && npm install && npm start                  # :3002
```
Run each service in its own terminal. Local APIs expect `Authorization: Bearer dev-admin`.
See `scripts/smoke_examples.md` for sample requests.

## Before opening a PR
- There is no automated test suite; smoke-test the services you touched.
- `target/` build output is tracked in git, so don't commit rebuilt jars unless you mean to.
