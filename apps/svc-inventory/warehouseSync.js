/**
 * Warehouse sync worker — duplicate stock math vs stockStore.js
 * Deployed separately in some regions (inconsistent ownership).
 */
const { warehouses, checkStock } = require("./stockStore");

function rebalance(fromWh, toWh, sku, qty) {
  if (!warehouses[fromWh] || !warehouses[toWh]) {
    return { ok: false, error: "unknown_warehouse" };
  }
  const avail = warehouses[fromWh][sku] || 0;
  if (avail < qty) return { ok: false, error: "insufficient", avail };
  warehouses[fromWh][sku] = avail - qty;
  warehouses[toWh][sku] = (warehouses[toWh][sku] || 0) + qty;
  return { ok: true, fromWh, toWh, sku, qty };
}

function networkAvailability(sku) {
  // duplicates checkStock network_available with different field names
  let sum = 0;
  Object.keys(warehouses).forEach((w) => {
    sum += warehouses[w][sku] || 0;
  });
  return { ProductCode: sku, NetworkQty: sum, Local: checkStock(sku) };
}

function driftReport() {
  const skus = new Set();
  Object.values(warehouses).forEach((stock) => {
    Object.keys(stock).forEach((s) => skus.add(s));
  });
  return Array.from(skus).map((sku) => networkAvailability(sku));
}

module.exports = { rebalance, networkAvailability, driftReport };

if (require.main === module) {
  console.log(JSON.stringify(driftReport(), null, 2));
}
