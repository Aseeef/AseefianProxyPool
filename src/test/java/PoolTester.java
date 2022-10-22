import com.github.Aseeef.AseefianProxyPool;
import com.github.Aseeef.ProxyConnection;
import com.github.Aseeef.wrappers.PoolConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class PoolTester {

    public static void main(String[] args) {

        try {
            File file = new File("Webshare 10 proxies.txt");
            PoolConfig config = new PoolConfig().setProxyTimeoutMillis(200).setDefaultConnectionWaitMillis(2000);
            AseefianProxyPool pool = new AseefianProxyPool(file, config, Proxy.Type.HTTP);
            pool.init();

            // add meta tags
            for (int i = 0 ; i < 25 ; i++) {
                ProxyConnection connection = pool.getConnection();
                connection.getMetadata().put("A", 1);
                connection.getMetadata().put("B", 2);
                if (connection.getMetadata().containsKey("A")) {
                    connection.getMetadata().put("A", connection.getMetadata().get("A"));
                }
                connection.close();
            }

            ThreadPoolExecutor es = (ThreadPoolExecutor) Executors.newCachedThreadPool();

            System.out.println("testing getting conn");
            long start = System.currentTimeMillis();
            for (int i = 0 ; i < 5 ; i++) {
                es.submit(() -> {
                    try {

                        long s = System.currentTimeMillis();
                        ProxyConnection p1 = pool.getConnection(pm -> pm.containsKey("A"));
                        System.out.println("1: " + (System.currentTimeMillis() - s));

                        s = System.currentTimeMillis();
                        p1.getHTTPConnection("https://checkip.amazonaws.com/").getInputStream().readAllBytes();
                        p1.close();
                        System.out.println("2: " + (System.currentTimeMillis() - s));

                    } catch (Exception ex) {ex.printStackTrace();}
                });
            }
            while (es.getActiveCount() != 0) {
                Thread.sleep(1000);
            }
            System.out.println("end " + (System.currentTimeMillis() - start));
            pool.getConnection().close();

            System.exit(0);
            if (true) return;

            BufferedReader br = new BufferedReader(new FileReader("users.csv"));
            ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
            while (br.ready()) {
                long l = System.currentTimeMillis();
                executorService.submit(() -> {
                    try (ProxyConnection connection = pool.getConnection()) {
                        HttpURLConnection get = (HttpURLConnection) connection.getHTTPConnection(String.format("https://sessionserver.mojang.com/session/minecraft/profile/%s", br.readLine().toLowerCase()));
                        get.setRequestMethod("GET");
                        InputStream is = get.getInputStream();
                        byte[] targetArray = new byte[is.available()];
                        is.read(targetArray);
                        System.out.println(new String(targetArray));
                        //System.out.println(connection.getHost());
                        //System.out.println(System.currentTimeMillis() - l + "ms" + pool.getAvailableProxies());
                    } catch (Exception e) {
                    }
                });
            }

            while (executorService.getActiveCount() != 0) {
                Thread.sleep(100);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("DONE");
        System.exit(0);

        /*
        System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");

        Authenticator.setDefault(new Authenticator() {
            @Override
            public PasswordAuthentication requestPasswordAuthenticationInstance(String host, InetAddress addr, int port, String protocol, String prompt, String scheme, URL url, RequestorType reqType) {
                System.out.println(host + "// " + addr + "//" + port + "//" + protocol + "//" + scheme + "//" + url);
                return super.requestPasswordAuthenticationInstance(host, addr, port, protocol, prompt, scheme, url, reqType);
            }
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                System.out.println("I RANN");
                System.out.println(this.getRequestingURL());
                System.out.println(this.getRequestingHost());
                System.out.println(this.getRequestingProtocol());
                System.out.println(this.getRequestingScheme());
                return new PasswordAuthentication("izmowoqc", "3ap8cd4xo2gq".toCharArray());
            }
        });
        URLConnection conn = new URL("https://api.myip.com").openConnection(new Proxy(Proxy.Type.DIRECT, new InetSocketAddress("138.128.59.129", 9058)));
        InputStream is = conn.getInputStream();
        byte[] targetArray = new byte[is.available()];
        is.read(targetArray);
        System.out.println(new String(targetArray));
        is.close();

         */


//        try (ProxyConnection proxy = new ApacheProxyPool(null).getConnection()) {
//            System.out.println(Authenticator.getDefault().requestPasswordAuthenticationInstance(null, null, 0, null, null, null, null, null));
//            HttpURLConnection url = (HttpURLConnection) new URL("https://api.myip.com").openConnection(proxy);
//            InputStream is = url.getInputStream();
//            byte[] targetArray = new byte[url.getContentLength()];
//            is.read(targetArray);
//            System.out.println(new String(targetArray));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

    }

}
