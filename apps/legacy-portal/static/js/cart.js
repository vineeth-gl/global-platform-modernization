/**
 * Legacy portal cart glue — incomplete React migration leftover in jQuery world
 */
function CartUI() {
  this.lines = [];
}

CartUI.prototype.add = function (sku, qty, price) {
  var found = null;
  this.lines.forEach(function (l) {
    if (l.sku === sku) found = l;
  });
  if (found) found.qty += qty;
  else this.lines.push({ sku: sku, qty: qty, unit_price: price });
};

CartUI.prototype.subtotal = function () {
  var s = 0;
  this.lines.forEach(function (l) {
    s += l.qty * l.unit_price;
  });
  return Math.round(s * 100) / 100;
};

CartUI.prototype.render = function ($el) {
  $el = $el || $("#cat-results");
  var html = "<div class='cart'><b>Cart</b><ul>";
  this.lines.forEach(function (l) {
    html += "<li>" + l.sku + " x" + l.qty + " @ " + l.unit_price + "</li>";
  });
  html += "</ul><div>Subtotal: " + this.subtotal() + "</div></div>";
  $el.append(html);
};

CartUI.prototype.checkout = function () {
  var self = this;
  DEMbApi.post(
    "/api/v1/orders",
    {
      customer_id: "C-1001",
      email: $("#ord-email").val() || "buyer@example.com",
      lines: self.lines,
      source: "legacy-cart",
    },
    function () {
      self.lines = [];
      alert("Order placed from cart");
      LegacyOrders.refreshOrders();
    }
  );
};

window.LegacyCart = new CartUI();
