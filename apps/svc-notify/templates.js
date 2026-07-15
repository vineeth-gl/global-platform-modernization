/** Email templates — minimal, undocumented */

function renderTemplate(name, data) {
  data = data || {};
  if (name === "order_created") {
    return {
      subject: `Order ${data.order_id} confirmed`,
      body: `Thanks. Order ${data.order_id} is confirmed for ${data.email || "customer"} in ${data.region || "n/a"}.`,
    };
  }
  if (name === "order_cancelled") {
    return {
      subject: `Order ${data.order_id} cancelled`,
      body: `Order ${data.order_id} cancelled. Reason: ${data.reason || "n/a"}`,
    };
  }
  return { subject: name, body: JSON.stringify(data) };
}

module.exports = { renderTemplate };
