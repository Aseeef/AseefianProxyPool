package com.github.Aseeef;

import com.github.Aseeef.exceptions.ExceptionHandler;
import com.github.Aseeef.exceptions.ProxyConnectionLeakedException;
import com.github.Aseeef.exceptions.ProxyPoolExhaustedException;
import com.github.Aseeef.proxy.*;

import java.io.*;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AseefianProxyPool {

    private final PoolConfig poolConfig;
    protected final ConcurrentHashMap<ProxySocketAddress, InternalProxyMeta> proxies = new ConcurrentHashMap<>(16, 0.75f, 3);
    private final ProxyAuthenticator authenticator;
    private final HashMap<String, String> defaultSystemSettings = new HashMap<>();

    private ScheduledExecutorService leakTesterService;
    private ScheduledExecutorService proxyHealthTask;
    private ExecutorService proxyHealthTestThreadPool;

    public AseefianProxyPool(File proxyListFile, Proxy.Type type) throws IOException {
        this(proxyListFile, new PoolConfig(), type);
    }

    public AseefianProxyPool(File proxyListFile, PoolConfig poolConfig, Proxy.Type type) throws IOException {
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

    public AseefianProxyPool(Collection<AseefianProxy> proxies, PoolConfig poolConfig) {
        this.poolConfig = poolConfig;
        this.authenticator = new ProxyAuthenticator(this);
        for (AseefianProxy proxy : proxies) {
            addProxy(proxy);
        }
    }

    public AseefianProxyPool(AseefianProxy proxy) {
        this(Collections.singleton(proxy), new PoolConfig());
    }

    public AseefianProxyPool addProxy(AseefianProxy proxy) {
        if (proxy.getProxySocketAddress().getType() != Proxy.Type.HTTP && proxy.getProxySocketAddress().getType() != Proxy.Type.SOCKS) {
            throw new IllegalArgumentException("Invalid proxy type " + proxy.getProxySocketAddress().getType() + ". Please use either SOCKS or HTTP proxies!");
        }
        this.proxies.put(proxy.getProxySocketAddress(), new InternalProxyMeta(proxy.getProxyCredentials().orElse(null)));
        return this;
    }

    public synchronized AseefianProxyPool init() {
        defaultSystemSettings.put("jdk.http.auth.proxying.disabledSchemes", System.getProperty("jdk.http.auth.proxying.disabledSchemes"));
        defaultSystemSettings.put("jdk.http.auth.tunneling.disabledSchemes", System.getProperty("jdk.http.auth.tunneling.disabledSchemes"));
        System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        Authenticator.setDefault(authenticator);

        proxyHealthTestThreadPool = new ThreadPoolExecutor(0, (int) Math.ceil(poolConfig.getMaxConcurrency() * 0.5), 60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                new ThreadFactory() {
                    private int i = 0;
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread proxyHTT = new Thread(r);
                        proxyHTT.setName("Health Check Thread #" + (i++));
                        proxyHTT.setDaemon(true);
                        proxyHTT.setUncaughtExceptionHandler(new ExceptionHandler());
                        return proxyHTT;
                    }
                });
        testProxies();

        if (poolConfig.isTestProxies()) {
            proxyHealthTask = Executors.newScheduledThreadPool(1, r -> createThread("Proxy Health Task", r));
            proxyHealthTask.scheduleAtFixedRate(this::testProxies, 0, poolConfig.getLeakTestFrequencyMillis(), TimeUnit.MILLISECONDS);
        }

        // todo: 1 thread should be enough for this.. right?
        if (poolConfig.getConnectionLeakThreshold() > 0) {
            leakTesterService = Executors.newScheduledThreadPool(1, r -> createThread("Proxy Leak Test", r));
            leakTesterService.scheduleAtFixedRate(() -> {
                for (Map.Entry<ProxySocketAddress, InternalProxyMeta> set : this.proxies.entrySet()) {
                    // skip leak test if its either in the pool already or its dead
                    InternalProxyMeta meta = set.getValue();
                    long timeTaken = meta.getTimeTaken();
                    // skip test for proxies in the pool, dead proxies, proxies with meta "skip leak" and proxies being inspected
                    if (timeTaken == -1 || !meta.isAlive() || meta.isInspecting() || meta.isSkipLeakTest()) {
                        continue;
                    }
                    // skip test for already leaked proxies
                    if (set.getValue().isLeaked() || set.getValue().getStackBorrower() == null)
                        continue;

                    if (System.currentTimeMillis() - timeTaken > poolConfig.getConnectionLeakThreshold()) {
                        meta.setLeaked(true);
                        new ProxyConnectionLeakedException(
                                "Detected a proxy leak for the following proxy: " + set.getKey().toString() + " (leaked since " + (System.currentTimeMillis() - timeTaken) + "ms ago)",
                                meta.getStackBorrower()
                        ).printStackTrace();
                    }
                }
            }, 0, poolConfig.getLeakTestFrequencyMillis(), TimeUnit.MILLISECONDS);
        }

        if (this.getActiveProxies() <= 0) {
            throw new IllegalStateException("Error! Unable to initialize the pool because we were unable to secure any healthy proxies!");
        }

        System.out.println("[Info] Successfully initialized the proxy pool using " + this.getAvailableProxies() + " proxies!");

        return this;
    }

    private static Thread createThread(String name, Runnable r) {
        Thread thread = new Thread(r);
        thread.setName(name);
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler(new ExceptionHandler());
        return thread;
    }

    public synchronized AseefianProxyPool close() {
        Authenticator.setDefault(null);
        for (Map.Entry<String, String> set : defaultSystemSettings.entrySet()) {
            System.setProperty(set.getKey(), set.getValue());
        }
        if (leakTesterService != null)
            leakTesterService.shutdown();
        if (proxyHealthTask != null)
            proxyHealthTask.shutdown();
        proxyHealthTestThreadPool.shutdown();
        return this;
    }

    /**
     * @return the number of proxies available in the pool
     */
    public int getAvailableProxies() {
        return (int) this.proxies.values().stream().filter(InternalProxyMeta::isInPool).count();
    }

    /**
     * @return the number of alive proxies which may be either in the pool or outside the pool.
     */
    public int getActiveProxies() {
        return (int) this.proxies.values().stream().filter(pm -> pm.isAlive()).count();
    }

    private void testProxies() {
        List<Future<?>> futures = new ArrayList<>(this.proxies.size());
        for (Map.Entry<ProxySocketAddress, InternalProxyMeta> set : this.proxies.entrySet()) {
            // each proxy ping can take a while so it's best to submit each test in a new thread
            // for fastest and most efficient execution
            Future<?> future = proxyHealthTestThreadPool.submit(() -> {
                ProxySocketAddress proxy = set.getKey();
                InternalProxyMeta meta = set.getValue();
                // skip proxies that were very recently inspected
                if (meta.getLatestHealthReport() != null && meta.getLatestHealthReport().getLastTested() > System.currentTimeMillis() - (poolConfig.getMinMillisTestAgo() * 0.75)) {
                    return;
                }
                // skip proxies not in the pool (but dont skip "dead proxies")
                if (meta.getTimeTaken() != -1)
                    return;

                meta.setInspecting(true);
                long ping;
                try (ProxyConnection conn = getConnection(proxy, set.getValue())) {
                    // using this ip testing method because
                    // 1. amazon stays reliably online
                    // 2. multiple servers around the world
                    // 3. uses little bandwith
                    ping = System.currentTimeMillis();
                    HttpURLConnection connection = (HttpURLConnection) conn.connect("http://checkip.amazonaws.com");
                    connection.setConnectTimeout(poolConfig.getProxyTimeoutMillis());
                    try (InputStream is = connection.getInputStream()) {
                        byte[] targetArray = new byte[is.available()];
                        is.read(targetArray);
                        assert new String(targetArray).equals(proxy.getHost());
                        ping = System.currentTimeMillis() - ping;
                    }
                } catch (Exception ex) {
                    if (!(ex instanceof SocketTimeoutException))
                        ex.printStackTrace();
                    ping = -1;
                }
                meta.setLatestHealthReport(new ProxyHealthReport(System.currentTimeMillis(), ping));
                meta.setInspecting(false);
            });
            futures.add(future);
        }
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * @return a copy of the list of all registered proxies.
     * There are no guarantees on the type, mutability, serializability, or thread-safety of the Map returned.
     */
    public Map<ProxySocketAddress, ProxyMetadata> getAllProxies() {
        return proxies.entrySet().stream().map((kv) -> Map.entry(kv.getKey(), kv.getValue().getMetadata())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public ProxyConnection getConnection() {
        return getConnection((pm) -> true, poolConfig.getDefaultConnectionWaitMillis());
    }

    public ProxyConnection getConnection(long connectionWaitMillis) {
        return getConnection((pm) -> true, connectionWaitMillis);
    }

    public ProxyConnection getConnection(Predicate<ProxyMetadata> predicate, long connectionWaitMillis) {
        long start = System.currentTimeMillis();
        while (true) {
            Optional<Map.Entry<ProxySocketAddress, InternalProxyMeta>> set = proxies.entrySet().stream()
                    .filter(p -> predicate.test(p.getValue().getMetadata()) &&
                            p.getValue().getLatestHealthReport().getLastTested() > System.currentTimeMillis() - poolConfig.getMinMillisTestAgo() &&
                            p.getValue().isInPool())
                    .min(Comparator.comparingLong(i -> i.getValue().getLatestHealthReport().getMillisResponseTime())); // get the proxy with the lowest response time first
            if (set.isPresent()) {
                return getConnection(set.get().getKey(), set.get().getValue());
            }
            if (connectionWaitMillis > 0 && System.currentTimeMillis() - start > connectionWaitMillis) {
                throw new ProxyPoolExhaustedException("Unable to obtain a proxy connection!");
            }
        }
    }

    private ProxyConnection getConnection(ProxySocketAddress address, InternalProxyMeta meta) {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        meta.setStackBorrower(Arrays.copyOfRange(elements, 2, elements.length));
        meta.setTimeTaken(System.currentTimeMillis());
        while(poolConfig.getMaxConcurrency() > 0 && getAvailableProxies() - getActiveProxies() > poolConfig.getMaxConcurrency()) {
            // getAvailableProxies() - getActiveProxies() = proxies outside the pool
            // so we wait until the proxies outside the pool are less than max concurrency
        }
        return new ProxyConnection(this, address);
    }


}
