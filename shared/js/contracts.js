/**
 * Same contracts duplicated for Node CI jobs
 */
const CREATE_ORDER_REQUEST = {
  customer_id: "C-1001",
  email: "buyer@example.com",
  country: "US",
  lines: [{ sku: "SKU-100", qty: 2, unit_price: 19.99 }],
  source: "contract-test",
};

const RESERVE_STOCK_REQUEST = { sku: "SKU-100", qty: 1, request_id: "t-1" };
const BILLING_V1_REQUEST = { OrderId: 900001, Amount: 19.99, Cur: "USD" };
const NOTIFY_ORDER_CREATED = { order_id: 900001, email: "buyer@example.com", region: "us-east" };

function assertHasKeys(obj, keys) {
  const missing = [];
  keys.forEach((k) => {
    if (obj == null || obj[k] === undefined) missing.push(k);
  });
  return missing;
}

module.exports = {
  CREATE_ORDER_REQUEST,
  RESERVE_STOCK_REQUEST,
  BILLING_V1_REQUEST,
  NOTIFY_ORDER_CREATED,
  assertHasKeys,
};
