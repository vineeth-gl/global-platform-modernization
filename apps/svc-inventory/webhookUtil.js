/**
 * HMAC-SHA256 webhook signing for inventory → monolith callbacks.
 */
const crypto = require("crypto");

function signWebhook(rawBody) {
  const secret = process.env.WEBHOOK_SECRET || "";
  if (!secret) {
    throw new Error("WEBHOOK_SECRET is required");
  }
  const timestamp = String(Math.floor(Date.now() / 1000));
  const signature = crypto
    .createHmac("sha256", secret)
    .update(timestamp + "." + rawBody)
    .digest("hex");
  return {
    "X-Webhook-Timestamp": timestamp,
    "X-Webhook-Signature": signature,
    "Content-Type": "application/json",
  };
}

module.exports = { signWebhook };
