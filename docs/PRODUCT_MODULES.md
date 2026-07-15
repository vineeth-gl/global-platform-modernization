# Product modules in this sample (5 of 25+)
1. Orders — monolith order_service + workflow
2. Customers — SQL + Mongo VIP split
3. Inventory — svc-inventory (+ forecast)
4. Billing — AcmeBill svc-billing (+ rating)
5. Notifications — svc-notify Kafka/Rabbit dual

Supporting: Tax sidecar, Returns, Shipping, Support tickets, Merchandising, Cart.
