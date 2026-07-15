/** Kafka stub — multi-cloud broker list */

class KafkaBus {
  constructor(brokers) {
    this.brokers = String(brokers).split(",").map((s) => s.trim());
    this.topics = {};
    this.connected = true;
  }

  status() {
    return { connected: this.connected, brokers: this.brokers, topicCount: Object.keys(this.topics).length };
  }

  publish(topic, payload) {
    if (!this.topics[topic]) this.topics[topic] = [];
    const msg = { topic, payload, partition: 0, offset: this.topics[topic].length, ts: Date.now() };
    this.topics[topic].push(msg);
    return { ok: true, engine: "kafka", offset: msg.offset, brokers: this.brokers };
  }
}

module.exports = { KafkaBus };
