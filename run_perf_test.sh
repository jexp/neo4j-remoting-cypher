mvn test-compile exec:java -Dexec.mainClass=transaction.MultiThreadedPerfTest -Druns=100000 -Dops=10 -Durl=http://127.0.0.1:8080/ -Dexec.classpathScope=test

