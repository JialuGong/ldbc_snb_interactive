/**
 (c) Copyright [2015] Hewlett-Packard Development Company, L.P.
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package hpl.alp2.titan.drivers.interactive;

import com.ldbc.driver.OperationHandler;
import com.ldbc.driver.ResultReporter;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery2PersonPosts;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery2PersonPostsResult;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.util.structures.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of LDBC Interactive workload short query 2
 * Given a start Person, retrieve the last 10 Messages (Posts or Comments) created by that person.
 * For each message, return that message, the original post in its conversation, and the author of that post.
 * Order results descending by message creation date, then descending by message identifier
 * Created by Tomer Sagi on 10-Mar-15.
 */
public class LdbcShortQuery2Handler implements OperationHandler<LdbcShortQuery2PersonPosts,TitanFTMDb.BasicDbConnectionState> {
    final static Logger logger = LoggerFactory.getLogger(LdbcShortQuery2Handler.class);

    @Override
    public void executeOperation(final LdbcShortQuery2PersonPosts operation,TitanFTMDb.BasicDbConnectionState dbConnectionState,ResultReporter resultReporter) {
        List<LdbcShortQuery2PersonPostsResult> result = new ArrayList<>();
        long person_id = operation.personId();
        TitanFTMDb.BasicClient client = dbConnectionState.client();
        final Vertex root;
        try {
            root = client.getVertex(person_id, "Person");
            logger.debug("Short Query 2 called on person id: {}", person_id);
            GremlinPipeline<Vertex, Vertex> gp = new GremlinPipeline<>(root);
            Iterable<Row> qResult = gp.in("hasCreator").as("message").select()
                    .order(QueryUtils.COMP_CDate_Postid).range(0, operation.limit() - 1);

            for (Row r : qResult) {
                Vertex message = (Vertex)r.getColumn("message");
                Vertex originalM =message;
                Vertex author;

                if (!((String)message.getProperty("label")).equalsIgnoreCase("Post")) { //Comment, retrieve original post in thread
                    GremlinPipeline<Vertex, Vertex> gpt = (new GremlinPipeline<>(message));
                    Iterator<Vertex> it = gpt.as("start").out("replyOf").loop("start", QueryUtils.LOOPTRUEFUNC, QueryUtils.LOOPTRUEFUNC)
                            .as("originalM");
                    while (it.hasNext())
                     originalM= it.next();
                }

                Iterator<Vertex> it = originalM.query().labels("hasCreator").vertices().iterator();
                if (!it.hasNext()) {
                    logger.error("No creator found for message {}",originalM.getId());
                    resultReporter.report(-1,null,operation);
                    return;
                }
                author = it.next();
                Long mid = client.getVLocalId((Long) message.getId());
                String content = message.getProperty("content");
                if (content.length() == 0)
                    content = message.getProperty("imageFile");

                LdbcShortQuery2PersonPostsResult res = new LdbcShortQuery2PersonPostsResult(
                        mid,content, (Long) message.getProperty("creationDate"),
                        client.getVLocalId((Long) originalM.getId()),
                        client.getVLocalId((Long) author.getId()),
                        (String) author.getProperty("firstName"), (String) author.getProperty("lastName"));
                result.add(res);
            }
            resultReporter.report(result.size(), result, operation);
        } catch (Exception e) {
        e.printStackTrace();
        resultReporter.report(-1, null, operation);
    }

    }


}