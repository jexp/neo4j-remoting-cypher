mvn test-compile exec:java -Dexec.mainClass=transaction.CypherTransactionPerfTest -Dexec.classpathScope=test -Druns=10000 -Dstatements=100 -Durl=http://localhost:7474/
