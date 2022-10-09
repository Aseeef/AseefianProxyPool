package aseef.dev.proxy;

import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter @Setter
public class ProxyMeta {

    public ProxyMeta(ProxyCredentials credentials) {
        this.credentials = credentials;
    }

    /**
     * The credentials for this proxy (may be null).
     */
    private ProxyCredentials credentials;

    /**
     * The time at which this proxy was taken from the pool in epoch millis. A value of -1 indicates that this proxy is currently with in the pool.
     */
    private long timeTaken = -1;

    /**
     * The last time that this proxy was validated to be working in epoch millis. A value of -1 indicates that this proxy has never been tested.
     */
    private long lastInspected = -1;

    /**
     * Whether this proxy is alive
     */
    private boolean alive = false;

    /**
     * Whether this proxy is a leaked proxy
     */
    private boolean leaked = false;

    /**
     * @return true if in the pool and false otherwise
     */
    public boolean isInPool() {
        return timeTaken == -1;
    }

    public Optional<ProxyCredentials> getCredentials() {
        return Optional.ofNullable(credentials);
    }
}
