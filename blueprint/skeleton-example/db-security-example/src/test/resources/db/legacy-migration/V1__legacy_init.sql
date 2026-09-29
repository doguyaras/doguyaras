-- Test senaryosu 9: "mevcut" semanin elle yaratilmis haline denk gelen migration. Baseline=1 ile hic kosmaz;
-- kossaydi tablo zaten var oldugu icin patlardi (42P07) — bu da baseline'in dogru surumle alindigini kanitlar.
CREATE TABLE legacy.legacy_thing (id INT PRIMARY KEY);
