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

import com.ldbc.driver.DbException;
import com.ldbc.driver.OperationHandler;
import com.ldbc.driver.ResultReporter;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcNoResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate5AddForumMembership;
import com.tinkerpop.blueprints.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.directory.SchemaViolationException;
import java.util.HashMap;
import java.util.Map;

/**
 * Adds membership edge between forum and person. Assumes both exist.
 * Created by Tomer Sagi on 14-Nov-14.
 */
public class LdbcQueryU5Handler implements OperationHandler<LdbcUpdate5AddForumMembership,TitanFTMDb.BasicDbConnectionState> {
    final static Logger logger = LoggerFactory.getLogger(LdbcQueryU5Handler.class);

    @Override
    public void executeOperation(LdbcUpdate5AddForumMembership operation, TitanFTMDb.BasicDbConnectionState dbConnectionState, ResultReporter reporter) throws DbException {

        TitanFTMDb.BasicClient client = dbConnectionState.client();

        try {
            Vertex forum = client.getVertex(operation.forumId(), "Forum");
            Vertex person = client.getVertex(operation.personId(), "Person");
            if (forum==null)
                logger.error("Forum membership requested for nonexistent forum id {}", operation.forumId());
            if (person==null)
                logger.error("Forum membership requested for nonexistent person {}", operation.personId());

            Map<String, Object> props = new HashMap<>(1);
            props.put("joinDate", operation.joinDate().getTime());
            client.addEdge(forum, person, "hasMember", props);

        } catch (SchemaViolationException e) {
            logger.error("invalid vertex label requested by query update");
            e.printStackTrace();
        }

        reporter.report(0, LdbcNoResult.INSTANCE,operation);
    }
}
