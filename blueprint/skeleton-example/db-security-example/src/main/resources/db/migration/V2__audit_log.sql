-- Append-only audit (referans Bolum 10.1, 10.6): uygulama rolu yalniz ekler. Yasal kayit: cascade/retention DELETE yok.
CREATE TABLE "order".audit_log (
    id          UUID        PRIMARY KEY DEFAULT uuidv7(),
    actor       TEXT        NOT NULL,
    action      TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Append-only + fiziksel sira korelasyonu: BRIN cok kucuktur ve zaman araligi sorgularina yeter.
CREATE INDEX idx_audit_log_created ON "order".audit_log USING BRIN (created_at);

-- Birinci hat (yetki): default privilege UPDATE/DELETE verdi, audit icin geri alinir. Uygulama rolu 42501 alir.
REVOKE UPDATE, DELETE ON "order".audit_log FROM svc_order;

-- Ikinci hat (trigger): tablo sahibi (migration rolu) bile satiri degistiremesin; RLS/GRANT'ten bagimsiz calisir.
CREATE FUNCTION "order".audit_log_reject_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is append-only: % rejected', TG_OP;
END
$$;
CREATE TRIGGER trg_audit_log_append_only
    BEFORE UPDATE OR DELETE ON "order".audit_log
    FOR EACH ROW EXECUTE FUNCTION "order".audit_log_reject_change();
