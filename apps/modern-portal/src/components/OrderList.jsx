function OrderList({ orders, onRefresh }) {
  return (
    <div className="panel">
      <h2>Orders <span className="badge">React</span></h2>
      <button onClick={onRefresh}>Refresh</button>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Status</th>
            <th>Total</th>
            <th>Region</th>
          </tr>
        </thead>
        <tbody>
          {(orders || []).map((o) => (
            <tr key={o.id}>
              <td>{o.id}</td>
              <td>{o.status}</td>
              <td>
                {o.total} {o.currency}
              </td>
              <td>{o.region}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

window.OrderList = OrderList;
