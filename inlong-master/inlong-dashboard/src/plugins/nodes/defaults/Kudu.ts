/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import { DataWithBackend } from '@/plugins/DataWithBackend';
import { RenderRow } from '@/plugins/RenderRow';
import { RenderList } from '@/plugins/RenderList';
import { NodeInfo } from '../common/NodeInfo';
import i18n from '@/i18n';

const { I18n } = DataWithBackend;
const { FieldDecorator } = RenderRow;

export default class KuduNode extends NodeInfo implements DataWithBackend, RenderRow, RenderList {
  @FieldDecorator({
    type: 'input',
    initialValue: '127.0.0.1:5071,127.0.0.1:5072',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      placeholder: '127.0.0.1:5071,127.0.0.1:5072',
    }),
  })
  @I18n('meta.Nodes.Kudu.masters')
  masters: string;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 30000,
    props: {
      min: 0,
    },
  })
  @I18n('meta.Nodes.Kudu.DefaultAdminOperationTimeoutMs')
  defaultAdminOperationTimeoutMs: number;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 30000,
    props: {
      min: 0,
    },
  })
  @I18n('meta.Nodes.Kudu.DefaultOperationTimeoutMs')
  defaultOperationTimeoutMs: number;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 10000,
    props: {
      min: 0,
    },
  })
  @I18n('meta.Nodes.Kudu.DefaultSocketReadTimeoutMs')
  defaultSocketReadTimeoutMs: number;

  @FieldDecorator({
    type: 'radio',
    initialValue: false,
    props: {
      options: [
        {
          label: i18n.t('basic.Yes'),
          value: true,
        },
        {
          label: i18n.t('basic.No'),
          value: false,
        },
      ],
    },
  })
  @I18n('meta.Nodes.Kudu.StatisticsDisabled')
  statisticsDisabled: boolean;
}
