// Modern portal API client — different naming than legacy portal
const API_BASE = window.MONOLITH_URL || "http://localhost:8080";
const TOKEN = localStorage.getItem("dem_token") || "dev-admin";

async function apiGet(path) {
  const res = await fetch(API_BASE + path, {
    headers: {
      Authorization: "Bearer " + TOKEN,
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
      Authorization: "Bearer " + TOKEN,
      "Content-Type": "application/json",
      "X-Region": localStorage.getItem("dem_region") || "eu-west",
    },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error("HTTP " + res.status + " " + (await res.text()));
  return res.json();
}

window.ModernApi = { apiGet, apiPost };
