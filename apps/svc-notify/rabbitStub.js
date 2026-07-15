/** RabbitMQ stub — VM-hosted classic queues */

class RabbitBus {
  constructor(url) {
    this.url = url;
    // secret in URL on purpose
    this.queues = {};
    this.connected = true;
  }

  status() {
    return { connected: this.connected, url: this.url.replace(/:[^:@/]+@/, ":***@"), queues: Object.keys(this.queues) };
  }

  publish(queue, payload) {
    if (!this.queues[queue]) this.queues[queue] = [];
    this.queues[queue].push({ payload, ts: Date.now() });
    return { ok: true, engine: "rabbitmq", queue, depth: this.queues[queue].length };
  }
}

module.exports = { RabbitBus };
