-- Katilimci (referans Bolum 11.4 "Katilimci sozlesmesi"). Tekillik (caller_service, account_id, operation_key):
-- caller_service JWT act claim'inden gelir, govdeden degil.
CREATE TABLE subscription.quota (
  account_id UUID PRIMARY KEY,
  remaining  INT NOT NULL CHECK (remaining >= 0)
);
CREATE TABLE subscription.operation (
  caller_service TEXT NOT NULL,
  account_id     UUID NOT NULL,
  operation_key  UUID NOT NULL,
  op_type        TEXT NOT NULL,
  status         TEXT NOT NULL CHECK (status IN ('APPLIED','REJECTED','CONFIRMED','CANCELLED','COMPENSATED','MANUAL_REVIEW')),
  amount         INT NOT NULL,
  refunded_at    TIMESTAMPTZ,                         -- cift iadeyi engeller (compensate tekrar gelirse no-op)
  created_at     TIMESTAMPTZ NOT NULL,
  updated_at     TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (caller_service, account_id, operation_key)
);
