package graphrobustness;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static graphrobustness.Util.createPool;
import static graphrobustness.Util.map;

/**
 * @author mh
 * @since 05.12.14
 */
public class NeoClientPerfTest {

    public static final String QUERY = "create ({name:{name},age:{age},date:{date}})";
    private static final int COUNT = Integer.parseInt(System.getProperty("statements","100"));
    public static final int CPU = Runtime.getRuntime().availableProcessors();
    private static final int RUNS = Integer.parseInt(System.getProperty("runs","10000"));
    public static final String URL = System.getProperty("url","http://localhost:7474/");

    public static void main(String[] args) throws Exception {
        ExecutorService pool = createPool(CPU,CPU*4);
        final ArrayBlockingQueue<NeoClient> queue = new ArrayBlockingQueue<>(CPU);
        for (int i=0;i<CPU;i++) { queue.offer(new NeoClient(URL)); }
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
        for (NeoClient neoClient : queue) {
            neoClient.close();
        }
    }

    private static void execute(BlockingQueue<NeoClient> queue) {
        NeoClient client = null;
        try {
            client = queue.take();
            NeoClient.Transaction tx = client.newTx();
            for (int i = 0; i < COUNT; i++) {
                tx.execute(QUERY, map("name", "name" + i, "age", i, "date", 238974239479L + i));
            }
            tx.close();
        } catch(Exception e) {
            System.out.println("Exception: "+e.getMessage());
            // we don't have to kill the thread throw new RuntimeException(e);
        } finally {
            if (client!=null) queue.offer(client);
        }
    }
}
