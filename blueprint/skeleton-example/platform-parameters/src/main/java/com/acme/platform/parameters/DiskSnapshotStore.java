package com.acme.platform.parameters;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.regex.Pattern;
import tools.jackson.databind.json.JsonMapper;

/**
 * Grup basina bir JSON dosyasi (<dir>/<group>.json). Her basarili fetch'ten sonra yazilir; soguk acilista kaynak
 * yoksa buradan ayaga kalkilir (referans Bolum 14 "bellekte ve diskte"). Yazim atomiktir (tmp + move): yarim dosya
 * okunmaz. Dizin instance'a ozel kalici bir volume'dur; birden fazla instance ayni dosyayi paylasmaz.
 */
public final class DiskSnapshotStore {
    /** kebab-case grup adi disinda dosya adi uretilmez (path traversal'a kapali). */
    private static final Pattern GROUP = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    private final Path dir;
    private final JsonMapper mapper;

    public DiskSnapshotStore(Path dir, JsonMapper mapper) {
        this.dir = dir;
        this.mapper = mapper;
    }

    public Optional<ParameterSnapshot> read(String group) {
        Path file = fileOf(group);
        if (!Files.isRegularFile(file)) return Optional.empty();
        try {
            return Optional.of(mapper.readValue(Files.readAllBytes(file), ParameterSnapshot.class));
        } catch (IOException e) {
            throw new UncheckedIOException("snapshot okunamadi: " + group, e);
        }
    }

    public void write(ParameterSnapshot snapshot) {
        String group = snapshot.group().group();
        Path file = fileOf(group);
        try {
            Files.createDirectories(dir);
            Path tmp = Files.createTempFile(dir, group + "-", ".tmp");
            Files.write(tmp, mapper.writeValueAsBytes(snapshot));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("snapshot yazilamadi: " + group, e);
        }
    }

    private Path fileOf(String group) {
        if (group == null || !GROUP.matcher(group).matches()) throw new IllegalArgumentException("gecersiz grup adi");
        return dir.resolve(group + ".json");
    }
}
