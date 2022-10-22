package com.github.Aseeef;

import com.github.Aseeef.exceptions.ExceptionHandler;
import com.github.Aseeef.exceptions.ProxyPoolExhaustedException;
import com.github.Aseeef.wrappers.*;
import lombok.SneakyThrows;

import java.io.*;
import java.net.Authenticator;
import java.net.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AseefianProxyPool {

    protected final PoolConfig poolConfig;
    protected final ConcurrentHashMap<ProxySocketAddress, InternalProxyMeta> proxies = new ConcurrentHashMap<>(16, 0.75f, 3);
    protected final ProxyAuthenticator authenticator;
    protected final HashMap<String, String> defaultSystemSettings = new HashMap<>();

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
            proxyHealthTask.scheduleAtFixedRate(this::testProxies, poolConfig.getProxyTestFrequency(), poolConfig.getProxyTestFrequency(), TimeUnit.MILLISECONDS);
        }

        // todo: 1 thread should be enough for this.. right?
        if (poolConfig.getConnectionLeakThreshold() > 0) {
            leakTesterService = Executors.newScheduledThreadPool(1, r -> createThread("Proxy Leak Test", r));
            leakTesterService.scheduleAtFixedRate(new ProxyLeakTester(this), 0, poolConfig.getLeakTestFrequencyMillis(), TimeUnit.MILLISECONDS);
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
     * @return the number of proxies available in the pool (that are in the pool AND alive)
     */
    public int getAvailableProxies() {
        return (int) this.proxies.values().stream().filter(ipm -> ipm.isInPool() && ipm.isAlive()).count();
    }

    /**
     * @return the number of alive proxies which may be either in the pool or outside the pool (but are all alive).
     */
    public int getActiveProxies() {
        return (int) this.proxies.values().stream().filter(InternalProxyMeta::isAlive).count();
    }

    @SneakyThrows
    private void testProxies() {
        List<Future<?>> futures = new ArrayList<>(this.proxies.size());
        for (Map.Entry<ProxySocketAddress, InternalProxyMeta> set : this.proxies.entrySet()) {
            // each proxy ping can take a while so it's best to submit each test in a new thread
            // for fastest and most efficient execution
            Future<?> future = proxyHealthTestThreadPool.submit(new ProxyHealthTester(this, set.getKey(), set.getValue()));
            futures.add(future);
        }
        // wait till all proxies are done testing before returning
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * @return a copy of the map of all registered proxies.
     */
    public Map<ProxySocketAddress, ProxyMetadata> getAllProxies() {
        return proxies.entrySet().stream().parallel().map((kv) -> new Object[]{kv.getKey(), kv.getValue().getMetadata()})
                .collect(Collectors.toConcurrentMap(o -> (ProxySocketAddress) o[0], o -> (ProxyMetadata) o[1]));
    }

    protected String toDebugString() {
        return proxies.entrySet().stream().map((entry) -> "Proxy[address=" + entry.getKey() + "," + "meta=" + entry.getValue() + "]").collect(Collectors.toList()).toString();
    }

    @Override
    public String toString() {
        return proxies.entrySet().stream().map((entry) -> "Proxy[address=" + entry.getKey() + "," + "meta=" + entry.getValue().getMetadata() + "]").collect(Collectors.toList()).toString();
    }

    public synchronized ProxyConnection getConnection() {
        return getConnection((pm) -> true, poolConfig.getDefaultConnectionWaitMillis());
    }

    public synchronized ProxyConnection getConnection(long connectionWaitMillis) {
        return getConnection((pm) -> true, connectionWaitMillis);
    }

    public synchronized ProxyConnection getConnection(Predicate<ProxyMetadata> predicate) {
        return getConnection(predicate, poolConfig.getDefaultConnectionWaitMillis());
    }

    public synchronized ProxyConnection getConnection(Predicate<ProxyMetadata> predicate, long connectionWaitMillis) {
        long start = System.currentTimeMillis();
        while (true) {
            Optional<Map.Entry<ProxySocketAddress, InternalProxyMeta>> set = proxies.entrySet().stream()
                    // filter to only proxies actively in pool AND alive
                    .filter(p -> p.getValue().isInPool() && p.getValue().isAlive())
                    .filter(p -> predicate.test(p.getValue().getMetadata()))
                    .min(getSorter()); // get the proxy with the lowest response time first
            if (set.isPresent()) {
                return getConnection(set.get().getKey(), set.get().getValue());
            }
            if (connectionWaitMillis > 0 && (System.currentTimeMillis() - start) > connectionWaitMillis) {
                throw new ProxyPoolExhaustedException("Unable to obtain a proxy connection!");
            }
        }
    }

    private Comparator<Map.Entry<ProxySocketAddress, InternalProxyMeta>> getSorter() {
        Comparator<Map.Entry<ProxySocketAddress, InternalProxyMeta>> proxySorter;
        assert poolConfig.getSortingMode() != null;
        switch (poolConfig.getSortingMode()) {
            case LATENCY:
                proxySorter = Comparator.comparingLong(entry -> entry.getValue().getLatestHealthReport().getMillisResponseTime());
                break;
            case LAST_USED:
                proxySorter = Comparator.comparingLong(entry -> entry.getValue().getTimeTaken());
                break;
            case CUSTOM:
                proxySorter = poolConfig.getCustomProxySorter();
                if (proxySorter == null) {
                    throw new IllegalStateException("The custom proxy sorter must be set when using a custom proxy sorting mode!");
                }
            default:
                throw new IllegalStateException();
        }
        return proxySorter;
    }

    public synchronized ProxyConnection getConnection(ProxySocketAddress address) {
        return getConnection(address, poolConfig.getDefaultConnectionWaitMillis());
    }

    // todo kinda dirty, combine with pred? Special way to o(1) filtering?? implement piroty? idk
    // todo: add sorting modes: get by least used proxy, best ping, last tested
    // todo: custom sorting predicates
    // todo: apache support
    // todo: add ability to treat proxy in the pool if target host is different
    //  and on that not, also add support for max concurrency PER proxy
    // todo: automatic saving of target hosts in the metadata
    public synchronized ProxyConnection getConnection(ProxySocketAddress address, long connectionWaitMillis) {
        long start = System.currentTimeMillis();
        while (true) {
            InternalProxyMeta set = proxies.get(address);
            if (set.isAlive() && set.isInPool()) {
                return getConnection(address, set);
            }
            if (connectionWaitMillis > 0 && System.currentTimeMillis() - start > connectionWaitMillis) {
                throw new ProxyPoolExhaustedException("Unable to obtain a proxy connection!");
            }
        }
    }

    //todo why no sync?
    protected ProxyConnection getConnection(ProxySocketAddress address, InternalProxyMeta meta) {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        meta.setStackBorrower(Arrays.copyOfRange(elements, 2, elements.length));
        meta.setTimeTaken(System.currentTimeMillis());
        meta.setInPool(false);
        while(poolConfig.getMaxConcurrency() > 0 && getAvailableProxies() - getActiveProxies() > poolConfig.getMaxConcurrency()) {
            // getAvailableProxies() - getActiveProxies() = proxies outside the pool
            // so we wait until the proxies outside the pool are less than max concurrency
        }
        return new ProxyConnection(this, address);
    }


}
