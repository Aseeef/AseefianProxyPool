package com.github.Aseeef;

import com.github.Aseeef.exceptions.ExceptionHandler;
import com.github.Aseeef.exceptions.ProxyConnectionLeakedException;
import com.github.Aseeef.proxy.AseefianProxy;
import com.github.Aseeef.proxy.ProxyCredentials;
import com.github.Aseeef.proxy.ProxyMeta;
import com.github.Aseeef.proxy.ProxySocketAddress;

import java.io.*;
import java.net.Authenticator;
import java.net.Proxy;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ApacheProxyPool {

    private final PoolConfig poolConfig;
    protected final ConcurrentHashMap<ProxySocketAddress, AtomicReference<ProxyMeta>> proxies = new ConcurrentHashMap<>(16, 0.75f, 3);
    private final ProxyAuthenticator authenticator;
    private final AtomicBoolean keepWorking = new AtomicBoolean(true);
    private final HashMap<String, String> defaultSystemSettings = new HashMap<>();

    public ApacheProxyPool(File proxyListFile, Proxy.Type type) throws IOException {
        this(proxyListFile, new PoolConfig(), type);
    }

    public ApacheProxyPool(File proxyListFile, PoolConfig poolConfig, Proxy.Type type) throws IOException {
        assert proxyListFile.exists() && proxyListFile.canRead() : "Error! Unable to access the proxy file list!";

        this.poolConfig = poolConfig;
        this.authenticator = new ProxyAuthenticator(this);
        BufferedReader reader = new BufferedReader(new FileReader(proxyListFile));
        while (reader.ready()) {
            String[] split = reader.readLine().split(":");
            ProxySocketAddress address = new ProxySocketAddress(split[0], Integer.parseInt(split[1]), type);
            ProxyCredentials credentials = null;
            if (split.length > 2) {
                credentials = new ProxyCredentials(split[2], split[3]);
            }
            addProxy(new AseefianProxy(address, credentials));
        }


    }

    public ApacheProxyPool(Collection<AseefianProxy> proxies, PoolConfig poolConfig) {
        this.poolConfig = poolConfig;
        this.authenticator = new ProxyAuthenticator(this);
        for (AseefianProxy proxy : proxies) {
            addProxy(proxy);
        }
    }

    public ApacheProxyPool(AseefianProxy proxy) {
        this(Collections.singleton(proxy), new PoolConfig());
    }

    public ApacheProxyPool addProxy(AseefianProxy proxy) {
        if (proxy.getProxySocketAddress().getType() != Proxy.Type.HTTP && proxy.getProxySocketAddress().getType() != Proxy.Type.SOCKS) {
            throw new IllegalArgumentException("Invalid proxy type " + proxy.getProxySocketAddress().getType() + ". Please use either SOCKS or HTTP proxies!");
        }
        this.proxies.put(proxy.getProxySocketAddress(), new AtomicReference<>(new ProxyMeta(proxy.getProxyCredentials().orElse(null))));
        return this;
    }

    public synchronized ApacheProxyPool init() {
        defaultSystemSettings.put("jdk.http.auth.proxying.disabledSchemes", System.getProperty("jdk.http.auth.proxying.disabledSchemes"));
        defaultSystemSettings.put("jdk.http.auth.tunneling.disabledSchemes", System.getProperty("jdk.http.auth.tunneling.disabledSchemes"));
        System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");

        Authenticator.setDefault(authenticator);

        testProxies();

        if (poolConfig.isTestProxies()) {
            Thread proxyHCThread = new Thread(() -> {
                while (true) {
                    if (keepWorking.get()) {
                        testProxies();
                    }
                    else break;
                }
            });
            proxyHCThread.setName("Proxy Health Check Thread");
            proxyHCThread.setDaemon(true);
            proxyHCThread.setUncaughtExceptionHandler(new ExceptionHandler());
            proxyHCThread.start();
        }

        Thread proxyLeakThread = new Thread(() ->  {
            while (true) {
                if (keepWorking.get()) {
                    for (Map.Entry<ProxySocketAddress, AtomicReference<ProxyMeta>> set : this.proxies.entrySet()) {
                        // skip leak test if its either in the pool already or its dead
                        long timeTaken = set.getValue().get().getTimeTaken();
                        if (timeTaken == -1 || !set.getValue().get().isAlive()) {
                            continue;
                        }
                        // skip leak test for proxies being inspected
                        if (set.getValue().get().isInspecting()) {
                            continue;
                        }
                        if (set.getValue().get().isLeaked() || set.getValue().get().getStackBorrower() == null)
                            continue;
                        if (System.currentTimeMillis() - timeTaken > poolConfig.getConnectionLeakThreshold()) {
                            set.getValue().get().setLeaked(true);
                            new ProxyConnectionLeakedException(
                                    "Detected a proxy leak for the following proxy: " + set.getKey().toString() + " (leaked since " + (System.currentTimeMillis() - timeTaken) + "ms ago)",
                                    set.getValue().get().getStackBorrower()
                            ).printStackTrace();
                        }
                    }
                }
                else break;
            }
        });
        proxyLeakThread.setName("Proxy Leak Test Thread");
        proxyLeakThread.setDaemon(true);
        proxyLeakThread.setUncaughtExceptionHandler(new ExceptionHandler());
        proxyLeakThread.start();

        if (this.getActiveProxies() <= 0) {
            throw new IllegalStateException("Error! Unable to initialize the pool because we were unable to secure any healthy proxies!");
        }

        System.out.println("[Info] Successfully initialized the proxy pool using " + this.getAvailableProxies() + " proxies!");

        return this;
    }

    public synchronized ApacheProxyPool close() {
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
        return (int) this.proxies.values().stream().filter(pm -> pm.get().isInPool()).count();
    }

    /**
     * @return the number of alive proxies which may be either in the pool or outside the pool.
     */
    public int getActiveProxies() {
        return (int) this.proxies.values().stream().filter(pm -> pm.get().isAlive()).count();
    }

    private void testProxies() {
        for (Map.Entry<ProxySocketAddress, AtomicReference<ProxyMeta>> set : this.proxies.entrySet()) {
            ProxySocketAddress proxy = set.getKey();
            ProxyMeta meta = set.getValue().get();

            // skip proxies that were very recently inspected
            if (meta.getLastInspected() > System.currentTimeMillis() - (poolConfig.getMinMillisTestAgo() * 0.7)) {
                continue;
            }
            // skip proxies not in the pool
            if (!meta.isInPool())
                continue;

            meta.setInspecting(true);
            try (ProxyConnection conn = getConnection(proxy, set.getValue())) {
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
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            meta.setLastInspected(System.currentTimeMillis());
            meta.setInspecting(false);
        }
    }

    public synchronized ProxyConnection getConnection() {
        // get the most recently tested proxy
        while (true) {
            Optional<Map.Entry<ProxySocketAddress, AtomicReference<ProxyMeta>>> set = proxies.entrySet().stream()
                    .filter(p -> p.getValue().get().getLastInspected() > System.currentTimeMillis() - poolConfig.getMinMillisTestAgo() && p.getValue().get().isInPool())
                    .max(Comparator.comparingLong(i -> i.getValue().get().getLastInspected()));
            if (set.isPresent()) {
                return getConnection(set.get().getKey(), set.get().getValue());
            }
        }
    }

    private synchronized ProxyConnection getConnection(ProxySocketAddress address, AtomicReference<ProxyMeta> meta) {
            StackTraceElement[] elements = Thread.currentThread().getStackTrace();
            meta.get().setStackBorrower(Arrays.copyOfRange(elements, 2, elements.length));
            meta.get().setTimeTaken(System.currentTimeMillis());
            return new ProxyConnection(this, address);
    }



}
