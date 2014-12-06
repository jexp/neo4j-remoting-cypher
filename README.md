Run with

    mvn test-compile exec:java -Dexec.mainClass=graphrobustness.NeoClientPerfTest -Dexec.classpathScope=test \
                               -Druns=10000 -Dstatements=100 -Durl=http://localhost:7474/

Uses a pool with `Runtime.getRuntime().availableProcessors()` threads
