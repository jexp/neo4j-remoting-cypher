/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package graphrobustness;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static java.util.Arrays.asList;

public class NeoClient implements AutoCloseable
{
    private final CloseableHttpClient http = HttpClients.custom()
            .setConnectionManager( new BasicHttpClientConnectionManager() ).build();
    private final String commitPath;

    public NeoClient(String connStr)
    {
        this.commitPath = connStr + "db/data/transaction/commit";
    }

    public static class Row
    {
        private final JsonNode raw;
        private final Map<String, Integer> columns;

        public Row( JsonNode raw, Map<String, Integer> columns )
        {
            this.raw = raw;
            this.columns = columns;
        }

        public long getLong(String columnName)
        {
            return raw.get( columns.get(columnName) ).asLong();
        }
    }

    public static class Result implements Iterator<Row>
    {
        private final Map<String, Integer> columns = new HashMap<>();
        private final JsonNode data;
        private int index = 0;

        public Result( JsonNode root )
        {
            this.data = root.get("results").get(0).get("data");

            int index = 0;
            for ( JsonNode columnn : root.get( "results" ).get( 0 ).get( "columns" ) )
            {
                columns.put( columnn.getTextValue(), index++ );
            }
        }

        public Row get( int rowId )
        {
            return new Row(data.get(rowId).get("row"), columns);
        }

        @Override
        public boolean hasNext()
        {
            return data.size() > index;
        }

        @Override
        public Row next()
        {
            return get(index++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static class Transaction implements AutoCloseable
    {
        private static final byte[] BUFFER = new byte[1024 * 1024];
        private final CloseableHttpClient http;
        private final String commitPath;
        private final List<Map<String, Object>> statements = new ArrayList<>(1000);

        public Transaction( CloseableHttpClient http, String commitPath )
        {
            this.http = http;
            this.commitPath = commitPath;
        }

        public void execute(String command, Object params)
        {
            statements.add( Util.map("statement", command, "parameters", params) );
        }

        @Override
        public void close() throws Exception
        {
            HttpPost request = new HttpPost( commitPath );
            request.setHeader( "Connection", "Keep-Alive" );
            request.setEntity( new StringEntity( Util.createJsonFrom(Util.map(
                    "statements", statements
            )), ContentType.APPLICATION_JSON ) );
            CloseableHttpResponse response = http.execute(request);
            assert response.getStatusLine().getStatusCode() == 200;
            InputStream content = response.getEntity().getContent();
//            while (content.read(BUFFER) != -1);
//            String text = new Scanner(content).useDelimiter("\\Z").next();
//            System.out.println("text = " + text);
            content.close();
        }
    }

    public Transaction newTx()
    {
        return new Transaction(http, commitPath);
    }

    // Cheating a bit, since this is just to test robustness
    public Result query(String command, Object params) throws IOException
    {
        HttpPost request = new HttpPost( commitPath );
        request.setEntity( new StringEntity( Util.createJsonFrom(Util.map(
                "statements", asList(Util.map("statement", command, "parameters", params))
        )), ContentType.APPLICATION_JSON ) );
        try(CloseableHttpResponse res = http.execute( request ))
        {
            InputStream content = res.getEntity().getContent();
            return new Result(Util.readTree(content));
        }
    }

    @Override
    public void close() throws Exception
    {
        http.close();
    }
}

