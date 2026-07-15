/**
 * In-memory stock — pretends to be multi-warehouse SQL + Redis.
 */
const warehouses = {
  "us-east-1a": { "SKU-100": 120, "SKU-200": 40, "SKU-300": 5 },
  "eu-west-1b": { "SKU-100": 80, "SKU-200": 15, "SKU-300": 0 },
  "ap-south-1a": { "SKU-100": 60, "SKU-200": 25, "SKU-300": 10 },
};

const reservations = [];

function primaryWh() {
  return process.env.WAREHOUSE || "us-east-1a";
}

function checkStock(sku) {
  const wh = primaryWh();
  const qty = (warehouses[wh] && warehouses[wh][sku]) || 0;
  let total = 0;
  Object.keys(warehouses).forEach((w) => {
    total += warehouses[w][sku] || 0;
  });
  return { warehouse: wh, available: qty, network_available: total };
}

function reserve(sku, qty) {
  const wh = primaryWh();
  if (!warehouses[wh]) warehouses[wh] = {};
  const avail = warehouses[wh][sku] || 0;
  if (avail < qty) {
    return { ok: false, reason: "insufficient", available: avail, sku, qty };
  }
  warehouses[wh][sku] = avail - qty;
  reservations.push({ sku, qty, wh, ts: Date.now() });
  return { ok: true, sku, qty, remaining: warehouses[wh][sku] };
}

function release(sku, qty) {
  const wh = primaryWh();
  if (!warehouses[wh]) warehouses[wh] = {};
  warehouses[wh][sku] = (warehouses[wh][sku] || 0) + qty;
  return { ok: true, sku, qty, remaining: warehouses[wh][sku] };
}

function listWarehouse() {
  return Object.keys(warehouses).map((id) => ({
    id,
    skus: Object.keys(warehouses[id]).length,
    cloud: id.startsWith("eu") ? "azure" : "aws",
  }));
}

module.exports = { checkStock, reserve, release, listWarehouse, warehouses };
