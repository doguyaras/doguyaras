-- Tuketici servisin (notification) kendi semasi: inbox (platform sablonundan) + is etkisi tablosu.
-- Etki tablosunda event_id uzerinde UNIQUE YOKTUR: cift teslim korumasi inbox'in isidir; testte cift etki gorulebilsin.
CREATE TABLE ${schema}.order_cancelled_effect (
  event_id   UUID NOT NULL,
  order_id   UUID NOT NULL,
  reason     TEXT NOT NULL,
  applied_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_order_cancelled_effect_event ON ${schema}.order_cancelled_effect (event_id);
