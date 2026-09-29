-- Local saga koordinator tablolari (referans Bolum 11.4). Koordinatorun KENDI semasinda; her servis kendi migration'inda
-- ${schema} yerine kendi semasini yazar. Merkezi coordinator servisi yoktur.
CREATE TABLE ${schema}.saga (
  id             UUID PRIMARY KEY,
  account_id     UUID NOT NULL,
  scope          TEXT NOT NULL,                       -- 'ORDER_CREATE' (UPPER_SNAKE)
  operation_key  UUID NOT NULL,                       -- X-Idempotency-Key
  status         TEXT NOT NULL CHECK (status IN ('STARTED','SUCCEEDED','CONFIRMED','CANCEL_REQUESTED','COMPENSATED','MANUAL_REVIEW')),
  result         TEXT,                                -- replay icin onceki sonuc (istemciye gosterilebilir kisa deger)
  created_at     TIMESTAMPTZ NOT NULL,
  updated_at     TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_saga_key UNIQUE (account_id, scope, operation_key)
);
CREATE TABLE ${schema}.saga_steps (
  id              UUID PRIMARY KEY,
  saga_id         UUID NOT NULL REFERENCES ${schema}.saga(id) ON DELETE CASCADE,
  step_name       TEXT NOT NULL,                      -- katilimci/adim adi ('quota'); cok adimli genelleme icin parametre
  next_action     TEXT NOT NULL CHECK (next_action IN ('CONFIRM','COMPENSATE')),
  status          TEXT NOT NULL CHECK (status IN ('PENDING','RETRY','RUNNING','DONE','MANUAL_REVIEW')),
  attempt         INT NOT NULL DEFAULT 0,
  next_attempt_at TIMESTAMPTZ NOT NULL,               -- begin: now + deadline (surec cokerse otomatik telafi)
  lock_token      UUID,
  locked_until    TIMESTAMPTZ,
  last_error_code VARCHAR(120),
  created_at      TIMESTAMPTZ NOT NULL,
  updated_at      TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_saga_steps_claim ON ${schema}.saga_steps (status, next_attempt_at, locked_until);
CREATE INDEX idx_saga_unresolved ON ${schema}.saga (created_at)
  WHERE status NOT IN ('CONFIRMED','COMPENSATED','MANUAL_REVIEW');
CREATE INDEX idx_saga_retention ON ${schema}.saga (updated_at) WHERE status IN ('CONFIRMED','COMPENSATED');
