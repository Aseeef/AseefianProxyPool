import com.github.Aseeef.ApacheProxyPool;
import com.github.Aseeef.ProxyConnection;
import lombok.SneakyThrows;

import java.io.File;
import java.io.InputStream;
import java.net.*;

public class PoolTester {

    @SneakyThrows
    public static void main(String[] args) {

        File file = new File("Webshare 10 proxies.txt");
        ApacheProxyPool pool = new ApacheProxyPool(file, Proxy.Type.SOCKS);
        pool.init();

        try (ProxyConnection connection = pool.getConnection()) {
            Thread.sleep(18000);
            System.out.println("HOST:" + connection.getHost());
            System.out.println(pool.getAvailableProxies());
            System.out.println(pool);
            System.out.println(System.currentTimeMillis() - connection.getLastInspected());
            long s = System.currentTimeMillis();
            String[] uids = {"e822f8ad901f4d9ab277659a513c8dbc", "137a50b09ac84c5bb0e8d7b42b7b3b67", "8d2868b9d3fa4fa9b103530783f030fe", "92209d2b2c36493fa590b01cccd7d6b2"};
            for (String uid : uids) {
                URLConnection conn = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uid).openConnection(connection);
                InputStream is = conn.getInputStream();
                byte[] targetArray = new byte[is.available()];
                is.read(targetArray);
                System.out.println(new String(targetArray));
                is.close();
            }
            System.out.println(System.currentTimeMillis() - s + "ms");
            System.out.println("HOST:" + connection.getHost());
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
