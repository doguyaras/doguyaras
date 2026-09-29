-- Read-model replikasyon DDL sablonu (referans Bolum 4.6). Tuketici servis kendi semasinda, kaynak basina BIR projeksiyon
-- tutar; ${schema} yerine kendi semasini yazar. Farkli kaynaklarin revizyonlari karsilastirilamaz (auth rev 100 iken
-- user rev 20 olabilir); bu yuzden tek revision kolonlu birlesik tablo YOKTUR: revizyon satirda, konum kaynakta tutulur.

-- Tuketim konumu: TAZELIK buradan olculur (now - last_event_time), projeksiyon satirinin yasindan degil.
-- Bir hesabin durumu bir ay degismemis olabilir; eski applied_at gecikme degildir.
CREATE TABLE ${schema}.rm_consumer_position (
  source          TEXT PRIMARY KEY,                       -- 'auth', 'user'
  last_seq        BIGINT NOT NULL,                        -- kaynak akisinda tuketilen son konum (monoton, GREATEST ile)
  last_event_time TIMESTAMPTZ NOT NULL,                   -- tuketilen son olayin KAYNAKTAKI zamani
  updated_at      TIMESTAMPTZ NOT NULL
);

-- DELTA sozlesmesinde hicbir olay atlanamaz: aggregate basina son uygulanan sira burada; seq != last_seq + 1 => bosluk.
CREATE TABLE ${schema}.rm_delta_position (
  source       TEXT NOT NULL,
  aggregate_id UUID NOT NULL,
  last_seq     BIGINT NOT NULL,
  PRIMARY KEY (source, aggregate_id)
);

-- SNAPSHOT (tam durum) ornegi. Sahibi: auth; olay: account.status.changed. Kucuk/esit revizyon yok sayilir.
CREATE TABLE ${schema}.rm_account_status (
  account_id      UUID PRIMARY KEY,
  active          BOOLEAN NOT NULL,
  legal_ok        BOOLEAN NOT NULL,
  source_revision BIGINT NOT NULL,                        -- auth'un bu hesap icin olay sirasi
  source_time     TIMESTAMPTZ NOT NULL,                   -- olayin kaynaktaki zamani (son is degisikligi)
  applied_at      TIMESTAMPTZ NOT NULL                    -- tuketicinin uyguladigi an (tazelik OLCUSU DEGIL)
);

-- DELTA (degisiklik) ornegi. Sahibi: user; olay: user.block.created / user.block.removed. Sira boslugunda uygulama durur.
CREATE TABLE ${schema}.rm_block_relation (
  blocker_id UUID NOT NULL,
  blocked_id UUID NOT NULL,
  source_seq BIGINT NOT NULL,                             -- user'in (blocker) basina monoton sirasi
  PRIMARY KEY (blocker_id, blocked_id)
);
