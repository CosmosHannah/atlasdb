/*
 * Copyright 2016 Palantir Technologies
 * <p>
 * Licensed under the BSD-3 License (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://opensource.org/licenses/BSD-3-Clause
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.palantir.cassandra.multinode;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.google.common.collect.ImmutableMap;
import com.jayway.awaitility.Awaitility;
import com.palantir.atlasdb.AtlasDbConstants;
import com.palantir.atlasdb.cassandra.CassandraKeyValueServiceConfig;
import com.palantir.atlasdb.cassandra.CassandraKeyValueServiceConfigManager;
import com.palantir.atlasdb.cassandra.ImmutableCassandraKeyValueServiceConfig;
import com.palantir.atlasdb.containers.Containers;
import com.palantir.atlasdb.containers.ThreeNodeCassandraCluster;
import com.palantir.atlasdb.encoding.PtBytes;
import com.palantir.atlasdb.keyvalue.api.Cell;
import com.palantir.atlasdb.keyvalue.api.TableReference;
import com.palantir.atlasdb.keyvalue.api.Value;
import com.palantir.atlasdb.keyvalue.cassandra.CassandraClientPool;
import com.palantir.atlasdb.keyvalue.cassandra.CassandraKeyValueService;
import com.palantir.docker.compose.connection.Container;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        OneNodeDownGetTest.class,
        OneNodeDownPutTest.class,
        OneNodeDownMetadataTest.class,
        OneNodeDownDeleteTest.class,
        OneNodeDownTableManipulationTest.class
        })
public final class OneNodeDownTestSuite {

    private static final String CASSANDRA_NODE_TO_KILL = ThreeNodeCassandraCluster.FIRST_CASSANDRA_CONTAINER_NAME;

    static final TableReference TEST_TABLE = TableReference.createWithEmptyNamespace("test_table");
    static final TableReference TEST_TABLE_TO_DROP = TableReference.createWithEmptyNamespace("test_table_to_drop");
    static final TableReference TEST_TABLE_TO_DROP_2 = TableReference.createWithEmptyNamespace("test_table_to_drop_2");

    static final byte[] FIRST_ROW = PtBytes.toBytes("row1");
    static final byte[] SECOND_ROW = PtBytes.toBytes("row2");
    static final byte[] FIRST_COLUMN = PtBytes.toBytes("col1");
    static final byte[] SECOND_COLUMN = PtBytes.toBytes("col2");
    static final Cell CELL_1_1 = Cell.create(FIRST_ROW, FIRST_COLUMN);
    static final Cell CELL_1_2 = Cell.create(FIRST_ROW, SECOND_COLUMN);
    static final Cell CELL_2_1 = Cell.create(SECOND_ROW, FIRST_COLUMN);
    static final Cell CELL_2_2 = Cell.create(SECOND_ROW, SECOND_COLUMN);
    static final Cell CELL_3_1 = Cell.create(PtBytes.toBytes("row3"), FIRST_COLUMN);
    static final Cell CELL_4_1 = Cell.create(PtBytes.toBytes("row4"), FIRST_COLUMN);

    static final byte[] DEFAULT_CONTENTS = PtBytes.toBytes("default_value");
    static final long DEFAULT_TIMESTAMP = 2L;
    static final long OLD_TIMESTAMP = 1L;
    static final Value DEFAULT_VALUE = Value.create(DEFAULT_CONTENTS, DEFAULT_TIMESTAMP);

    static CassandraKeyValueService db;

    private OneNodeDownTestSuite(){}

    @ClassRule
    public static final Containers CONTAINERS = new Containers(OneNodeDownTestSuite.class)
            .with(new ThreeNodeCassandraCluster());

    @BeforeClass
    public static void initializeKvsAndDegradeCluster() throws IOException, InterruptedException {
        setupTestTable();
        degradeCassandraCluster();
        db = createCassandraKvs();
    }

    @AfterClass
    public static void closeKvs() throws IOException, InterruptedException {
        db.close();
    }

    private static void setupTestTable() {
        CassandraKeyValueService setupDb = createCassandraKvs();
        setupDb.createTable(TEST_TABLE, AtlasDbConstants.GENERIC_TABLE_METADATA);
        setupDb.put(TEST_TABLE, ImmutableMap.of(CELL_1_1, PtBytes.toBytes("old_value")), OLD_TIMESTAMP);
        setupDb.put(TEST_TABLE, ImmutableMap.of(CELL_1_1, DEFAULT_CONTENTS), DEFAULT_TIMESTAMP);
        setupDb.put(TEST_TABLE, ImmutableMap.of(CELL_1_2, DEFAULT_CONTENTS), DEFAULT_TIMESTAMP);
        setupDb.put(TEST_TABLE, ImmutableMap.of(CELL_2_1, DEFAULT_CONTENTS), DEFAULT_TIMESTAMP);

        setupDb.createTable(TEST_TABLE_TO_DROP, AtlasDbConstants.EMPTY_TABLE_METADATA);
        setupDb.createTable(TEST_TABLE_TO_DROP_2, AtlasDbConstants.EMPTY_TABLE_METADATA);

        setupDb.close();
    }

    protected static CassandraKeyValueService createCassandraKvs() {
        CassandraKeyValueServiceConfig config = ImmutableCassandraKeyValueServiceConfig
                .copyOf(ThreeNodeCassandraCluster.KVS_CONFIG)
                .withSchemaMutationTimeoutMillis(3_000);
        return CassandraKeyValueService.create(
                CassandraKeyValueServiceConfigManager.createSimpleManager(config),
                ThreeNodeCassandraCluster.LEADER_CONFIG);
    }

    private static void degradeCassandraCluster() throws IOException, InterruptedException {
        killFirstCassandraNode();

        // startup checks aren't guaranteed to pass immediately after killing the node, so we wait until
        // they do. unclear if this is an AtlasDB product problem. see #1154
        waitUntilStartupChecksPass();
    }

    private static void killFirstCassandraNode() throws IOException, InterruptedException {
        Container container = CONTAINERS.getContainer(CASSANDRA_NODE_TO_KILL);
        container.kill();
    }

    private static void waitUntilStartupChecksPass() {
        Awaitility.await()
                .atMost(60, TimeUnit.SECONDS)
                .until(OneNodeDownTestSuite::startupChecksPass);
    }

    private static boolean startupChecksPass() {
        CassandraKeyValueServiceConfigManager manager = CassandraKeyValueServiceConfigManager.createSimpleManager(
                ThreeNodeCassandraCluster.KVS_CONFIG);
        try {
            // startup checks are done implicitly in the constructor
            new CassandraClientPool(manager.getConfig());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    protected static void verifyValue(Cell cell, Value value) {
        Map<Cell, Value> result = db.get(TEST_TABLE, ImmutableMap.of(cell, Long.MAX_VALUE));
        assertThat(value).isEqualTo(result.get(cell));
    }

    protected static boolean tableExists(TableReference tableReference) {
        Iterator<TableReference> it = OneNodeDownTestSuite.db.getAllTableNames().iterator();
        while (it.hasNext()) {
            if (it.next().equals(tableReference)) {
                return true;
            }
        }
        return false;
    }
}
