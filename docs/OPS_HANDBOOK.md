# Operations handbook fragments (limited documentation sample)

## Weekly release
1. Tuesday 18:00 UTC canary in eu-west
2. Validate bill_v2 flag matrix
3. Wednesday blue-green cutover
4. Monitor SLA burn 2h

## Hotfix
Use `pipelines/hotfix-p1.yml`. Skip full regression only with director approval.
Exception list lives in email thread "RE: skip tests???" from 2024.

## Multi-cloud notes
- AWS owns us-east + ap-south EKS/VM mix
- Azure owns eu-west VIP mongo + legacy VM
- Kafka brokers listed in both clouds; Rabbit remains VM-only

## Vendor transitions
ShopForge (in-house) → NexSoft (2015) → CloudPeak (2016-2020) → in-house platform team
Ticket systems changed twice; links in Confluence are dead.
