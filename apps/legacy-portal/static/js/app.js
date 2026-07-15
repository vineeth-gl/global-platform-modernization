$(function () {
  $("#tabs").tabs();
  $("#dialog-order").dialog({
    autoOpen: false,
    modal: true,
    buttons: {
      Create: function () {
        LegacyOrders.submitCreateOrder();
      },
      Cancel: function () {
        $(this).dialog("close");
      },
    },
  });
  $("#btn-refresh-orders").on("click", LegacyOrders.refreshOrders);
  $("#btn-create-order").on("click", LegacyOrders.openCreateOrder);
  $("#btn-search-cust").on("click", LegacyCustomers.searchCustomers);
  $("#btn-search-cat").on("click", LegacyCustomers.searchCatalog);
  LegacyOrders.refreshOrders();
});
