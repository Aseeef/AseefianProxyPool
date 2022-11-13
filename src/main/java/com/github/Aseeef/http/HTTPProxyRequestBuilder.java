package com.github.Aseeef.http;

import com.github.Aseeef.ProxyConnection;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class HTTPProxyRequestBuilder {

    protected final URL url;
    protected RequestMethod requestMethod = RequestMethod.GET;
    protected HashMap<String, String> headers = new HashMap<>();
    {
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("User-Agent", "Java/AseefianProxyPool");
    }
    protected byte[] contentBody;
    protected ContentType contentType = ContentType.TEXT_PLAIN;
    protected String customContentType;
    protected ProxyConnection proxyConnection = null;
    protected int connectionTimeOutMillis = 10000;
    protected boolean followRedirects = true;
    protected boolean useCaches = false;

    public HTTPProxyRequestBuilder(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    public HTTPProxyRequestBuilder(URL url) {
        this.url = url;
    }

    public HTTPProxyRequest build() throws IOException {
        return new HTTPProxyRequest(this);
    }

    public HTTPProxyRequestBuilder setProxyConnection(ProxyConnection proxyConnection) {
        this.proxyConnection = proxyConnection;
        return this;
    }

    public HTTPProxyRequestBuilder setHTTPMethod(RequestMethod method) {
        this.requestMethod = method;
        return this;
    }

    public HTTPProxyRequestBuilder setCustomContentType(String customContentType) {
        this.customContentType = customContentType;
        return this;
    }

    public HTTPProxyRequestBuilder setContentBody(String contentBody) {
        return setContentBody(contentBody, StandardCharsets.UTF_8);
    }

    public HTTPProxyRequestBuilder setContentBody(String contentBody, Charset charset) {
        this.contentBody = contentBody.getBytes(charset);
        return this;
    }

    public HTTPProxyRequestBuilder setContentBody(byte[] bytes) {
        this.contentBody = bytes;
        return this;
    }

    public HTTPProxyRequestBuilder setConnectionTimeoutMillis(int millis) {
        this.connectionTimeOutMillis = millis;
        return this;
    }

    public HTTPProxyRequestBuilder setFollowRedirects(boolean follow) {
        this.followRedirects = follow;
        return this;
    }

    public HTTPProxyRequestBuilder setUseCaches(boolean useCaches) {
        this.useCaches = useCaches;
        return this;
    }

    public static HTTPProxyRequestBuilder builder(URL requestUrl) {
        return new HTTPProxyRequestBuilder(requestUrl);
    }

    public static HTTPProxyRequestBuilder builder(String requestUrl) throws MalformedURLException {
        return new HTTPProxyRequestBuilder(requestUrl);
    }

    enum ContentType {
        CUSTOM(null),

        APPLICATION_EDI_X12("application/EDI-X12"),
        APPLICATION_EDIFACT("application/EDIFACT"),
        APPLICATION_JAVASCRIPT("application/javascript"),
        APPLICATION_OCTET_STREAM("application/octet-stream"),
        APPLICATION_OGG("application/ogg"),
        APPLICATION_PDF("application/pdf"),
        APPLICATION_XHTML_XML("application/xhtml+xml"),
        APPLICATION_X_SHOCKWAVE_FLASH("application/x-shockwave-flash"),
        APPLICATION_JSON("application/json"),
        APPLICATION_LD_JSON("application/ld+json"),
        APPLICATION_XML("application/xml"),
        APPLICATION_ZIP("application/zip"),
        APPLICATION_X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded"),

        AUDIO_MPEG("audio/mpeg"),
        AUDIO_X_MS_WMA("audio/x-ms-wma"),
        VND_RN_REALAUDIO("audio/vnd.rn-realaudio"),
        AUDIO_X_WAV("audio/x-wav"),

        IMAGE_GIF("image/gif"),
        IMAGE_JPEG("image/jpeg"),
        IMAGE_PNG("image/png"),
        IMAGE_TIFF("image/tiff"),
        IMAGE_VND_MICROSOFT_ICON("image/vnd.microsoft.icon"),
        IMAGE_X_ICON("image/x-icon"),
        IMAGE_VND_DJVU("image/vnd.djvu"),
        IMAGE_SVG_XML("image/svg+xml"),

        MULTIPART_MIXED("multipart/mixed"),
        MULTIPART_ALTERNATIVE("multipart/alternative"),
        MULTIPART_RELATED_MHTML("multipart/related"),
        MULTIPART_FORM_DATA("multipart/form-data"),

        TEXT_CSS("text/css"),
        TEXT_CVS("text/csv"),
        TEXT_HTML("text/html"),
        TEXT_PLAIN("text/plain"),
        TEXT_XML("text/xml"),

        VIDEO_MPEG("video/mpeg"),
        VIDEO_MP4("video/mp4"),
        VIDEO_QUICKTIME("video/quicktime"),
        VIDEO_X_MS_WMV("video/x-ms-wmv"),
        VIDEO_X_MSVIDEO("video/x-msvideo"),
        VIDEO_X_FLV("video/x-flv"),
        VIDEO_WEBM("video/webm"),
        ;
        final String type;
        ContentType(String type) {
            this.type = type;
        }
    }

    enum RequestMethod {
        POST(true, true),
        GET(true, false),
        DELETE(true, true),
        PUT(true, true),
        PATCH(true, true),
        ;
        boolean input;
        boolean output;
        RequestMethod(boolean input, boolean output) {
            this.input = input;
            this.output = output;
        }
    }

}
