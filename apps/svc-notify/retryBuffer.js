/**
 * Dead letter / retry — notify complexity
 */
const { KafkaBus } = require("./kafkaStub");
const { RabbitBus } = require("./rabbitStub");

class RetryBuffer {
  constructor() {
    this.failed = [];
    this.kafka = new KafkaBus(process.env.KAFKA_BROKERS || "kafka-aws:9092");
    this.rabbit = new RabbitBus(process.env.RABBIT_URL || "amqp://guest:guest@rabbit-vm:5672");
  }

  capture(topic, payload, err) {
    this.failed.push({ topic, payload, err: String(err), at: Date.now(), attempts: 0 });
  }

  flush() {
    const still = [];
    for (const item of this.failed) {
      item.attempts += 1;
      try {
        this.kafka.publish(item.topic, item.payload);
        this.rabbit.publish(item.topic.replace(/\./g, "_"), item.payload);
      } catch (e) {
        if (item.attempts < 5) still.push(item);
      }
    }
    this.failed = still;
    return { remaining: this.failed.length };
  }
}

module.exports = { RetryBuffer };
