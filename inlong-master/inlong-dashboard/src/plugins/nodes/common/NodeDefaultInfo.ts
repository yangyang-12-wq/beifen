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
import { nodes, defaultValue } from '..';

const { I18nMap, I18n } = DataWithBackend;
const { FieldList, FieldDecorator } = RenderRow;
const { ColumnList, ColumnDecorator } = RenderList;

export class NodeDefaultInfo implements DataWithBackend, RenderRow, RenderList {
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
  @I18n('meta.Nodes.Name')
  displayName: string;

  @FieldDecorator({
    type: nodes.length > 3 ? 'select' : 'radio',
    initialValue: defaultValue,
    rules: [{ required: true }],
    props: values => ({
      disabled: Boolean(values.id),
      options: nodes
        .filter(item => item.value)
        .map(item => ({
          label: item.label,
          value: item.value,
        })),
    }),
  })
  @ColumnDecorator({
    render: type => nodes.find(c => c.value === type)?.label || type,
  })
  @I18n('meta.Nodes.Type')
  type: string;

  @FieldDecorator({
    type: UserSelect,
    rules: [{ required: true }],
    props: {
      mode: 'multiple',
      currentUserClosable: false,
    },
  })
  @ColumnDecorator()
  @I18n('meta.Nodes.Owners')
  inCharges: string;

  clusterTags: string;

  @FieldDecorator({
    type: 'textarea',
    props: {
      maxLength: 256,
    },
  })
  @I18n('meta.Nodes.Description')
  description?: string;

  @ColumnDecorator()
  @I18n('basic.Creator')
  readonly creator: string;

  @ColumnDecorator()
  @I18n('basic.Modifier')
  readonly modifier: string;

  readonly version?: number;

  parse(data) {
    return data;
  }

  stringify(data) {
    return data;
  }

  renderRow() {
    const constructor = this.constructor as typeof NodeDefaultInfo;
    return constructor.FieldList;
  }

  renderList() {
    const constructor = this.constructor as typeof NodeDefaultInfo;
    return constructor.ColumnList;
  }
}
