-- Siparis iptal alanlari ve iptal logu.
ALTER TABLE "order".order_item ADD COLUMN reason VARCHAR(500) NOT NULL;
ALTER TABLE "order".order_item ADD COLUMN cancelled_at TIMESTAMP;

CREATE TABLE "order".order_cancel_log (
  id          SERIAL PRIMARY KEY,
  order_id    UUID REFERENCES "order".order_item(id) ON DELETE CASCADE,
  account_id  UUID REFERENCES "subscription".account(id),
  reason      TEXT,
  created_at  TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_cancel_log_order ON "order".order_cancel_log (order_id);
CREATE INDEX idx_cancel_log_order2 ON "order".order_cancel_log (order_id);

GRANT USAGE ON SCHEMA subscription TO svc_order;
GRANT SELECT ON subscription.account_status TO svc_order;
