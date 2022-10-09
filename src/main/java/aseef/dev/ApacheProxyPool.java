package aseef.dev;

import aseef.dev.proxy.AseefianProxy;
import aseef.dev.proxy.ProxySocketAddress;
import aseef.dev.proxy.ProxyCredentials;

import java.net.Authenticator;
import java.net.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

public class ApacheProxyPool {

    private final PoolConfig poolConfig;
    protected final HashMap<ProxySocketAddress, ProxyCredentials> proxies;
    private final ProxyAuthenticator authenticator;

    public ApacheProxyPool(Collection<AseefianProxy> proxies, PoolConfig poolConfig) {
        assert proxies != null;
        assert !proxies.isEmpty();

        this.poolConfig = poolConfig;
        System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        this.proxies = new HashMap<>();
        for (AseefianProxy proxy : proxies) {
            if (proxy.getProxySocketAddress().getType() != Proxy.Type.HTTP && proxy.getProxySocketAddress().getType() != Proxy.Type.SOCKS) {
                throw new IllegalArgumentException("Invalid proxy type " + proxy.getProxySocketAddress().getType() + ". Please use either SOCKS or HTTP proxies!");
            }
            this.proxies.put(proxy.getProxySocketAddress(), proxy.getProxyCredentials().orElse(null));
        }
        this.authenticator = new ProxyAuthenticator(this);
        Authenticator.setDefault(authenticator);
    }

    public ApacheProxyPool(AseefianProxy proxy) {
        this(Collections.singleton(proxy), new PoolConfig());
    }

    private void testProxies() {

    }

    protected void returnProxy(ProxySocketAddress proxy) {

    }

    public ProxyConnection getConnection() {
        return getConnection(proxies.keySet().stream().findAny().get());
    }

    private ProxyConnection getConnection(ProxySocketAddress address) {
        return new ProxyConnection(this, address);
    }



}
