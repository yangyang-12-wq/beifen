/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.cdc;

import org.apache.inlong.sort.cdc.mysql.utils.OperationUtils;
import org.apache.inlong.sort.protocol.ddl.enums.AlterType;
import org.apache.inlong.sort.protocol.ddl.enums.OperationType;
import org.apache.inlong.sort.protocol.ddl.enums.PositionType;
import org.apache.inlong.sort.protocol.ddl.expressions.AlterColumn;
import org.apache.inlong.sort.protocol.ddl.operations.AlterOperation;
import org.apache.inlong.sort.protocol.ddl.operations.CreateTableOperation;
import org.apache.inlong.sort.protocol.ddl.operations.Operation;
import org.apache.inlong.sort.protocol.ddl.operations.UnsupportedOperation;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * Test for table operations
 */
public class TestOperation {

    @Test
    public void testRenameTableOperation() {
        String sql = "rename table `tv3` to `tv4`";
        HashMap<String, Integer> sqlType = new HashMap<>();
        sqlType.put("tv3", 1);
        Operation operation = OperationUtils.generateOperation(sql, sqlType);
        assert operation != null;
        Assert.assertEquals(operation.getOperationType(), OperationType.RENAME);
    }

    @Test
    public void testRenameTableByAlter() {
        String sql = "alter table a rename to b";
        HashMap<String, Integer> sqlType = new HashMap<>();
        Operation operation = OperationUtils.generateOperation(sql, sqlType);
        assert operation != null;
        Assert.assertTrue(operation instanceof AlterOperation);
    }

    @Test
    public void testDropTableOperation() {
        String sql = "drop table `tv3`";
        HashMap<String, Integer> sqlType = new HashMap<>();
        sqlType.put("tv3", 1);
        Operation operation = OperationUtils.generateOperation(sql, sqlType);
        assert operation != null;
        Assert.assertEquals(operation.getOperationType(), OperationType.DROP);
    }

    @Test
    public void testAddColumnOperation() {
        String sql = "alter table a add column b int comment \"test\" first";
        HashMap<String, Integer> sqlType = new HashMap<>();
        sqlType.put("b", 1);
        Operation operation = OperationUtils.generateOperation(sql, sqlType);
        assert operation != null;
        Assert.assertEquals(operation.getOperationType(), OperationType.ALTER);
        AlterColumn alterColumn = ((AlterOperation) operation).getAlterColumns().get(0);
        Assert.assertEquals(alterColumn.getAlterType(), AlterType.ADD_COLUMN);
        Assert.assertEquals(alterColumn.getNewColumn().getName(), "b");
        Assert.assertEquals(alterColumn.getNewColumn().getPosition().getPositionType(), PositionType.FIRST);
        Assert.assertNull(alterColumn.getNewColumn().getPosition().getColumnName());
    }

    @Test
    public void testRenameColumnOperation() {
        String sql = "alter table a CHANGE b c int";
        HashMap<String, Integer> sqlType = new HashMap<>();
        sqlType.put("c", 1);
        Operation operation = OperationUtils.generateOperation(sql, sqlType);
        assert operation != null;
        Assert.assertEquals(operation.getOperationType(), OperationType.ALTER);
        AlterColumn alterColumn = ((AlterOperation) operation).getAlterColumns().get(0);
        Assert.assertEquals(alterColumn.getAlterType(), AlterType.CHANGE_COLUMN);
        Assert.assertEquals(alterColumn.getNewColumn().getName(), "c");
        Assert.assertEquals(alterColumn.getOldColumn().getName(), "b");
    }

    @Test
    public void testUnsupportedOperation() {
        String sql = "alter table a drop key 'b'";
        HashMap<String, Integer> sqlType = new HashMap<>();
        Operation operation = OperationUtils.generateOperation(sql, sqlType);
        assert operation != null;
        Assert.assertTrue(operation instanceof UnsupportedOperation);
        Assert.assertEquals(operation.getOperationType(), OperationType.OTHER);
    }

    @Test
    public void testCreateTableWithCharacterConstraint() {
        String sql = "create table a (b int) engine=innodb character "
                + "set=utf8 collate=utf8_bin row_format=dynamic";
        HashMap<String, Integer> sqlType = new HashMap<>();
        sqlType.put("b", 1);
        Operation operation = OperationUtils.generateOperation(sql, sqlType);
        assert operation != null;
        Assert.assertTrue(operation instanceof CreateTableOperation);
    }

    @Test
    public void alterTableChangeType() {
        String sql = "ALTER TABLE test CHANGE COLUMN name name1 "
                + "mediumtext character set utf8mb4 COLLATE=utf8 NULL";
        HashMap<String, Integer> sqlType = new HashMap<>();
        sqlType.put("name1", 1);
        Operation operation = OperationUtils.generateOperation(sql, sqlType);
        assert operation != null;
        Assert.assertTrue(operation instanceof AlterOperation);
        AlterColumn alterColumn = ((AlterOperation) operation).getAlterColumns().get(0);
        Assert.assertEquals(alterColumn.getAlterType(), AlterType.CHANGE_COLUMN);
        Assert.assertEquals(alterColumn.getNewColumn().getName(), "name1");
        Assert.assertEquals(alterColumn.getOldColumn().getName(), "name");
    }

    @Test
    public void dropTableConstraint() {
        String sql = "ALTER TABLE test drop constraint a";
        HashMap<String, Integer> sqlType = new HashMap<>();
        sqlType.put("name1", 1);
        Operation operation = OperationUtils.generateOperation(sql, sqlType);
        assert operation != null;
        Assert.assertTrue(operation instanceof AlterOperation);
        Assert.assertEquals(operation.getOperationType(), OperationType.ALTER);
        Assert.assertEquals(((AlterOperation) operation).getAlterColumns().get(0).getAlterType(),
                AlterType.DROP_CONSTRAINT);
    }

    @Test
    public void addConstraint() {
        String sql = "ALTER TABLE test add constraint primary key (a)";
        HashMap<String, Integer> sqlType = new HashMap<>();
        sqlType.put("name1", 1);
        Operation operation = OperationUtils.generateOperation(sql, sqlType);
        assert operation != null;
        Assert.assertTrue(operation instanceof AlterOperation);
        Assert.assertEquals(operation.getOperationType(), OperationType.ALTER);
        Assert.assertEquals(((AlterOperation) operation).getAlterColumns().get(0).getAlterType(),
                AlterType.ADD_CONSTRAINT);
    }

}
