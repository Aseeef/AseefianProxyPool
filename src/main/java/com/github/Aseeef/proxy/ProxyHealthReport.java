package com.github.Aseeef.proxy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class ProxyHealthReport {
    private final long lastTested;
    private final long millisResponseTime;
    public boolean isAlive() {
        return millisResponseTime != -1;
    }
}
