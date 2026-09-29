-- Insan okur siparis numarasi: sequence tabanli. Default privilege'in SEQUENCES kismini kanitlar:
-- USAGE olmadan DEFAULT nextval(...) uygulama rolunde 42501 verir.
CREATE SEQUENCE "order".order_number_seq;
CREATE TABLE "order".order_number (
    number      BIGINT      PRIMARY KEY DEFAULT nextval('"order".order_number_seq'),
    order_id    UUID        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
