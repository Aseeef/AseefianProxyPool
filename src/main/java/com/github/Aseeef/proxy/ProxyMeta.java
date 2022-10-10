package com.github.Aseeef.proxy;

import lombok.Getter;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Getter
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
    private AtomicLong timeTaken = new AtomicLong(-1);

    /**
     * The reference from the stack of who took the connection
     */
    private AtomicReference<StackTraceElement[]> stackBorrower = new AtomicReference<>();

    /**
     * The last time that this proxy was validated to be working in epoch millis. A value of -1 indicates that this proxy has never been tested.
     */
    private AtomicLong lastInspected = new AtomicLong(-1);

    /**
     * Whether this proxy is alive
     */
    private AtomicBoolean alive = new AtomicBoolean(true);

    /**
     * Whether this proxy is a leaked proxy
     */
    private AtomicBoolean leaked = new AtomicBoolean(false);

    /**
     * Whether this proxy is currently being inspected. Inspected proxies cannot be leaked.
     */
    private AtomicBoolean inspecting = new AtomicBoolean(false);

    /**
     * @return true if in the pool and false otherwise
     */
    public boolean isInPool() {
        return this.timeTaken.get() == -1 && this.alive.get();
    }

    public Optional<ProxyCredentials> getCredentials() {
        return Optional.ofNullable(credentials);
    }
}
