#!/bin/bash
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
cd "$(dirname "$0")"/../ || exit

copyfile() {
    for line in $(cat "$1/dependencys.txt"); do
        cp "./lib/$line" $1/
    done
}

# Copy the jar files in dependencys.txt to every module directory
copyfile "./inlong-agent/lib"
copyfile "./inlong-dataproxy/lib"
copyfile "./inlong-manager/lib"
copyfile "./inlong-tubemq-server/lib"
copyfile "./inlong-tubemq-manager/lib"
copyfile "./inlong-audit/lib"
rm -fr ./lib
