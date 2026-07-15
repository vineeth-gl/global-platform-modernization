/** JS copy of validators — drift from Python versions */
function validateEmail(email) {
  return /.+@.+\..+/.test(String(email || ""));
}

function validateSku(sku) {
  return /^SKU-\d+/i.test(String(sku || ""));
}

function validateCountry(code) {
  return String(code || "").length >= 2;
}

module.exports = { validateEmail, validateSku, validateCountry };
