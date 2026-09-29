package com.acme.platform.parameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/** Disk snapshot: gercek dosya sistemi, tam tur (yaz -> oku) ve dosya adi guvenligi. */
class DiskSnapshotStoreTest {

    @TempDir Path dir;
    final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void roundTripPreservesRevisionValuesCriticalityAndFetchedAt() throws Exception {
        DiskSnapshotStore store = new DiskSnapshotStore(dir.resolve("nested"), mapper);   // dizin yoksa olusur
        Instant at = Instant.parse("2026-09-29T09:58:30Z");
        ParameterGroupDto dto = new ParameterGroupDto("order-limits", 42,
                Map.of("order.max_items", mapper.readTree("25"), "order.channels", mapper.readTree("[{\"code\":\"WEB\"}]")),
                Criticality.SECURITY_CRITICAL);
        store.write(new ParameterSnapshot(dto, at));

        ParameterSnapshot read = store.read("order-limits").orElseThrow();
        assertThat(read.fetchedAt()).isEqualTo(at);
        assertThat(read.group()).isEqualTo(dto);
        assertThat(dir.resolve("nested").resolve("order-limits.json")).exists();
        assertThat(Files.list(dir.resolve("nested")).filter(f -> f.toString().endsWith(".tmp"))).as("gecici dosya kalmaz").isEmpty();
    }

    @Test
    void missingFileReadsAsEmpty() {
        assertThat(new DiskSnapshotStore(dir, mapper).read("order-limits")).isEmpty();
    }

    @Test
    void rejectsNonKebabCaseGroupNamesSoNoPathTraversal() {
        DiskSnapshotStore store = new DiskSnapshotStore(dir, mapper);
        for (String bad : new String[]{"../etc", "Order", "order_limits", "a/b", "", "-x"}) {
            assertThatThrownBy(() -> store.read(bad)).as(bad).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
