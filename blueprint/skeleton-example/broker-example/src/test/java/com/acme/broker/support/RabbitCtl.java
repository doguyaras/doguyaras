package com.acme.broker.support;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Kaos testi icin rabbitmqctl stop_app / start_app (node kalir, broker uygulamasi durur). Komut ve node adi sistem
 * ozelligi/ortam degiskeniyle verilir; bulunamazsa test acik bir mesajla BASARISIZ olur (sessiz atlama yok).
 *   -Dbvt.rabbitmqctl=/path/to/rabbitmqctl  -Dbvt.rabbitmq.node=rabbit@localhost  -Dbvt.erlang.bin=/opt/otp27/bin
 * Komut bosluk icerebilir (container icindeki broker icin): BVT_RABBITMQCTL="docker exec <container> rabbitmqctl";
 * ilk kelime '/' icermiyorsa PATH'te aranir. BVT_RABBITMQ_NODE=local ise "-n" verilmez (container icinde yerel node).
 */
public final class RabbitCtl {

    private final List<String> rabbitmqctl;
    private final String node;
    private final String erlangBin;

    public RabbitCtl() {
        this.rabbitmqctl = List.of(prop("bvt.rabbitmqctl", "BVT_RABBITMQCTL", "/home/pgtest/rmq/rabbitmq_server-4.3.0/sbin/rabbitmqctl").trim().split("\\s+"));
        this.node = prop("bvt.rabbitmq.node", "BVT_RABBITMQ_NODE", "rabbit@localhost");
        this.erlangBin = prop("bvt.erlang.bin", "BVT_ERLANG_BIN", "/opt/otp27/bin");
    }

    public boolean available() {
        String exe = rabbitmqctl.get(0);
        if (exe.contains("/")) return new File(exe).canExecute();
        for (String dir : (erlangBin + File.pathSeparator + System.getenv().getOrDefault("PATH", "/usr/bin:/bin")).split(File.pathSeparator)) {
            if (!dir.isBlank() && new File(dir, exe).canExecute()) return true;
        }
        return false;
    }

    public String describe() { return String.join(" ", rabbitmqctl) + (localNode() ? "" : " -n " + node); }

    private boolean localNode() { return "local".equalsIgnoreCase(node); }

    public void stopApp() throws IOException, InterruptedException { run("stop_app"); }

    public void startApp() throws IOException, InterruptedException { run("start_app"); }

    private void run(String command) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(rabbitmqctl);
        if (!localNode()) { cmd.add("-n"); cmd.add(node); }
        cmd.add(command);
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        pb.environment().put("PATH", erlangBin + File.pathSeparator + System.getenv().getOrDefault("PATH", "/usr/bin:/bin"));
        pb.environment().putIfAbsent("HOME", System.getProperty("user.home"));
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!p.waitFor(60, TimeUnit.SECONDS)) { p.destroyForcibly(); throw new IOException("rabbitmqctl " + command + " timed out"); }
        if (p.exitValue() != 0) throw new IOException("rabbitmqctl " + command + " failed (" + p.exitValue() + "): " + out);
    }

    private static String prop(String sys, String env, String def) {
        String v = System.getProperty(sys);
        if (v == null || v.isBlank()) v = System.getenv(env);
        return v == null || v.isBlank() ? def : v;
    }
}
