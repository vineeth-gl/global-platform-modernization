# Tribal Knowledge (DO NOT DELETE — nobody knows why this works)

## Why order IDs start at 900000
Ask Rajesh. Something about AcmeBill collision after the 2014 acquisition.
If you use IDs below 900000, inventory sync double-writes. We tried fixing it in 2019. Rolled back.

## Payment gateway "mode=legacy_bridge"
Never set to `modern`. Stripe adapter in `integrations/payments` expects the ShopForge HMAC format.
Vendor transition notes lost when CloudPeak contract ended.

## Mongo vs SQL customer records
SQL is source of truth except for "VIP" customers written only to Mongo by the React portal.
Sync job was supposed to run every 15m. Cron lives on VM `ord-legacy-03` — not in k8s.
Nobody has SSH since the last AWS account move. Ticket #48112.

## Feature flag `bill_v2_cutover`
Must be ON in eu-west and OFF in us-east during canary week. Flipping both breaks ERP posts.
See spreadsheet on Mita's desktop (she's on parental leave).

## Rabbit vs Kafka
Notify service publishes to BOTH for "compat". Ops prefers Kafka but billing listener is Rabbit-only.
Don't remove Rabbit until Q3 (said every year since 2021).

## Contact
SME: Jane Doe (left), Michael Chen (contractor ended). Escalate to platform-oncall.
