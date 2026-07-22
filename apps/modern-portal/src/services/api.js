// Modern portal API client — different naming than legacy portal
const API_BASE = window.MONOLITH_URL || "http://localhost:8080";

function authHeaders() {
  const token = localStorage.getItem("dem_token");
  if (!token) return {};
  return { Authorization: "Bearer " + token };
}

async function apiGet(path) {
  const res = await fetch(API_BASE + path, {
    headers: {
      ...authHeaders(),
      "X-Region": localStorage.getItem("dem_region") || "eu-west",
    },
  });
  if (!res.ok) throw new Error("HTTP " + res.status);
  return res.json();
}

async function apiPost(path, body) {
  const res = await fetch(API_BASE + path, {
    method: "POST",
    headers: {
      ...authHeaders(),
      "Content-Type": "application/json",
      "X-Region": localStorage.getItem("dem_region") || "eu-west",
    },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error("HTTP " + res.status + " " + (await res.text()));
  return res.json();
}

window.ModernApi = { apiGet, apiPost };
