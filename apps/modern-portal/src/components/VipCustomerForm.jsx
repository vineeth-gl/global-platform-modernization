function VipCustomerForm({ onCreated }) {
  const [name, setName] = React.useState("");
  const [email, setEmail] = React.useState("");
  const [country, setCountry] = React.useState("DE");
  const [msg, setMsg] = React.useState("");

  async function submit(e) {
    e.preventDefault();
    setMsg("Saving...");
    try {
      // VIP customers go to Mongo only — known split brain
      const cust = await ModernApi.apiPost("/api/v1/customers", {
        name,
        email,
        country,
        vip: true,
        isVIP: true,
      });
      setMsg("Created VIP " + cust.id + " via " + (cust.source || "?"));
      onCreated && onCreated(cust);
    } catch (err) {
      setMsg(String(err));
    }
  }

  return (
    <div className="panel">
      <h2>VIP Customer (Mongo)</h2>
      <form onSubmit={submit}>
        <label>Name</label>
        <input value={name} onChange={(e) => setName(e.target.value)} required />
        <label>Email</label>
        <input value={email} onChange={(e) => setEmail(e.target.value)} required />
        <label>Country</label>
        <input value={country} onChange={(e) => setCountry(e.target.value)} />
        <button type="submit">Create VIP</button>
      </form>
      <p>{msg}</p>
    </div>
  );
}

window.VipCustomerForm = VipCustomerForm;
