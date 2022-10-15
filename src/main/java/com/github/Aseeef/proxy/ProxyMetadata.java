package com.github.Aseeef.proxy;

import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.ConcurrentHashMap;

@ToString(callSuper = true)
public class ProxyMetadata extends ConcurrentHashMap<String, Object> {
    /**
     * The most recent health report on this proxy containing the time of the test and the latency of the proxy in that test
     */
    @Getter
    private final ProxyHealthReport proxyHealthReport;
    protected ProxyMetadata(ProxyHealthReport proxyHealthReport) {
        this.proxyHealthReport = proxyHealthReport;
    }
}
