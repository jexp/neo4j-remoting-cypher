package graphrobustness;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static graphrobustness.Util.createPool;
import static graphrobustness.Util.map;

/**
 * @author mh
 * @since 05.12.14
 */
public class NeoClientPerfTest {

    public static final String QUERY = "create ({name:{name},age:{age},date:{date}})";
    private static final int COUNT = 100;
    public static final int CPU = Runtime.getRuntime().availableProcessors();
    private static final int RUNS = 10000;

    public static void main(String[] args) throws Exception {
        final NeoClient client = new NeoClient("http://localhost:7474/");
        ExecutorService pool = createPool(CPU,CPU*4);
        long time = System.currentTimeMillis();
        for (int i=0;i<RUNS;i++) {
            pool.submit(new Runnable(){
                public void run() {
                    execute(client);
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.MINUTES);
        double seconds = (System.currentTimeMillis() - time) / 1000.0f;
        System.out.printf("txn %d statements/tx %d statements %d time = %.2f seconds %.2f tx/s %.2f statements / s %n",
                RUNS,COUNT,RUNS*COUNT,
                seconds, RUNS / (seconds), RUNS * COUNT / (seconds));
        client.close();
    }

    private static void execute(NeoClient client) {
        try {
            NeoClient.Transaction tx = client.newTx();
            for (int i = 0; i < COUNT; i++) {
                tx.execute(QUERY, map("name", "name" + i, "age", i, "date", 238974239479L + i));
            }
            tx.close();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
}
