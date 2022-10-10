package com.github.Aseeef.proxy;

import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;

import java.util.Optional;

@Getter(onMethod_ = {@Synchronized}) @Setter(onMethod_ = {@Synchronized})
public class ProxyMeta {

    public ProxyMeta(ProxyCredentials credentials) {
        this.credentials = credentials;
    }

    /**
     * The credentials for this proxy (may be null).
     */
    private final ProxyCredentials credentials;

    /**
     * The time at which this proxy was taken from the pool in epoch millis. A value of -1 indicates that this proxy is currently with in the pool.
     */
    private volatile long timeTaken = -1L;

    /**
     * The reference from the stack of who took the connection
     */
    private volatile StackTraceElement[] stackBorrower = null;

    /**
     * The last time that this proxy was validated to be working in epoch millis. A value of -1 indicates that this proxy has never been tested.
     */
    private volatile long lastInspected = -1L;

    /**
     * Whether this proxy is alive
     */
    private volatile boolean alive = true;

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
    public boolean isInPool() {
        return this.timeTaken == -1 && this.alive;
    }

    public Optional<ProxyCredentials> getCredentials() {
        return Optional.ofNullable(credentials);
    }
}
