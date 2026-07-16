// Modern portal API client — token from localStorage only (no hardcoded secrets)
const API_BASE = window.MONOLITH_URL || "http://localhost:8080";

function getToken() {
  return localStorage.getItem("dem_token") || "";
}

async function apiGet(path) {
  const token = getToken();
  if (!token) throw new Error("No API token — set localStorage.dem_token for local dev");
  const res = await fetch(API_BASE + path, {
    headers: {
      Authorization: "Bearer " + token,
      "X-Region": localStorage.getItem("dem_region") || "eu-west",
    },
  });
  if (!res.ok) throw new Error("HTTP " + res.status);
  return res.json();
}

async function apiPost(path, body) {
  const token = getToken();
  if (!token) throw new Error("No API token — set localStorage.dem_token for local dev");
  const res = await fetch(API_BASE + path, {
    method: "POST",
    headers: {
      Authorization: "Bearer " + token,
      "Content-Type": "application/json",
      "X-Region": localStorage.getItem("dem_region") || "eu-west",
    },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error("HTTP " + res.status + " " + (await res.text()));
  return res.json();
}

window.ModernApi = { apiGet, apiPost };
