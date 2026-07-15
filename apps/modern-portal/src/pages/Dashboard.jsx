function Dashboard() {
  const [orders, setOrders] = React.useState([]);
  const [err, setErr] = React.useState("");
  const [query, setQuery] = React.useState("");
  const [catalog, setCatalog] = React.useState([]);

  async function load() {
    try {
      const data = await ModernApi.apiGet("/api/v1/orders");
      setOrders(data.orders || []);
      setErr("");
    } catch (e) {
      setErr(String(e));
    }
  }

  async function searchCatalog() {
    try {
      const data = await ModernApi.apiGet("/api/v1/catalog/search?q=" + encodeURIComponent(query));
      setCatalog(data.results || []);
    } catch (e) {
      setErr(String(e));
    }
  }

  React.useEffect(() => {
    load();
  }, []);

  return (
    <>
      <header>
        <h1>DEMb Modern Portal</h1>
        <p>React beside legacy jQuery — hybrid UI estate. Region default: eu-west (bill_v2 canary).</p>
        {err ? <p style={{ color: "#f87171" }}>{err}</p> : null}
      </header>
      <main>
        <OrderList orders={orders} onRefresh={load} />
        <VipCustomerForm onCreated={() => load()} />
        <CreateOrderForm onCreated={() => load()} />
        <CatalogPanel query={query} onQuery={setQuery} results={catalog} onSearch={searchCatalog} />
      </main>
    </>
  );
}

window.Dashboard = Dashboard;
