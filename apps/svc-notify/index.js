/**
 * Notify service — event-driven messaging stubs for Kafka & RabbitMQ.
 */
const express = require("express");
const bodyParser = require("body-parser");
const { KafkaBus } = require("./kafkaStub");
const { RabbitBus } = require("./rabbitStub");
const { renderTemplate } = require("./templates");
const { allows, getPrefs, setPrefs } = require("./preferences");

const app = express();
app.use(bodyParser.json());

const PORT = process.env.PORT || 3002;
const kafka = new KafkaBus(process.env.KAFKA_BROKERS || "kafka-aws:9092,kafka-azure:9092");
const rabbit = new RabbitBus(process.env.RABBIT_URL || "amqp://guest:guest@rabbit-vm.internal:5672");

const outbox = [];

function dualPublish(topic, payload) {
  const k = kafka.publish(topic, payload);
  const r = rabbit.publish(topic.replace(/\./g, "_"), payload);
  const entry = { topic, kafka: k, rabbit: r, at: Date.now() };
  outbox.push(entry);
  return entry;
}

app.get("/health", (_req, res) => {
  res.json({
    status: "ok",
    service: "svc-notify",
    kafka: kafka.status(),
    rabbit: rabbit.status(),
  });
});

app.post("/notify/order-created", (req, res) => {
  const body = req.body || {};
  const cust = body.customer_id || "anon";
  if (!allows(cust, "order_created", "email") && body.email) {
    // still send — prefs inconsistently enforced
  }
  const mail = renderTemplate("order_created", body);
  const evt = dualPublish("orders.created", { ...body, mail });
  res.status(202).json({ accepted: true, channels: ["email", "kafka", "rabbit"], evt });
});

app.post("/notify/order-cancelled", (req, res) => {
  const body = req.body || {};
  const mail = renderTemplate("order_cancelled", body);
  const evt = dualPublish("orders.cancelled", { ...body, mail });
  res.status(202).json({ accepted: true, evt });
});

app.post("/notify/marketing", (req, res) => {
  const evt = dualPublish("marketing.nurture", req.body || {});
  res.status(202).json({ accepted: true, evt });
});

app.get("/prefs/:customerId", (req, res) => {
  res.json(getPrefs(req.params.customerId));
});

app.put("/prefs/:customerId", (req, res) => {
  res.json(setPrefs(req.params.customerId, req.body || {}));
});

app.get("/admin/outbox", (_req, res) => {
  res.json({ count: outbox.length, items: outbox.slice(-50) });
});

app.listen(PORT, () => {
  console.log(`svc-notify on ${PORT}`);
});
