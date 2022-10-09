import aseef.dev.ApacheProxyPool;
import aseef.dev.proxy.AseefianProxy;
import aseef.dev.ProxyConnection;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.net.*;

public class PoolTester {

    @SneakyThrows
    public static void main(String[] args) {

        try (ProxyConnection connection = new ApacheProxyPool(new AseefianProxy("138.128.59.129", 9058, Proxy.Type.HTTP,"izmowoqc", "3ap8cd4xo2gq")).init().getConnection()) {
            long s = System.currentTimeMillis();
            URLConnection conn = new URL("http://checkip.amazonaws.com").openConnection(connection);
            InputStream is = conn.getInputStream();
            byte[] targetArray = new byte[is.available()];
            is.read(targetArray);
            System.out.println(new String(targetArray));
            is.close();
            System.out.println(System.currentTimeMillis() - s);
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
