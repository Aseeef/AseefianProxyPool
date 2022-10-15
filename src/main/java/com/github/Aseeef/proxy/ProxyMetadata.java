package com.github.Aseeef.proxy;

import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;

public class ProxyMetadata extends ConcurrentHashMap<String, Object> {
    @Getter
    private final ProxyHealthReport proxyHealthReport;
    protected ProxyMetadata(ProxyHealthReport proxyHealthReport) {
        this.proxyHealthReport = proxyHealthReport;
    }
}
