package com.da.web.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Logger {
    private static final String DEFAULT_LOG = "DApp";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ConcurrentLinkedQueue<String> LOG_QUEUE = new ConcurrentLinkedQueue<>();
    private static volatile boolean enabled = true;

    static {
        Thread daemon = new Thread(() -> {
            while (true) {
                StringBuilder sb = new StringBuilder();
                String msg;
                while ((msg = LOG_QUEUE.poll()) != null) {
                    sb.append(msg);
                }
                if (sb.length() > 0) {
                    System.out.print(sb);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        daemon.setDaemon(true);
        daemon.start();
    }

    public static void error(Class<?> clazz, Exception e) {
        if (!enabled) return;
        log(clazz, "ERROR", getStackTrace(e));
    }

    public static void error(Class<?> clazz, String msg) {
        if (!enabled) return;
        log(clazz, "ERROR", msg);
    }
    
    public static void error(Class<?> clazz, String msg, Exception e) {
        if (!enabled) return;
        log(clazz, "ERROR", msg + ": " + getStackTrace(e));
    }

    public static void warn(Class<?> clazz, String msg) {
        if (!enabled) return;
        log(clazz, "WARN", msg);
    }

    public static void info(Class<?> clazz, String msg) {
        if (!enabled) return;
        log(clazz, "INFO", msg);
    }

    public static void debug(Class<?> clazz, String msg) {
        if (!enabled) return;
        log(clazz, "DEBUG", msg);
    }

    public static void setEnabled(boolean enabled) {
        Logger.enabled = enabled;
    }

    public static void init(String logEnabled) {
        if (logEnabled != null && "false".equalsIgnoreCase(logEnabled.trim())) {
            enabled = false;
        }
    }

    private static void log(Class<?> clazz, String level, String msg) {
        String time = LocalDateTime.now().format(FORMATTER);
        String name = clazz == null ? DEFAULT_LOG : clazz.getSimpleName();
        LOG_QUEUE.offer(String.format("[%s] [%s] [%s] %s%n", time, level, name, msg));
    }

    private static String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}