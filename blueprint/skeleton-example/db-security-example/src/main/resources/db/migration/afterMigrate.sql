-- Flyway SQL callback: her basarili migrate() sonunda (bekleyen migration olmasa bile) migration rolu ile kosar.
-- NEDEN: flyway_schema_history, migration rolunun "order" semasinda yarattigi bir tablodur; ALTER DEFAULT PRIVILEGES
-- ona da uygulama rolu icin SELECT/INSERT/UPDATE/DELETE verir. Ele gecirilen uygulama sureci history satirini
-- silerse sonraki deploy migration'i yeniden uygular ya da validate'te patlar. Default privilege tablo bazinda
-- istisna tanimaz; bu yuzden yetki her migrate sonunda geri alinir (idempotent, elle yapilan drift'i de onarir).
-- Tablo adi ve sema Flyway'in yerlesik placeholder'lari ile gelir: flyway.table ayari degisse de dogru tabloyu hedefler.
REVOKE ALL ON "${flyway:defaultSchema}"."${flyway:table}" FROM svc_order;
