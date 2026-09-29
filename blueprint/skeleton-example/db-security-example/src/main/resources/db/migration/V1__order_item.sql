-- Siparis kalemi: hesap sahipligi icin RLS ikinci savunma hatti (referans Bolum 10.1, 10.3).
-- Sema "order" altyapi migration'inda svc_order_migrate sahipliginde yaratildi; burada CREATE SCHEMA yok.
-- Sahibi: order servisi. account_id baska servisin kimligi: FK yok, duz UUID.
CREATE TABLE "order".order_item (
    id          UUID        PRIMARY KEY DEFAULT uuidv7(),   -- PG18 UUIDv7: zaman sirali, B-tree sayfa bolunmesi yok
    account_id  UUID        NOT NULL,
    resource    TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Hesabin listesi, yeni->eski, deterministik tie-breaker (v7 ile id sirasi created_at ile uyusur).
CREATE INDEX idx_order_item_account_created ON "order".order_item (account_id, created_at DESC, id DESC);

ALTER TABLE "order".order_item ENABLE ROW LEVEL SECURITY;
-- Baglam TX icinde SET LOCAL ile verilir (AccountScope). Set edilmemisse current_setting(..., true) NULL veya
-- '' doner; NULLIF ile NULL'a cekilir -> hicbir satir gorunmez (policy'siz/baglamsiz = kapali).
-- WITH CHECK yazilmadigi icin USING ifadesi INSERT/UPDATE'te de uygulanir: baska hesaba satir yazilamaz.
CREATE POLICY order_item_account_isolation ON "order".order_item
    USING (account_id = NULLIF(current_setting('app.account_id', true), '')::uuid);
