package com.github.Aseeef.wrappers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor @Getter @Setter @ToString
public class ProxyHealthReport {
    private volatile long lastTested;
    private volatile long millisResponseTime;
    public boolean isAlive() {
        // lastTested=-1 means this proxy has never been tested before
        // so we assume the proxy is alive.
        // millisResponseTime=-1 EITHER means the proxy is dead OR that it has never been tested before
        return lastTested == -1 || millisResponseTime != -1;
    }
}
