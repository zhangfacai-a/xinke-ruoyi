package com.xinke.erp.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class LiveScheduleGate {
    private final String activePortFile;
    private final int port;
    private final boolean enabled;

    public LiveScheduleGate(Environment environment) {
        activePortFile=environment.getProperty("live.scheduling.active-port-file","");
        port=environment.getProperty("server.port",Integer.class,8080);
        enabled=environment.getProperty("live.scheduling.enabled",Boolean.class,true);
    }

    public boolean isActive() {
        if(!enabled)return false;
        if(activePortFile.isBlank())return true;
        try {
            return Integer.parseInt(Files.readString(Path.of(activePortFile)).trim())==port;
        } catch(Exception exception) {
            return false;
        }
    }
}
