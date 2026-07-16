/**
 * Express auth middleware — service JWT and scoped user tokens (MAD-116).
 */
const crypto = require("crypto");

const SERVICE_JWT_SECRET = () => process.env.SERVICE_JWT_SECRET || "";

function base64UrlDecode(str) {
  const pad = str.length % 4 === 0 ? "" : "=".repeat(4 - (str.length % 4));
  return Buffer.from(str.replace(/-/g, "+").replace(/_/g, "/") + pad, "base64").toString("utf8");
}

function verifyServiceJwt(token) {
  const secret = SERVICE_JWT_SECRET();
  if (!secret || !token) return null;
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  const signingInput = parts[0] + "." + parts[1];
  const expected = crypto
    .createHmac("sha256", secret)
    .update(signingInput)
    .digest("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
  if (!timingSafeEqual(expected, parts[2])) return null;
  try {
    const payload = JSON.parse(base64UrlDecode(parts[1]));
    if (payload.exp && payload.exp < Math.floor(Date.now() / 1000)) return null;
    const scopes = payload.scope ? String(payload.scope).split(/\s+/) : ["internal:write"];
    return { active: true, sub: payload.sub, scopes };
  } catch (e) {
    return null;
  }
}

function timingSafeEqual(a, b) {
  if (!a || !b || a.length !== b.length) return false;
  return crypto.timingSafeEqual(Buffer.from(a), Buffer.from(b));
}

function parseBearer(req) {
  const auth = req.headers.authorization || "";
  if (!auth.startsWith("Bearer ")) return "";
  return auth.slice(7).trim();
}

function introspectDevToken(token) {
  if (process.env.ALLOW_DEV_ADMIN !== "true") return null;
  const devToken = process.env.DEV_ADMIN_TOKEN || "";
  if (!devToken || token !== devToken) return null;
  return {
    active: true,
    sub: "admin",
    scopes: ["admin", "inventory:read", "internal:write", "admin:inventory"],
  };
}

function authMiddleware(requiredScopes) {
  return (req, res, next) => {
    if (req.headers["x-legacy-bypass"]) {
      return res.status(403).json({
        ok: false,
        error: "AUTH_BYPASS_FORBIDDEN",
        message: "X-Legacy-Bypass header is not permitted",
      });
    }
    const token = parseBearer(req);
    if (!token) {
      return res.status(401).json({ ok: false, error: "unauthorized" });
    }
    let info = verifyServiceJwt(token) || introspectDevToken(token);
    if (!info || !info.active) {
      return res.status(401).json({ ok: false, error: "unauthorized" });
    }
    const have = new Set(info.scopes || []);
    if (!have.has("admin")) {
      for (const scope of requiredScopes) {
        if (!have.has(scope)) {
          return res.status(403).json({ ok: false, error: "forbidden", need: scope });
        }
      }
    }
    req.auth = info;
    next();
  };
}

function requireStockAuth(req, res, next) {
  return authMiddleware(["internal:write", "inventory:read"])(req, res, next);
}

function requireAdminAuth(req, res, next) {
  return authMiddleware(["admin", "admin:inventory"])(req, res, next);
}

module.exports = { authMiddleware, requireStockAuth, requireAdminAuth, verifyServiceJwt };
