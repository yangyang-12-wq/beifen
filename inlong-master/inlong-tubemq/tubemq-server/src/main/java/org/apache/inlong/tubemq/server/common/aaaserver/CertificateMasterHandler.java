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

package org.apache.inlong.tubemq.server.common.aaaserver;

import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public interface CertificateMasterHandler {

    boolean identityValidBrokerInfo(
            ClientMaster.MasterCertificateInfo authenticInfo, ProcessResult result);

    boolean identityValidUserInfo(ClientMaster.MasterCertificateInfo authenticInfo,
            boolean isProduce, ProcessResult result);

    boolean validProducerAuthorizeInfo(String userName,
            Set<String> topics, String clientIp, ProcessResult result);

    boolean validConsumerAuthorizeInfo(String userName, String groupName, Set<String> topics,
            Map<String, TreeSet<String>> topicConds, String clientIp, ProcessResult result);

}
