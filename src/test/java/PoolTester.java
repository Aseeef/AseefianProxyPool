import com.github.Aseeef.ApacheProxyPool;
import com.github.Aseeef.ProxyConnection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.HttpURLConnection;
import java.net.Proxy;

public class PoolTester {

    public static void main(String[] args) {

        try {
            File file = new File("Webshare 10 proxies.txt");
            ApacheProxyPool pool = new ApacheProxyPool(file, Proxy.Type.SOCKS);
            pool.init();

            BufferedReader br = new BufferedReader(new FileReader(new File("users.csv")));

            while (br.ready()) {
                long l = System.currentTimeMillis();
                try (ProxyConnection connection = pool.getConnection()) {
                    HttpURLConnection get = (HttpURLConnection) connection.connect(String.format("https://sessionserver.mojang.com/session/minecraft/profile/%s", br.readLine().toLowerCase()));
                    get.setRequestMethod("GET");
                    String s = new String(get.getInputStream().readAllBytes());
                    Thread.sleep(18000);
                }
                System.out.println(System.currentTimeMillis() - l + "ms" + pool.getAvailableProxies());

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

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
