package com.acme.platform.ratelimit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Gercek redis-server sureci (kanit seviyesi 2). Docker gerektirmez; kalicilik kapali (--save "" --appendonly no)
 * oldugu icin yeniden baslatma = bos Redis + bos script cache: failover/restart sonrasi NOSCRIPT yolunu da sinar.
 */
final class RedisServerProcess {

    static final String BINARY = "/usr/bin/redis-server";

    final int port;
    private final Path dir;
    private Process process;

    private RedisServerProcess(int port, Path dir) { this.port = port; this.dir = dir; }

    static RedisServerProcess startOnFreePort() throws IOException {
        int port;
        try (ServerSocket s = new ServerSocket(0)) { port = s.getLocalPort(); }
        RedisServerProcess r = new RedisServerProcess(port, Files.createTempDirectory("rl-redis"));
        r.start();
        return r;
    }

    void start() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(List.of(BINARY, "--port", Integer.toString(port), "--bind", "127.0.0.1",
                "--save", "", "--appendonly", "no", "--dir", dir.toString()))
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(dir.resolve("redis.log").toFile()));
        process = pb.start();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        // Gercek surecin portu dinlemeye baslamasi icin sinirli yoklama (uygulama mantigi beklenmiyor).
        while (!ping()) {
            if (!process.isAlive() || System.nanoTime() > deadline) {
                throw new IllegalStateException("redis-server baslamadi, log: " + Files.readString(dir.resolve("redis.log")));
            }
            try { Thread.sleep(20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
        }
    }

    /** Redis'in cokmesi: surec olur, TCP baglantilari kapanir. */
    void stop() throws InterruptedException {
        if (process == null) return;
        process.destroyForcibly();
        if (!process.waitFor(10, TimeUnit.SECONDS)) throw new IllegalStateException("redis-server olmedi");
        process = null;
    }

    boolean ping() {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("127.0.0.1", port), 200);
            s.setSoTimeout(500);
            OutputStream out = s.getOutputStream();
            out.write("PING\r\n".getBytes(StandardCharsets.US_ASCII));
            out.flush();
            InputStream in = s.getInputStream();
            byte[] buf = new byte[7];
            int n = in.readNBytes(buf, 0, 7);
            return n == 7 && new String(buf, StandardCharsets.US_ASCII).equals("+PONG\r\n");
        } catch (IOException e) {
            return false;
        }
    }
}
