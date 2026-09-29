-- Modul basina sema (Bolum 1.1 Sekil A: "sema/modul ayrimi"). Uretimde Flyway migration'i olur.
-- event_publication tablosu burada DEGIL: Spring Modulith'in kendi DDL'i (jar icindeki
-- schemas/v2/schema-postgresql.sql) birebir migration'a kopyalanir; testler o dosyayi dogrudan uygular.
CREATE SCHEMA IF NOT EXISTS ordering;
CREATE SCHEMA IF NOT EXISTS inventory;

CREATE TABLE IF NOT EXISTS ordering.orders (
  id        UUID PRIMARY KEY,
  sku       TEXT NOT NULL,
  quantity  INT  NOT NULL CHECK (quantity > 0),
  placed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS inventory.stock (
  sku       TEXT PRIMARY KEY,
  available INT  NOT NULL CHECK (available >= 0)
);

-- Idempotent dinleyicinin dedup anahtari: siparis basina tek rezervasyon.
CREATE TABLE IF NOT EXISTS inventory.reservation (
  order_id    UUID PRIMARY KEY,
  sku         TEXT NOT NULL,
  quantity    INT  NOT NULL,
  reserved_at TIMESTAMPTZ NOT NULL
);
