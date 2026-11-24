package com.xinke.erp.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class LiveScheduleGate {
    private final String activePortFile;
    private final int port;

    public LiveScheduleGate(Environment environment) {
        activePortFile=environment.getProperty("live.scheduling.active-port-file","");
        port=environment.getProperty("server.port",Integer.class,8080);
    }

    public boolean isActive() {
        if(activePortFile.isBlank())return true;
        try {
            return Integer.parseInt(Files.readString(Path.of(activePortFile)).trim())==port;
        } catch(Exception exception) {
            return false;
        }
    }
}
