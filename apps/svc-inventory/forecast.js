/**
 * Port of forecast.py heuristics — intentional cross-language duplication
 */
function movingAverage(values, window) {
  window = window || 7;
  const out = [];
  for (let i = 0; i < values.length; i++) {
    const start = Math.max(0, i - window + 1);
    const chunk = values.slice(start, i + 1);
    out.push(chunk.reduce((a, b) => a + b, 0) / chunk.length);
  }
  return out;
}

function forecastDemand(dailyQty, horizon) {
  horizon = horizon || 14;
  if (!dailyQty || !dailyQty.length) return Array(horizon).fill(0);
  const ma = movingAverage(dailyQty, 7);
  const last = ma[ma.length - 1] || 0;
  const out = [];
  for (let i = 1; i <= horizon; i++) out.push(Math.round(last * (1 + 0.01 * i) * 100) / 100);
  return out;
}

function reorderPoint(avgDaily, leadDays, safety) {
  safety = safety == null ? 1.5 : safety;
  return Math.round(avgDaily * leadDays * safety * 100) / 100;
}

const HISTORY = {
  "SKU-100": [10, 12, 9, 11, 13, 12, 14, 15, 11, 10, 12, 13, 14, 12],
  "SKU-200": [3, 4, 2, 5, 4, 3, 4, 5, 6, 4, 3, 4, 5, 4],
  "SKU-300": [1, 0, 2, 1, 0, 1, 1, 2, 1, 0, 1, 1, 0, 1],
};

function forSku(sku) {
  const hist = HISTORY[sku] || Array(14).fill(0);
  const fut = forecastDemand(hist);
  const avg = hist.reduce((a, b) => a + b, 0) / (hist.length || 1);
  return {
    sku,
    history: hist,
    forecast: fut,
    reorder_point: reorderPoint(avg, 5),
    avg_daily: Math.round(avg * 100) / 100,
  };
}

module.exports = { movingAverage, forecastDemand, reorderPoint, forSku, HISTORY };
