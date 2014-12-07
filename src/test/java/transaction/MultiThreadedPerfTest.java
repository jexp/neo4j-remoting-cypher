package transaction;

import rest.RequestResult;
import rest.RestRequest;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static util.Util.createPool;
import static util.Util.map;

/**
 * @author mh
 * @since 05.12.14
 */
public class MultiThreadedPerfTest {

    private static final int COUNT = Integer.parseInt(System.getProperty("ops","100"));
    public static final int CPU = Runtime.getRuntime().availableProcessors();
    private static final int RUNS = Integer.parseInt(System.getProperty("runs","10000"));
    public static final String URL = System.getProperty("url","http://localhost:7474/");

    public static void main(String[] args) throws Exception {
        ExecutorService pool = createPool(CPU,CPU*4);
        final BlockingQueue<RestRequest> queue = new ArrayBlockingQueue<>(CPU);
        for (int i=0;i<CPU;i++) { queue.offer(new RestRequest(URL)); }
        long time = System.currentTimeMillis();
        for (int i=0;i<RUNS;i++) {
            pool.submit(new Runnable(){
                public void run() {
                    execute(queue);
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.MINUTES);
        double seconds = (System.currentTimeMillis() - time) / 1000.0f;
        System.out.printf("txn %d statements/tx %d statements %d time = %.2f seconds %.2f tx/s %.2f statements / s %n",
                RUNS, COUNT, RUNS * COUNT,
                seconds, RUNS / (seconds), RUNS * COUNT / (seconds));
        for (RestRequest request : queue) {
            request.close();
        }
    }

    private static void execute(BlockingQueue<RestRequest> queue) {
        RestRequest request = null;
        try {
            request = queue.take();
            execute(request,COUNT);
        } catch(Exception e) {
            System.out.println("Exception: "+e.getMessage());
            // we don't have to kill the thread throw new RuntimeException(e);
        } finally {
            if (request!=null) queue.offer(request);
        }
    }

    private static void execute(RestRequest request, int count) {
        for (int i=0;i<count;i++) {
	        request.get(URL, null);
            // System.out.println(request.get(URL, CharSequence.class).getText());
        }
    }
}
