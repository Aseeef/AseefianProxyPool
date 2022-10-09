package aseef.dev.proxy;

import java.net.Proxy;
import java.util.Optional;

public class AseefianProxy {
    private final ProxySocketAddress proxySocketAddress;
    private final ProxyCredentials proxyCredentials;
    public AseefianProxy(ProxySocketAddress proxySocketAddress, ProxyCredentials credentials) {
        this.proxySocketAddress = proxySocketAddress;
        this.proxyCredentials = credentials;
    }
    public AseefianProxy(ProxySocketAddress proxySocketAddress) {
        this.proxySocketAddress = proxySocketAddress;
        this.proxyCredentials = null;
    }
    public AseefianProxy(String host, int port, Proxy.Type type) {
        this.proxySocketAddress = new ProxySocketAddress(host, port, type);
        this.proxyCredentials = null;
    }
    public AseefianProxy(String host, int port, Proxy.Type type, String username, String password) {
        this.proxySocketAddress = new ProxySocketAddress(host, port, type);
        this.proxyCredentials = new ProxyCredentials(username, password);
    }
    public ProxySocketAddress getProxySocketAddress() {
        return proxySocketAddress;
    }
    public Optional<ProxyCredentials> getProxyCredentials() {
        return Optional.ofNullable(proxyCredentials);
    }
}
