package com.acme.dbsecurity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/**
 * Test yardimcisi: gercek pgbouncer surecini (varsayilan /usr/sbin/pgbouncer, -Dpgbouncer.bin ile degistirilir)
 * uretilmis bir ini ile on planda baslatir ve durdurur. Seviye 3 kanit: JDBC -> pgbouncer -> PostgreSQL, uc surec.
 */
final class PgBouncerProcess implements AutoCloseable {

    /** Ini'ye giren, testin degistirdigi ayarlar; geri kalani sabit. */
    record Config(int maxPreparedStatements, int defaultPoolSize) { }

    private final Process process;
    private final Path dir;
    final int port;

    private PgBouncerProcess(Process process, Path dir, int port) {
        this.process = process;
        this.dir = dir;
        this.port = port;
    }

    static PgBouncerProcess start(int pgPort, String dbName, String user, String password, Config cfg) throws IOException {
        Path bin = Path.of(System.getProperty("pgbouncer.bin", "/usr/sbin/pgbouncer"));
        if (!Files.isExecutable(bin)) throw new IllegalStateException("pgbouncer binary yok: " + bin);
        Path dir = Files.createTempDirectory("pgbouncer-it");
        int port = freePort();
        // auth_type=plain: userlist'teki duz sifre ile istemci dogrulanir; sunucuya ayni sifreyle SCRAM yapilir.
        Files.writeString(dir.resolve("userlist.txt"), "\"" + user + "\" \"" + password + "\"\n", StandardCharsets.UTF_8);
        String ini = """
                [databases]
                %s = host=127.0.0.1 port=%d dbname=%s
                [pgbouncer]
                listen_addr = 127.0.0.1
                listen_port = %d
                unix_socket_dir = %s
                auth_type = plain
                auth_file = %s
                pool_mode = transaction
                max_prepared_statements = %d
                min_pool_size = 0
                default_pool_size = %d
                ignore_startup_parameters = extra_float_digits
                logfile = %s
                pidfile = %s
                """.formatted(dbName, pgPort, dbName, port, dir, dir.resolve("userlist.txt"), cfg.maxPreparedStatements(),
                cfg.defaultPoolSize(), dir.resolve("pgbouncer.log"), dir.resolve("pgbouncer.pid"));
        Path iniFile = dir.resolve("pgbouncer.ini");
        Files.writeString(iniFile, ini, StandardCharsets.UTF_8);
        Path out = dir.resolve("stdout-stderr.log");
        Process p = new ProcessBuilder(bin.toString(), iniFile.toString()).redirectErrorStream(true)
                .redirectOutput(out.toFile()).start();
        // Gercek zamanli altyapi: port acilana kadar sinirli bekleme (uygulama mantigi degil, surec baslangici)
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            if (!p.isAlive()) {
                throw new IllegalStateException("pgbouncer basladiktan hemen sonra cikti (exit " + p.exitValue() + "):\n"
                        + Files.readString(out));
            }
            try (Socket s = new Socket(InetAddress.getLoopbackAddress(), port)) {
                return new PgBouncerProcess(p, dir, port);
            } catch (IOException notYet) {
                try { Thread.sleep(50); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        p.destroyForcibly();
        throw new IllegalStateException("pgbouncer 10 sn icinde port acmadi:\n" + Files.readString(out));
    }

    String jdbcUrl(String dbName) { return "jdbc:postgresql://127.0.0.1:" + port + "/" + dbName; }

    String log() throws IOException {
        Path log = dir.resolve("pgbouncer.log");
        return Files.exists(log) ? Files.readString(log) : Files.readString(dir.resolve("stdout-stderr.log"));
    }

    private static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) { return s.getLocalPort(); }
    }

    @Override
    public void close() {
        process.destroy();
        try {
            if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
        deleteDir();
    }

    /** ini, log ve duz metin sifreli userlist.txt /tmp'de birikmesin. */
    private void deleteDir() {
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException e) { throw new UncheckedIOException(e); }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
