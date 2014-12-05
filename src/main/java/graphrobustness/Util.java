package graphrobustness;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author mh
 * @since 05.12.14
 */
public class Util {
    public static final ObjectMapper mapper = new ObjectMapper();

    public static JsonNode readTree(InputStream content) throws IOException {
        return mapper.readTree(content);
    }

    public static String createJsonFrom(Map<String, Object> map) throws IOException {
        return mapper.writeValueAsString(map);
    }

    public static Map<String, Object> map(String key, Object value) {
        return Collections.singletonMap(key, value);
    }

    public static Map<String, Object> map(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> result = new HashMap<>(2);
        result.put(key1,value1);
        result.put(key2,value2);
        return result;
    }

    public static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new HashMap<>(2);
        for (int i = 0; i < values.length; i+=2) {
           result.put((String)values[i],values[i+1]);
        }
        return result;
    }

    public static ExecutorService createPool(int threads, int queueSize) {
        return new ThreadPoolExecutor(1, threads, 30, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(queueSize),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

}
