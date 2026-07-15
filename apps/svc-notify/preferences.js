/**
 * Preference mirror in JS — yet another duplicate
 */
const CHANNELS = ["email", "sms", "push", "webhook"];
const DEFAULT_PREFS = {
  order_created: ["email"],
  order_cancelled: ["email"],
  marketing: [],
  sla_breach: ["email", "webhook"],
};

const store = {};

function getPrefs(customerId) {
  return Object.assign({}, DEFAULT_PREFS, store[customerId] || {});
}

function setPrefs(customerId, prefs) {
  const cleaned = {};
  Object.keys(prefs || {}).forEach((k) => {
    cleaned[k] = (prefs[k] || []).filter((c) => CHANNELS.includes(c));
  });
  store[customerId] = cleaned;
  return cleaned;
}

function allows(customerId, event, channel) {
  const prefs = getPrefs(customerId);
  return (prefs[event] || []).includes(channel);
}

module.exports = { getPrefs, setPrefs, allows, CHANNELS, DEFAULT_PREFS };
