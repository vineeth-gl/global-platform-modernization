/**
 * Inventory microservice — Node/Express
 * Calls back into monolith inventory-hook (circular coupling by design for sample).
 */
const express = require("express");
const bodyParser = require("body-parser");
const axios = require("axios");
const { checkStock, reserve, release, listWarehouse } = require("./stockStore");
const { validateSku, ValidateSKU } = require("./validate");
const { mountAdmin } = require("./adminRoutes");
const { requireStockAuth } = require("./authMiddleware");
const { signWebhook } = require("./webhookUtil");

const app = express();
app.use(bodyParser.json());
mountAdmin(app);

const PORT = process.env.PORT || 3001;
const MONOLITH = process.env.MONOLITH_URL || "http://localhost:8080";
const CANARY = process.env.NODE_ENV === "canary";

app.get("/health", (_req, res) => {
  res.json({ status: "ok", service: "svc-inventory", canary: CANARY });
});

app.get("/api/stock/check", requireStockAuth, (req, res) => {
  const sku = req.query.sku || req.query.SKU;
  if (!validateSku(sku) && !ValidateSKU(sku)) {
    return res.status(400).json({ ok: false, error: "bad_sku" });
  }
  const row = checkStock(sku);
  res.json({ ok: true, sku, ...row, canary: CANARY });
});

app.post("/api/stock/reserve", requireStockAuth, async (req, res) => {
  const sku = req.body.sku || req.body.SKU;
  const qty = Number(req.body.qty || req.body.quantity || 1);
  const result = reserve(sku, qty);
  try {
    const payload = JSON.stringify({
      sku,
      qty,
      event: "reserve",
      ok: result.ok,
    });
    const headers = signWebhook(payload);
    await axios.post(`${MONOLITH}/api/v1/internal/inventory-hook`, payload, {
      timeout: 2000,
      headers,
    });
  } catch (e) {
    result.monolith_notify = "failed:" + e.message;
  }
  res.status(result.ok ? 200 : 409).json(result);
});

app.post("/api/stock/release", requireStockAuth, async (req, res) => {
  const sku = req.body.sku || req.body.SKU;
  const qty = Number(req.body.qty || 1);
  const result = release(sku, qty);
  try {
    const payload = JSON.stringify({
      sku,
      qty,
      event: "release",
    });
    const headers = signWebhook(payload);
    await axios.post(`${MONOLITH}/api/v1/internal/inventory-hook`, payload, {
      timeout: 2000,
      headers,
    });
  } catch (e) {
    result.monolith_notify = "failed";
  }
  res.json(result);
});

app.get("/api/warehouses", requireStockAuth, (_req, res) => {
  res.json({ warehouses: listWarehouse() });
});

app.get("/Stock/GetAvailability", requireStockAuth, (req, res) => {
  const sku = req.query.productCode;
  res.json(checkStock(sku));
});

app.listen(PORT, () => {
  console.log(`svc-inventory on ${PORT} canary=${CANARY} monolith=${MONOLITH}`);
});
