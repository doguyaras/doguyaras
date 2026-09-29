-- order semasi: siparis kalemi. id uygulamada UUIDv7 ile uretilir (referans Bolum 10.3).
CREATE TABLE "order".order_item (
  id          UUID PRIMARY KEY,
  account_id  UUID NOT NULL,
  sku         TEXT NOT NULL,
  quantity    INT  NOT NULL CHECK (quantity > 0),
  status      TEXT NOT NULL DEFAULT 'CREATED' CHECK (status IN ('CREATED','CANCELLED')),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Hesabin siparis listesi (keyset sayfalama: created_at DESC, id DESC) icin.
CREATE INDEX idx_order_item_account ON "order".order_item (account_id, created_at DESC, id DESC);
