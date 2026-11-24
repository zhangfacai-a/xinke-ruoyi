package com.xinke.erp.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

class LiveScheduleGateTest {
    @TempDir Path directory;

    @Test void singleInstanceIsActiveWithoutMarker() {
        assertTrue(new LiveScheduleGate(new MockEnvironment()).isActive());
    }

    @Test void onlyMarkedPortRunsTasks() throws Exception {
        Path marker=directory.resolve("active-port");
        MockEnvironment environment=new MockEnvironment()
            .withProperty("server.port","8081")
            .withProperty("live.scheduling.active-port-file",marker.toString());
        LiveScheduleGate gate=new LiveScheduleGate(environment);
        assertFalse(gate.isActive());
        Files.writeString(marker,"8080\n");
        assertFalse(gate.isActive());
        Files.writeString(marker,"8081\n");
        assertTrue(gate.isActive());
    }
}
