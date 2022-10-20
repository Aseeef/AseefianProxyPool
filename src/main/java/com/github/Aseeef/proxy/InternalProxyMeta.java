package com.github.Aseeef.proxy;

import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;

import java.util.Optional;

@Getter(onMethod_ = {@Synchronized}) @Setter(onMethod_ = {@Synchronized})
public class InternalProxyMeta {

    public InternalProxyMeta(ProxyCredentials credentials) {
        this.credentials = credentials;
    }

    /**
     * The credentials for this proxy (may be null).
     */
    private final ProxyCredentials credentials;

    /**
     * The last time that this proxy was validated to be working in epoch millis. A value of null indicates that this proxy has never been tested.
     */
    private final ProxyHealthReport latestHealthReport = new ProxyHealthReport(-1L, -1L);

    /**
     * User defined metadata information for this proxy
     */
    private final ProxyMetadata metadata = new ProxyMetadata(latestHealthReport);

    /**
     * The time at which this proxy was taken last taken from the pool in epoch millis. A value of -1 indicates that this proxy has never been used.
     */
    private volatile long timeTaken = -1L;

    private volatile boolean inPool = true;

    /**
     * The reference from the stack of who took the connection
     */
    private volatile StackTraceElement[] stackBorrower = null;

    /**
     * Whether to skip proxy leak test
     */
    private volatile boolean skipLeakTest = false;

    /**
     * Whether this proxy is a leaked proxy
     */
    private volatile boolean leaked = false;

    /**
     * Whether this proxy is currently being inspected. Inspected proxies cannot be leaked.
     */
    private volatile boolean inspecting = false;

    /**
     * @return true if in the pool and false otherwise
     */
    public synchronized boolean isInPool() {
        return this.inPool && this.isAlive();
    }

    /**
     * @return whether this proxy is alive based on the latest tests. If no health report tests are available, defaults to true.
     */
    public synchronized boolean isAlive() {
        return this.latestHealthReport.isAlive();
    }

    public Optional<ProxyCredentials> getCredentials() {
        return Optional.ofNullable(credentials);
    }
}
