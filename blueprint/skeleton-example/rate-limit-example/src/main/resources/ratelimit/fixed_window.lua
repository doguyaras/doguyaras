-- Fixed window sayac (referans Bolum 9.6). Tek script = tek atomik adim: INCRBY ile TTL kontrolu arasina
-- baska istemcinin komutu giremez; iki instance ayni anda "ilk vurus" sanip sayaci bozamaz.
-- KEYS[1] = rl:<scope>:<sha256(scope \0 ozne)>, ARGV[1] = pencere (sn), ARGV[2] = maliyet (genelde 1)
-- Donus: { sayac, kalan pencere (ms) }
local current = redis.call('INCRBY', KEYS[1], ARGV[2])
local pttl = redis.call('PTTL', KEYS[1])
-- "current == maliyet" yerine TTL'e bakilir: TTL'siz kalmis bir key (elle SET, eski surum, INCR/EXPIRE'i ayri
-- yapan bir istemci) aksi halde sonsuza dek kilitli kalir; bu kosul onu bir sonraki vurusta iyilestirir.
if pttl < 0 then
  redis.call('EXPIRE', KEYS[1], ARGV[1])
  pttl = tonumber(ARGV[1]) * 1000
end
return { current, pttl }
