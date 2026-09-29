-- Altyapi migration'i: superuser/DBA rolunde, servis basina BIR KEZ kosar (referans Bolum 10.1).
-- Flyway'in disindadir: Flyway migration rolu ile kosar ve kendi rolunu/semasini yaratamaz.
-- Dolar-suslu placeholder'lar ServiceRoleBootstrap tarafindan doldurulur; kimlikler orada dogrulanir.

-- NEDEN iki rol: sema sahibi ve DDL yetkisi yalniz migration rolunde kalir. Ele gecirilen uygulama sureci
-- tablo dusuremez, trigger/REVOKE kaldiramaz, audit tablosunu degistiremez.
CREATE ROLE ${migrate_role} LOGIN PASSWORD '${migrate_password}';
CREATE ROLE ${app_role} LOGIN PASSWORD '${app_password}';
CREATE SCHEMA "${schema}" AUTHORIZATION ${migrate_role};

-- public semasi kimseye acik degil: yanlislikla public'e tablo yazilmasin, uygulama rolu oradan okuyamasin.
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA "${schema}" TO ${app_role};

-- Migration rolunun ILERIDE yaratacagi her tablo/sequence'te uygulama rolunun yetkisi otomatik gelsin:
-- her migration'a GRANT yazmak unutulur, default privilege unutulmaz. DDL yetkisi verilmez.
ALTER DEFAULT PRIVILEGES FOR ROLE ${migrate_role} IN SCHEMA "${schema}"
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${app_role};
ALTER DEFAULT PRIVILEGES FOR ROLE ${migrate_role} IN SCHEMA "${schema}"
    GRANT USAGE, SELECT ON SEQUENCES TO ${app_role};

-- Rol bazli zaman asimlari: global postgresql.conf yerine role baglanir; migration rolunde statement_timeout
-- YOK (backfill uzun surebilir), yalniz lock_timeout (DDL kilidi beklerken uretimi kilitlemesin).
ALTER ROLE ${app_role} SET statement_timeout = '${statement_timeout}';
ALTER ROLE ${app_role} SET lock_timeout = '${lock_timeout}';
ALTER ROLE ${app_role} SET idle_in_transaction_session_timeout = '${idle_in_transaction_timeout}';
ALTER ROLE ${migrate_role} SET lock_timeout = '${migrate_lock_timeout}';
