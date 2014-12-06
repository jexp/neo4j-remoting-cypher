package rest;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import util.Util;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author mh
 * @since 06.12.14
 */
public class RequestResult<T> {
    private final int status;
    private final String location;
    private final T value;

    public RequestResult(CloseableHttpResponse response,Class<? extends T> type) throws IOException {
        this.status = response.getStatusLine().getStatusCode();
        Header loc = response.getLastHeader(HttpHeaders.LOCATION);
        this.location = loc != null ? loc.getValue() : null;
        try (InputStream content = response.getEntity().getContent()) {
            this.value = parse(content, type);
        }
    }

    private T parse(InputStream content, Class<? extends T> type) {
        if (type.equals(CharSequence.class))
            return (T) Util.readAsString(content);
        else
            return Util.fromJson(content, type);
    }

    public int getStatus() {
        return status;
    }

    public String getText() {
        return value.toString();
    }

    public boolean statusIs(int status) {
        return this.status == status;
    }

    public String getLocation() {
        return location;
    }

    public T getValue() {
        return value;
    }
}
