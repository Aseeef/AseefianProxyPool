package com.github.Aseeef.http;

import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HTTPProxyRequest {

    protected URL url;
    protected HttpURLConnection urlConnection;
    @Getter private int responseCode = -1;

    public HTTPProxyRequest(HTTPProxyRequestBuilder httpProxyRequest) throws IOException {
        this.url = httpProxyRequest.url;
        this.urlConnection = (HttpURLConnection) url.openConnection(httpProxyRequest.proxyConnection);
        this.urlConnection.setRequestMethod(httpProxyRequest.requestMethod.toString());
        this.urlConnection.setConnectTimeout(httpProxyRequest.connectionTimeOutMillis);
        this.urlConnection.setReadTimeout(httpProxyRequest.connectionTimeOutMillis);
        this.urlConnection.setUseCaches(httpProxyRequest.useCaches);
        for (Map.Entry<String, String> set : httpProxyRequest.headers.entrySet()) {
            this.urlConnection.addRequestProperty(set.getKey(), set.getValue());
        }
        this.urlConnection.setInstanceFollowRedirects(httpProxyRequest.followRedirects);
        this.urlConnection.setDoOutput(httpProxyRequest.requestMethod.output);
        this.urlConnection.setDoInput(httpProxyRequest.requestMethod.input);

        if (this.urlConnection.getDoOutput()) {
            this.urlConnection.addRequestProperty("Content-Type", httpProxyRequest.contentType.type);
            this.urlConnection.getOutputStream().write(httpProxyRequest.contentBody);
        }
    }

    public HTTPProxyRequest connect() throws IOException {
        this.urlConnection.connect();
        this.responseCode = urlConnection.getResponseCode();
        return this;
    }

    public InputStream getErrorStream() {
        if (this.urlConnection.getDoInput()) {
            return this.urlConnection.getErrorStream();
        } else {
            return null;
        }
    }

    public byte[] getErrorBytes() throws IOException {
        if (this.urlConnection.getDoInput()) {
            return streamToBytes(getErrorStream());
        } else {
            return null;
        }
    }

    public String getErrorString() throws IOException {
        return getErrorString(StandardCharsets.UTF_8);
    }

    public String getErrorString(Charset charset) throws IOException {
        return new String(getResponseBytes(), charset);
    }

    public InputStream getInputStream() throws IOException {
        if (this.urlConnection.getDoInput()) {
            return this.urlConnection.getInputStream();
        } else {
            return null;
        }
    }

    public byte[] getResponseBytes() throws IOException {
        if (this.urlConnection.getDoInput()) {
            return streamToBytes(getInputStream());
        } else {
            return null;
        }
    }

    public String getContentString() throws IOException {
        return getContentString(StandardCharsets.UTF_8);
    }

    public String getContentString(Charset charset) throws IOException {
        return new String(getResponseBytes(), charset);
    }

    public static byte[] streamToBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(stream.available());
        int temp;
        while ((temp = stream.read()) != -1) {
            outputStream.write(temp);
        }
        return outputStream.toByteArray();
    }

}
