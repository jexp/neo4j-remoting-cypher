package transaction;

import org.junit.*;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;
import org.neo4j.harness.junit.Neo4jRule;
import rest.RestRequest;

import java.io.IOException;
import java.util.*;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.neo4j.helpers.collection.MapUtil.map;

public class CypherTransactionTest {

//    @Rule public Neo4jRule neo4jRule = new Neo4jRule().withConfig("dbms.security.authorization_enabled","false");
    private static String uri;
    private static RestRequest restRequest;

    static ServerControls server;

    @BeforeClass
    public static void startServer() {
        server  = TestServerBuilders.newInProcessBuilder().withConfig("dbms.security.authorization_enabled","false").newServer();
        uri = server.httpURI().toString();
        restRequest = new RestRequest(uri);
    }
    @AfterClass public static void stopServer() throws IOException {
        restRequest.close();
        if (server!=null) server.close();
    }

    @Test
    public void testSingleSend() throws Exception {
        CypherTransaction transaction = new CypherTransaction(uri, CypherTransaction.ResultType.row);
        CypherTransaction.Result result = transaction.send("RETURN 42", null);
        assertEquals(asList("42"), result.getColumns());
        Iterator<List<Object>> rows = result.getRows().iterator();
        assertEquals(true,rows.hasNext());
        assertEquals(Arrays.<Object>asList(42), rows.next());
        assertEquals(false,rows.hasNext());
        assertEquals("RETURN 42", result.getStatement().getStatement());
        assertEquals(Collections.<String,Object>emptyMap(), result.getStatement().getParameters());
    }

    private void assertPropertyValue(Map nodeMap, String name, Object expected) {
        assertPropertyValue(Long.parseLong(nodeMap.get("id").toString()), name, expected);
    }

    private void assertPropertyValue(Number id, String name, Object expected) {
        String propertyUri = "node/" + id + "/properties/" + name;
        Object value = restRequest.get(propertyUri, expected.getClass()).getValue();
        assertEquals(expected,value);
    }
    @Test
    public void testGraphResult() throws Exception {
        CypherTransaction transaction = new CypherTransaction(uri, CypherTransaction.ResultType.graph);
        transaction.add("CREATE (n:Person {name:'Graph'}) RETURN n", null);
        List<CypherTransaction.Result> commit = transaction.commit();
        assertEquals(1, commit.size());
        CypherTransaction.Result result = commit.get(0);
        Map nodeMap = (Map) result.getRows().iterator().next().get(0);
        assertEquals("Graph", ((Map)nodeMap.get("properties")).get("name"));
        assertEquals(asList("Person"), nodeMap.get("labels"));
        assertEquals(true, nodeMap.containsKey("id"));

        assertPropertyValue(nodeMap, "name", "Graph");
    }
    @Test
    public void testRestResult() throws Exception {
        CypherTransaction transaction = new CypherTransaction(uri, CypherTransaction.ResultType.rest);
        transaction.add("CREATE (n:Person {name:'Rest'}) RETURN n", null);
        List<CypherTransaction.Result> commit = transaction.commit();
        assertEquals(1, commit.size());
        CypherTransaction.Result result = commit.get(0);
        Map nodeMap = (Map) result.getRows().iterator().next().get(0);
        assertEquals("Rest", ((Map)nodeMap.get("data")).get("name"));
        assertEquals(true, nodeMap.containsKey("self"));

        String self = nodeMap.get("self").toString();
        Number id = Long.parseLong(self.split("/")[self.split("/").length - 1]);
        assertPropertyValue(id, "name", "Rest");
    }

    @Test
    public void testCommit() throws Exception {
        CypherTransaction transaction = new CypherTransaction(uri, CypherTransaction.ResultType.row);
        CypherTransaction.Result result = transaction.send("CREATE (n {name:'John'}) RETURN id(n)", null);
        List<CypherTransaction.Result> commit = transaction.commit();
        assertEquals(1, commit.size());
        Number id = (Number) result.getRows().iterator().next().get(0);
        assertPropertyValue(id, "name", "John");
    }

    @Test
    public void testWriteCommit() throws Exception {
        CypherTransaction transaction = new CypherTransaction(uri, CypherTransaction.ResultType.row);
        CypherTransaction.Result result = transaction.send("CREATE (n {name:'John'}) RETURN id(n) as id", null);
        Object id = result.iterator().next().get("id");
        CypherTransaction.Result result2 = transaction.send("MATCH (n) WHERE id(n) = {id} return id(n) as id", map("id",id));
        List<CypherTransaction.Result> commit = transaction.commit();
        assertEquals(1, commit.size());
        Number id2 = (Number) result.getRows().iterator().next().get(0);
        assertPropertyValue(id2, "name", "John");
    }

    @Test(expected = NoSuchElementException.class)
    public void testRollback() throws Exception {
        CypherTransaction transaction = new CypherTransaction(uri, CypherTransaction.ResultType.row);
        CypherTransaction.Result result = transaction.send("CREATE (n {name:'John'}) RETURN id(n)", null);
        transaction.rollback();
        Number id = (Number) result.getRows().iterator().next().get(0);
        assertPropertyValue(id, "name", "John");
    }
}
