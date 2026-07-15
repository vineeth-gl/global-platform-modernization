-- SQL schema mimic (orders DB)
CREATE TABLE customers (
  id VARCHAR(32) PRIMARY KEY,
  email VARCHAR(255),
  name VARCHAR(255),
  country CHAR(2),
  vip BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE products (
  sku VARCHAR(32) PRIMARY KEY,
  name VARCHAR(255),
  price NUMERIC(12,2),
  currency CHAR(3),
  active BOOLEAN DEFAULT TRUE
);

CREATE TABLE orders (
  id BIGINT PRIMARY KEY,
  customer_id VARCHAR(32) REFERENCES customers(id),
  status VARCHAR(32),
  region VARCHAR(32),
  tax NUMERIC(12,2),
  total NUMERIC(12,2),
  currency CHAR(3),
  email VARCHAR(255),
  meta JSONB,
  created_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ
);

CREATE TABLE order_lines (
  order_id BIGINT REFERENCES orders(id),
  sku VARCHAR(32),
  qty INT,
  unit_price NUMERIC(12,2)
);

-- AcmeBill collision: sequences start at 900000
CREATE SEQUENCE order_id_seq START 900000;
