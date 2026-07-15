function CatalogPanel({ query, onQuery, results, onSearch }) {
  return (
    <div className="panel" style={{ gridColumn: "1 / -1" }}>
      <h2>Catalog Search</h2>
      <input value={query} onChange={(e) => onQuery(e.target.value)} placeholder="SKU or name" />
      <button onClick={onSearch}>Search</button>
      <ul>
        {(results || []).map((r, i) => (
          <li key={i}>
            {r.ProductCode} — {r.Title} — {r.Amt} {r.Cur}
            {r.legacyOnly ? " (legacyOnly)" : ""}
          </li>
        ))}
      </ul>
    </div>
  );
}

window.CatalogPanel = CatalogPanel;
