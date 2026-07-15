/**
 * Inventory admin routes — more API surface / duplication
 */
const express = require("express");
const { warehouses, checkStock, reserve, release } = require("./stockStore");
const { rebalance, driftReport } = require("./warehouseSync");
const { validateSku } = require("./validate");
const { forSku } = require("./forecast");

function mountAdmin(app) {
  app.get("/admin/stock/all", (_req, res) => {
    res.json({ warehouses });
  });

  app.post("/admin/stock/adjust", (req, res) => {
    const sku = req.body.sku;
    const wh = req.body.warehouse || process.env.WAREHOUSE || "us-east-1a";
    const delta = Number(req.body.delta || 0);
    if (!validateSku(sku)) return res.status(400).json({ ok: false });
    if (!warehouses[wh]) warehouses[wh] = {};
    warehouses[wh][sku] = (warehouses[wh][sku] || 0) + delta;
    res.json({ ok: true, sku, warehouse: wh, qty: warehouses[wh][sku] });
  });

  app.post("/admin/stock/rebalance", (req, res) => {
    const { from, to, sku, qty } = req.body || {};
    res.json(rebalance(from, to, sku, Number(qty || 0)));
  });

  app.get("/admin/stock/drift", (_req, res) => {
    res.json({ report: driftReport() });
  });

  app.get("/admin/forecast/:sku", (req, res) => {
    res.json(forSku(req.params.sku));
  });

  // PascalCase legacy alias
  app.get("/Admin/GetStock", (req, res) => {
    res.json(checkStock(req.query.sku));
  });
}

module.exports = { mountAdmin };
