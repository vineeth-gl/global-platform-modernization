function renderOrders(data) {
  var rows = (data && data.orders) || [];
  var $tb = $("#orders-table tbody").empty();
  rows.forEach(function (o) {
    $tb.append(
      "<tr><td>" +
        o.id +
        "</td><td>" +
        o.status +
        "</td><td>" +
        o.total +
        " " +
        (o.currency || "") +
        "</td><td>" +
        (o.region || "") +
        "</td></tr>"
    );
  });
}

function refreshOrders() {
  DEMbApi.get("/api/v1/orders", renderOrders);
}

function openCreateOrder() {
  $("#dialog-order").dialog("open");
}

function submitCreateOrder() {
  DEMbApi.post(
    "/api/v1/orders",
    {
      customer_id: $("#ord-cust").val(),
      email: $("#ord-email").val(),
      lines: [{ sku: $("#ord-sku").val(), qty: Number($("#ord-qty").val() || 1) }],
      source: "legacy-portal",
    },
    function () {
      $("#dialog-order").dialog("close");
      refreshOrders();
    }
  );
}

window.LegacyOrders = { refreshOrders: refreshOrders, openCreateOrder: openCreateOrder, submitCreateOrder: submitCreateOrder };
