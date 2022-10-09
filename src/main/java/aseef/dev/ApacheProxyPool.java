package aseef.dev;

import aseef.dev.exceptions.ProxyConnectionLeakedException;
import aseef.dev.proxy.AseefianProxy;
import aseef.dev.proxy.ProxyMeta;
import aseef.dev.proxy.ProxySocketAddress;
import aseef.dev.exceptions.ExceptionHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.Proxy;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ApacheProxyPool {

    private final PoolConfig poolConfig;
    protected final HashMap<ProxySocketAddress, ProxyMeta> proxies;
    private final ProxyAuthenticator authenticator;
    private final AtomicBoolean keepWorking = new AtomicBoolean(true);
    private final HashMap<String, String> defaultSystemSettings = new HashMap<>();

    public ApacheProxyPool(Collection<AseefianProxy> proxies, PoolConfig poolConfig) {
        assert proxies != null;
        assert !proxies.isEmpty();

        this.poolConfig = poolConfig;
        this.proxies = new HashMap<>();
        for (AseefianProxy proxy : proxies) {
            addProxy(proxy);
        }
        this.authenticator = new ProxyAuthenticator(this);
    }

    public ApacheProxyPool addProxy(AseefianProxy proxy) {
        if (proxy.getProxySocketAddress().getType() != Proxy.Type.HTTP && proxy.getProxySocketAddress().getType() != Proxy.Type.SOCKS) {
            throw new IllegalArgumentException("Invalid proxy type " + proxy.getProxySocketAddress().getType() + ". Please use either SOCKS or HTTP proxies!");
        }
        this.proxies.put(proxy.getProxySocketAddress(), new ProxyMeta(proxy.getProxyCredentials().orElse(null)));
        return this;
    }

    public ApacheProxyPool init() {
        defaultSystemSettings.put("jdk.http.auth.proxying.disabledSchemes", System.getProperty("jdk.http.auth.proxying.disabledSchemes"));
        defaultSystemSettings.put("jdk.http.auth.tunneling.disabledSchemes", System.getProperty("jdk.http.auth.tunneling.disabledSchemes"));
        System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        Authenticator.setDefault(authenticator);

        testProxies();

        if (poolConfig.isTestProxies()) {
            Thread proxyHCThread = new Thread(() -> {
                while (true) {
                    if (keepWorking.get())
                        testProxies();
                    else break;
                }
            });
            proxyHCThread.setName("Proxy Health Check Thread");
            proxyHCThread.setDaemon(true);
            proxyHCThread.setUncaughtExceptionHandler(new ExceptionHandler());
            proxyHCThread.start();
        }

        Thread proxyHCThread = new Thread(() -> {
            while (true) {
                if (keepWorking.get()) {
                    for (Map.Entry<ProxySocketAddress, ProxyMeta> set : this.proxies.entrySet()) {
                        if (!set.getValue().isLeaked() && set.getValue().getTimeTaken() > System.currentTimeMillis() + poolConfig.getConnectionLeakThreshold()) {
                            set.getValue().setLeaked(true);
                            new ProxyConnectionLeakedException("Detected a proxy leak for the following proxy: " + set.getKey().toString()).printStackTrace();
                        }
                    }
                }
                else break;
            }
        });
        proxyHCThread.setName("Proxy Leak Test Thread");
        proxyHCThread.setDaemon(true);
        proxyHCThread.setUncaughtExceptionHandler(new ExceptionHandler());
        proxyHCThread.start();

        return this;
    }

    public ApacheProxyPool close() {
        keepWorking.set(false);
        Authenticator.setDefault(null);
        for (Map.Entry<String, String> set : defaultSystemSettings.entrySet()) {
            System.setProperty(set.getKey(), set.getValue());
        }
        return this;
    }

    /**
     * @return the number of proxies available in the pool
     */
    public int getAvailableProxies() {
        return (int) this.proxies.values().stream().filter(ProxyMeta::isInPool).count();
    }

    /**
     * @return the number of alive proxies which may be either in the pool or outside the pool.
     */
    public int getActiveProxies() {
        return (int) this.proxies.values().stream().filter(ProxyMeta::isAlive).count();
    }

    public ApacheProxyPool(AseefianProxy proxy) {
        this(Collections.singleton(proxy), new PoolConfig());
    }

    private void testProxies() {
        for (ProxySocketAddress proxy : this.proxies.keySet()) {
            try (ProxyConnection conn = getConnection(proxy)) {
                ProxyMeta meta = this.proxies.get(proxy);
                if (meta.getLastInspected() > System.currentTimeMillis() - poolConfig.getTestFreqMillis()) {
                    continue;
                }
                URLConnection connection = conn.connect("http://checkip.amazonaws.com");
                connection.setConnectTimeout(poolConfig.getProxyTimeoutMillis());
                try (InputStream is = connection.getInputStream()) {
                    byte[] targetArray = new byte[is.available()];
                    is.read(targetArray);
                    assert new String(targetArray).equals(proxy.getHost());
                    meta.setAlive(true);
                } catch (Exception ex) {
                    meta.setAlive(false);
                }
                meta.setLastInspected(System.currentTimeMillis());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private ProxySocketAddress requestProxy() {
        // get the most recently tested proxy
        return null;
    }

    public ProxyConnection getConnection() {
        return getConnection(proxies.keySet().stream().findAny().get());
    }

    private ProxyConnection getConnection(ProxySocketAddress address) {
        return new ProxyConnection(this, address);
    }



}
