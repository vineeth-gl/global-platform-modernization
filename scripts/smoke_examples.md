# Smoke examples (Java 8 monolith + Node services)

```bash
export TOKEN=dev-admin

# Create order (monolith Java 8)
curl -s -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -H "X-Region: eu-west" \
  -d '{"customer_id":"C-1001","email":"buyer@example.com","lines":[{"sku":"SKU-100","qty":1}]}'

# Inventory check (Node)
curl -s "http://localhost:3001/api/stock/check?sku=SKU-100"

# Billing v1 Acme (Java 8)
curl -s -X POST http://localhost:5001/v1/charge -H "Content-Type: application/json" \
  -d '{"OrderId":900001,"Amount":19.99,"Cur":"USD"}'

# Tax sidecar (Java 8)
curl -s -X POST http://localhost:5002/tax/compute -H "Content-Type: application/json" \
  -d '{"country":"DE","subtotal":100}'

# Notify (Node)
curl -s -X POST http://localhost:3002/notify/order-created -H "Content-Type: application/json" \
  -d '{"order_id":900001,"email":"buyer@example.com","region":"us-east"}'
```
