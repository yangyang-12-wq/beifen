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
import UserSelect from '@/ui/components/UserSelect';
import { clusters, defaultValue } from '..';

const { I18nMap, I18n } = DataWithBackend;
const { FieldList, FieldDecorator } = RenderRow;
const { ColumnList, ColumnDecorator } = RenderList;

export class ClusterDefaultInfo implements DataWithBackend, RenderRow, RenderList {
  static I18nMap = I18nMap;
  static FieldList = FieldList;
  static ColumnList = ColumnList;

  readonly id: number;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      maxLength: 128,
    }),
  })
  @ColumnDecorator()
  @I18n('pages.Clusters.Name')
  displayName: string;

  @FieldDecorator({
    type: clusters.length > 3 ? 'select' : 'radio',
    initialValue: defaultValue,
    rules: [{ required: true }],
    props: values => ({
      disabled: Boolean(values.id),
      options: clusters
        .filter(item => item.value !== 'DATAPROXY' && item.value !== '')
        .map(item => ({
          label: item.label,
          value: item.value,
        })),
    }),
  })
  @ColumnDecorator({
    render: type => clusters.find(c => c.value === type)?.label || type,
  })
  @I18n('pages.Clusters.Type')
  type: string;

  @FieldDecorator({
    type: 'select',
    rules: [{ required: true }],
    props: {
      mode: 'multiple',
      filterOption: false,
      options: {
        requestTrigger: ['onOpen', 'onSearch'],
        requestService: keyword => ({
          url: '/cluster/tag/list',
          method: 'POST',
          data: {
            keyword,
            pageNum: 1,
            pageSize: 20,
          },
        }),
        requestParams: {
          formatResult: result =>
            result?.list?.map(item => ({
              ...item,
              label: item.clusterTag,
              value: item.clusterTag,
            })),
        },
      },
    },
  })
  @ColumnDecorator()
  @I18n('pages.Clusters.Tag')
  clusterTags: string;

  @FieldDecorator({
    type: UserSelect,
    rules: [{ required: true }],
    props: {
      mode: 'multiple',
      currentUserClosable: false,
    },
  })
  @ColumnDecorator()
  @I18n('pages.Clusters.InCharges')
  inCharges: string;

  @FieldDecorator({
    type: 'textarea',
    props: {
      maxLength: 256,
    },
  })
  @I18n('pages.Clusters.Description')
  description: string;

  @ColumnDecorator()
  @I18n('basic.Creator')
  readonly creator: string;

  version?: number;

  parse(data) {
    return data;
  }

  stringify(data) {
    return data;
  }

  renderRow() {
    const constructor = this.constructor as typeof ClusterDefaultInfo;
    return constructor.FieldList;
  }

  renderList() {
    const constructor = this.constructor as typeof ClusterDefaultInfo;
    return constructor.ColumnList;
  }
}
