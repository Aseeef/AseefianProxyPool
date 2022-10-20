import com.github.Aseeef.AseefianProxyPool;
import com.github.Aseeef.wrappers.PoolConfig;
import com.github.Aseeef.ProxyConnection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

public class PoolTester {

    public static void main(String[] args) {

        try {
            File file = new File("Webshare 10 proxies.txt");
            PoolConfig config = new PoolConfig().setProxyTimeoutMillis(50);
            AseefianProxyPool pool = new AseefianProxyPool(file, config, Proxy.Type.HTTP);
            pool.init();
            System.out.println(pool.getAllProxies().entrySet().stream().map(kv -> kv.getValue().getProxyHealthReport().getMillisResponseTime()).collect(Collectors.toList()));

            BufferedReader br = new BufferedReader(new FileReader("users.csv"));
            ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
            try (ProxyConnection connection = pool.getConnection()) {
                while (br.ready()) {
                    long l = System.currentTimeMillis();
                    HttpURLConnection get = (HttpURLConnection) connection.connect(String.format("https://sessionserver.mojang.com/session/minecraft/profile/%s", br.readLine().toLowerCase()));
                    get.setRequestMethod("GET");
                    executorService.submit(() -> {
                        try {
                            InputStream is = get.getInputStream();
                            byte[] targetArray = new byte[is.available()];
                            is.read(targetArray);
                            //System.out.println(new String(targetArray));
                            //System.out.println(connection.getHost());
                            //System.out.println(System.currentTimeMillis() - l + "ms" + pool.getAvailableProxies());
                        } catch (Exception e) {}
                    });
                }
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
