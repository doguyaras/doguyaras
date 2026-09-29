-- order servisinin domain tablosu. Saga tablolari platform-messaging'in db/platform/saga_coordinator.sql dosyasindan
-- ${schema} = "order" ile uretilir (ayni migration setinde, bu dosyadan once).
CREATE TABLE "order".order_item (
  id            UUID PRIMARY KEY,
  account_id    UUID NOT NULL,
  resource_id   UUID NOT NULL UNIQUE,                 -- ayni kaynak iki kez siparis edilemez (domain uniqueness)
  operation_key UUID NOT NULL,
  created_at    TIMESTAMPTZ NOT NULL
);
