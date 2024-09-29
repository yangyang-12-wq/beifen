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

package org.apache.inlong.manager.pojo.sink.mysql;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;

/**
 * Test for {@link MySQLSinkDTO}
 */
public class MySQLSinkDTOTest {

    @Test
    public void testFilterSensitive() throws Exception {
        // the sensitive params no use url code
        String originUrl = MySQLSinkDTO.filterSensitive(
                "jdbc:mysql://127.0.0.1:3306?autoDeserialize=TRue&allowLoadLocalInfile = TRue&allowUrlInLocalInfile=TRue&allowLoadLocalInfileInPath=/&autoReconnect=true");
        Assertions.assertEquals(
                "jdbc:mysql://127.0.0.1:3306?autoReconnect=true&autoDeserialize=false&allowUrlInLocalInfile=false&allowLoadLocalInfile=false",
                originUrl);

        originUrl = MySQLSinkDTO.filterSensitive(
                "jdbc:mysql://127.0.0.1:3306?autoReconnect=true&autoDeserialize = TRue&allowLoadLocalInfile=TRue&allowUrlInLocalInfile=TRue&allowLoadLocalInfileInPath=/");
        Assertions.assertEquals(
                "jdbc:mysql://127.0.0.1:3306?autoReconnect=true&autoDeserialize=false&allowUrlInLocalInfile=false&allowLoadLocalInfile=false",
                originUrl);

        originUrl = MySQLSinkDTO.filterSensitive(
                "jdbc:mysql://127.0.0.1:3306?autoDeserialize=TRue&allowLoadLocalInfile = TRue&autoReconnect=true&allowUrlInLocalInfile=TRue&allowLoadLocalInfileInPath=/");
        Assertions.assertEquals(
                "jdbc:mysql://127.0.0.1:3306?autoReconnect=true&autoDeserialize=false&allowUrlInLocalInfile=false&allowLoadLocalInfile=false",
                originUrl);
        originUrl = MySQLSinkDTO.filterSensitive(
                "jdbc:mysql://127.0.0.1:3306?autoDeserialize=Yes&allowLoadLocalInfile = Yes&autoReconnect=true&allowUrlInLocalInfile=YEs&allowLoadLocalInfileInPath=/");
        Assertions.assertEquals(
                "jdbc:mysql://127.0.0.1:3306?autoReconnect=true&autoDeserialize=false&allowUrlInLocalInfile=false&allowLoadLocalInfile=false",
                originUrl);

        // the sensitive params use url code
        originUrl = MySQLSinkDTO.filterSensitive(
                URLEncoder.encode(
                        "jdbc:mysql://127.0.0.1:3306?autoDeserialize=TRue&allowLoadLocalInfile = TRue&allowUrlInLocalInfile=TRue&allowLoadLocalInfileInPath=/&autoReconnect=true",
                        "UTF-8"));
        Assertions.assertEquals(
                "jdbc:mysql://127.0.0.1:3306?autoReconnect=true&autoDeserialize=false&allowUrlInLocalInfile=false&allowLoadLocalInfile=false",
                originUrl);

        originUrl = MySQLSinkDTO.filterSensitive(
                URLEncoder.encode(
                        "jdbc:mysql://127.0.0.1:3306?autoReconnect=true&autoDeserialize = TRue&allowLoadLocalInfile=TRue&allowUrlInLocalInfile=TRue&allowLoadLocalInfileInPath=/",
                        "UTF-8"));
        Assertions.assertEquals(
                "jdbc:mysql://127.0.0.1:3306?autoReconnect=true&autoDeserialize=false&allowUrlInLocalInfile=false&allowLoadLocalInfile=false",
                originUrl);

        originUrl = MySQLSinkDTO.filterSensitive(
                URLEncoder.encode(
                        "jdbc:mysql://127.0.0.1:3306?autoDeserialize=TRue&allowLoadLocalInfile = TRue&autoReconnect=true&allowUrlInLocalInfile=TRue&allowLoadLocalInfileInPath=/",
                        "UTF-8"));
        Assertions.assertEquals(
                "jdbc:mysql://127.0.0.1:3306?autoReconnect=true&autoDeserialize=false&allowUrlInLocalInfile=false&allowLoadLocalInfile=false",
                originUrl);

        originUrl = MySQLSinkDTO.filterSensitive(
                URLEncoder.encode(
                        "jdbc:mysql://127.0.0.1:3306?autoDeserialize=Yes&allowLoadLocalInfile = yes&autoReconnect=true&allowUrlInLocalInfile=YES&allowLoadLocalInfileInPath=/",
                        "UTF-8"));
        Assertions.assertEquals(
                "jdbc:mysql://127.0.0.1:3306?autoReconnect=true&autoDeserialize=false&allowUrlInLocalInfile=false&allowLoadLocalInfile=false",
                originUrl);

        originUrl = MySQLSinkDTO.filterSensitive(
                "jdbc:mysql://127.0.0.1:3306?autoDeserialize=%59%65%73&allowLoadLocalInfile = yes&allowUrlInLocalInfil%65+=%74%72%75%45&allowLoadLocalInfileInPath=%2F");
        Assertions.assertEquals(
                "jdbc:mysql://127.0.0.1:3306?autoDeserialize=false&allowUrlInLocalInfile=false&allowLoadLocalInfile=false",
                originUrl);

        originUrl = MySQLSinkDTO.filterSensitive(
                "jdbc:mysql://127.0.0.1:3306?autoDeserialize\t\t=%59%65%73&allowLoa\r\rdLocalInfile = yes&allowUrlInLocalInfil%65+\t=%74%72%75%45&allowLoadLocalInfileInPath\n=%2F");
        Assertions.assertEquals(
                "jdbc:mysql://127.0.0.1:3306?autoDeserialize=false&allowUrlInLocalInfile=false&allowLoadLocalInfile=false",
                originUrl);
    }

    @Test
    public void testSetDbNameToUrl() {
        String originUrl = MySQLSinkDTO.setDbNameToUrl(
                "jdbc:mysql://127.0.0.1:3306?autoDeserialize=TRue&allowLoadLocalInfile=TRue&autoReconnect=true&allowUrlInLocalInfile=TRue&allowLoadLocalInfileInPath=/",
                "test_db");
        Assertions.assertEquals(
                "jdbc:mysql://127.0.0.1:3306/test_db?autoDeserialize=TRue&allowLoadLocalInfile=TRue&autoReconnect=true&allowUrlInLocalInfile=TRue&allowLoadLocalInfileInPath=/",
                originUrl);
        originUrl = MySQLSinkDTO.setDbNameToUrl("jdbc:mysql://127.0.0.1:3306", "test_db");
        Assertions.assertEquals("jdbc:mysql://127.0.0.1:3306/test_db", originUrl);
        originUrl = MySQLSinkDTO.setDbNameToUrl("jdbc:mysql://127.0.0.1:3306/", "test_db");
        Assertions.assertEquals("jdbc:mysql://127.0.0.1:3306/test_db", originUrl);
    }

}
