-- Iptal edilmemis siparislerin hesap bazli listesi (GET /v1/orders?status=CREATED) icin partial index:
-- CANCELLED satirlar zamanla cogunluk olur, tam index yerine yalniz aktif satirlar indekslenir.
CREATE INDEX idx_order_item_account_active
    ON "order".order_item (account_id, created_at DESC, id DESC)
    WHERE status = 'CREATED';
