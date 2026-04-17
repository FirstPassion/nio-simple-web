package com.da.web.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class LoggerTest {

    @Test
    public void testInitDefaultEnabled() {
        Logger.init(null);
        Logger.error(LoggerTest.class, "test");
        Logger.warn(LoggerTest.class, "test");
        Logger.info(LoggerTest.class, "test");
    }

    @Test
    public void testInitExplicitEnabled() {
        Logger.init("true");
        Logger.error(LoggerTest.class, "test");
    }

    @Test
    public void testInitDisabled() {
        Logger.init("false");
        Logger.setEnabled(true);
    }

    @Test
    public void testSetEnabled() {
        Logger.setEnabled(false);
        Logger.setEnabled(true);
    }

    @Test
    public void testErrorWithException() {
        Logger.init(null);
        Exception e = new RuntimeException("test error");
        Logger.error(LoggerTest.class, e);
    }

    @Test
    public void testErrorWithMessage() {
        Logger.init(null);
        Logger.error(LoggerTest.class, "error message");
    }

    @Test
    public void testWarnMessage() {
        Logger.init(null);
        Logger.warn(LoggerTest.class, "warn message");
    }

    @Test
    public void testInfoMessage() {
        Logger.init(null);
        Logger.info(LoggerTest.class, "info message");
    }
}