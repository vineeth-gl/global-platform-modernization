/**
 * Legacy portal shipping/returns extras
 */
function loadReturnsPlaceholder() {
  $("#cust-list").append("<li class='muted'>Returns module: ask platform SME (tribal)</li>");
}

function showRegionHint() {
  var regions = ["us-east", "eu-west", "ap-south"];
  console.log("Active regions for follow-the-sun:", regions.join(", "));
}

window.LegacyExtras = { loadReturnsPlaceholder: loadReturnsPlaceholder, showRegionHint: showRegionHint };
