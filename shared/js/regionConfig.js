/**
 * Region config mirror for Node services
 */
const REGIONS = {
  "us-east": { cloud: "aws", bill_v2: false, support_pod: "AMER", kafka: true, rabbit: true },
  "eu-west": { cloud: "azure", bill_v2: true, support_pod: "EMEA", kafka: true, rabbit: true },
  "ap-south": { cloud: "aws", bill_v2: false, support_pod: "APAC", kafka: true, rabbit: false },
};

function getRegion(name) {
  return Object.assign({}, REGIONS[name] || REGIONS["us-east"]);
}

function listClouds() {
  return Array.from(new Set(Object.values(REGIONS).map((r) => r.cloud))).sort();
}

module.exports = { REGIONS, getRegion, listClouds };
