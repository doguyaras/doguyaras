-- Baseline sonrasi uygulanmasi beklenen tek migration.
ALTER TABLE legacy.legacy_thing ADD COLUMN note TEXT;
