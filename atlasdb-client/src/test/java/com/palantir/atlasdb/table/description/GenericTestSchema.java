/*
 * Copyright 2017 Palantir Technologies
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.atlasdb.table.description;

import java.io.File;

import com.palantir.atlasdb.keyvalue.api.Namespace;
import com.palantir.atlasdb.schema.AtlasSchema;

public class GenericTestSchema implements AtlasSchema {
    public static final AtlasSchema INSTANCE = new GenericTestSchema();

    private static final Schema GENERIC_TEST_SCHEMA = generateSchema();

    private static Schema generateSchema() {
        Schema schema = new Schema("GenericTestSchema",
                GenericTestSchema.class.getPackage().getName() + ".generated",
                Namespace.DEFAULT_NAMESPACE);

        // use for testing rangeScanAllowed code
        schema.addTableDefinition("rangeScanTest", new TableDefinition() {{
            javaTableName("RangeScanTest");

            rowName();
            rowComponent("component1", ValueType.STRING);

            columns();
            column("column1", "c", ValueType.VAR_LONG);

            rangeScanAllowed();
        }});

        schema.addTableDefinition("genericRangeScanTest", new TableDefinition() {{
            javaTableName("GenericRangeScanTest");

            rowName();
            rowComponent("component1", ValueType.SHA256HASH);

            columns();
                dynamicColumns();
                columnComponent("component2", ValueType.STRING);
                value(ValueType.STRING);

            rangeScanAllowed();
        }});

        return schema;
    }

    public static Schema getSchema() {
        return GENERIC_TEST_SCHEMA;
    }

    public static void main(String[]  args) throws Exception {
        GENERIC_TEST_SCHEMA.renderTables(new File("src/test/java"));
    }

    @Override
    public Schema getLatestSchema() {
        return GENERIC_TEST_SCHEMA;
    }

    @Override
    public Namespace getNamespace() {
        return Namespace.DEFAULT_NAMESPACE;
    }
}
