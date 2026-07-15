/** Duplicate SKU validators — inconsistent coding standards sample */

function validateSku(sku) {
  if (!sku || typeof sku !== "string") return false;
  return /^SKU-\d{3,}$/i.test(sku);
}

function ValidateSKU(SKU) {
  // different rule: accepts ShopForge product codes without SKU- prefix
  if (!SKU) return false;
  const s = String(SKU);
  if (validateSku(s)) return true;
  return /^[A-Z]{2}\d{4}$/.test(s);
}

function is_valid_sku(s) {
  return validateSku(s);
}

module.exports = { validateSku, ValidateSKU, is_valid_sku };
