function CreateOrderForm({ onCreated }) {
  const [sku, setSku] = React.useState("SKU-100");
  const [qty, setQty] = React.useState(1);
  const [msg, setMsg] = React.useState("");

  async function submit(e) {
    e.preventDefault();
    setMsg("Submitting...");
    try {
      const order = await ModernApi.apiPost("/api/v1/orders", {
        customer_id: "C-1001",
        email: "buyer@example.com",
        lines: [{ sku, qty: Number(qty) }],
        source: "react-portal",
      });
      setMsg("Created order " + order.id + " status=" + order.status);
      onCreated && onCreated(order);
    } catch (err) {
      setMsg(String(err));
    }
  }

  return (
    <div className="panel">
      <h2>Quick Order</h2>
      <form onSubmit={submit}>
        <label>SKU</label>
        <input value={sku} onChange={(e) => setSku(e.target.value)} />
        <label>Qty</label>
        <input value={qty} onChange={(e) => setQty(e.target.value)} type="number" min="1" />
        <button type="submit">Place order</button>
      </form>
      <p>{msg}</p>
    </div>
  );
}

window.CreateOrderForm = CreateOrderForm;
