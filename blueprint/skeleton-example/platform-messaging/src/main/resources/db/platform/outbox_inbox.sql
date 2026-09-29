-- Generic outbox + inbox DDL sablonu (referans Bolum 11.2-11.3). Her servis kendi semasinda Flyway migration'i olarak
-- kopyalar ve ${schema} yerine kendi semasini yazar. Debezium Outbox Event Router semasiyla uyumludur.
CREATE TABLE ${schema}.outbox_event (
  id              UUID PRIMARY KEY,                       -- UUIDv7; ayni zamanda CloudEvents "id"
  kind            TEXT NOT NULL CHECK (kind IN ('EVENT','COMMAND','HTTP')),   -- lane
  aggregate_type  TEXT NOT NULL,
  aggregate_id    UUID NOT NULL,                          -- routing/partition key; siralama bunun icinde
  event_type      TEXT NOT NULL,                          -- 'order.order.created'
  payload         JSONB NOT NULL,
  headers         JSONB NOT NULL DEFAULT '{}',            -- traceparent, tracestate, hedef (HTTP icin)
  status          TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PUBLISHING','DEAD')),
  priority        SMALLINT NOT NULL DEFAULT 0,            -- yuksek once; guvenlik kararlari > is olaylari > toplu isler
  dead_policy     TEXT NOT NULL DEFAULT 'DEAD_ON_PERMANENT'
                    CHECK (dead_policy IN ('DEAD_ON_PERMANENT','NEVER_DEAD')),  -- guvenlik yan etkisi: NEVER_DEAD
  retry_count     INT  NOT NULL DEFAULT 0,
  next_retry_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  locked_until    TIMESTAMPTZ,
  claim_token     UUID,
  last_error_code VARCHAR(120),                           -- exception SimpleName / HTTP status (mesaj degil)
  expires_at      TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX idx_outbox_event_claim ON ${schema}.outbox_event (kind, status, next_retry_at, priority DESC, created_at);
CREATE INDEX idx_outbox_event_aggregate ON ${schema}.outbox_event (aggregate_id, created_at);

-- Inbox: dedup kapsami HANDLER'dir; satir + is degisikligi ayni TX'te yazilir, ack commit'ten sonra.
CREATE TABLE ${schema}.inbox_event (
  handler     TEXT NOT NULL,
  event_id    UUID NOT NULL,
  received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (handler, event_id)
);
