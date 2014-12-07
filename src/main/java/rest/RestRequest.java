package rest;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import util.Util;

import java.io.IOException;
import java.util.Map;

/**
 * @author mh
 * @since 06.12.14
 */
public class RestRequest implements AutoCloseable {
    ConnectionKeepAliveStrategy keepAliveStrategy = new DefaultConnectionKeepAliveStrategy() {
        // Keep connections alive 5 seconds if a keep-alive value has not be explicitly set by the server
        @Override
        public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
            long keepAlive = super.getKeepAliveDuration(response, context);
            return keepAlive != -1 ? keepAlive : 5000;
        }
    };

    ConnectionReuseStrategy connectionReuseStrategy = new DefaultConnectionReuseStrategy() {
        @Override
        public boolean keepAlive(HttpResponse response, HttpContext context) {
            return true;
        }
    };

    private final CloseableHttpClient http = HttpClients.custom()
// didn't help in testing
//            .setKeepAliveStrategy(keepAliveStrategy)
//            .setConnectionReuseStrategy(connectionReuseStrategy)
            .setConnectionManager(new BasicHttpClientConnectionManager()).build();


    private final String uri;

    public RestRequest(String uri) {
        this.uri = uri.contains("/db/data") ? uri : uri + "db/data/";
    }

    public <T> RequestResult<T> post(String url, Map<String, Object> params, Class<? extends T> type) {
        HttpPost request = new HttpPost( resolve(url) );
        String payload = Util.createJsonFrom(params);
        request.setEntity( new StringEntity(payload, ContentType.APPLICATION_JSON ) );
        return execute(request,type);
    }

    private String resolve(String url) {
        if (url.startsWith("http")) return url;
        return uri + url; // todo resolve absolute sub-urls
    }

    public <T> RequestResult<T> put(String url, Map<String, Object> params, Class<? extends T> type) {
        HttpPut request = new HttpPut( resolve(url) );
        String payload = Util.createJsonFrom(params);
        request.setEntity( new StringEntity(payload, ContentType.APPLICATION_JSON ) );
        return execute(request,type);
    }
    public <T> RequestResult<T> get(String url, Class<? extends T> type) {
        HttpGet request = new HttpGet( resolve(url) );
        return execute(request,type);
    }

    public RequestResult delete(String url) {
        HttpDelete request = new HttpDelete( resolve(url) );
        return execute(request, String.class);
    }

    private <T> RequestResult<T> execute(HttpRequestBase request,Class<? extends T> type) {
//        System.err.println("Executing "+request.getClass().getSimpleName()+" to "+request.getURI());
        try {
            request.setHeader( "Connection", "Keep-Alive" );
            CloseableHttpResponse response = http.execute(request);
            return new RequestResult<T>(response,type);
        } catch (IOException e) {
            throw new RuntimeException("Error executing "+request.getClass().getSimpleName()+" to "+request.getURI(),e);
        }
    }

    public void close() throws IOException {
        http.close();
    }
}
