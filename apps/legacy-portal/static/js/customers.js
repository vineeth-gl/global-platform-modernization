function searchCustomers() {
  var q = $("#cust-q").val() || "";
  DEMbApi.get("/api/v1/customers?q=" + encodeURIComponent(q), function (data) {
    var $ul = $("#cust-list").empty();
    (data.customers || []).forEach(function (c) {
      $ul.append(
        "<li>" +
          c.id +
          " — " +
          c.name +
          " &lt;" +
          c.email +
          "&gt;" +
          (c.vip ? " [VIP]" : "") +
          " (" +
          (c.source || "?") +
          ")</li>"
      );
    });
  });
}

function searchCatalog() {
  var q = $("#cat-q").val() || "";
  DEMbApi.get("/api/v1/catalog/search?q=" + encodeURIComponent(q), function (data) {
    var $box = $("#cat-results").empty();
    (data.results || []).forEach(function (r) {
      $box.append(
        '<div class="item">' +
          r.ProductCode +
          " — " +
          r.Title +
          " — " +
          r.Amt +
          " " +
          r.Cur +
          "</div>"
      );
    });
  });
}

window.LegacyCustomers = { searchCustomers: searchCustomers, searchCatalog: searchCatalog };
