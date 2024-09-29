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
import i18n from '@/i18n';
import EditableTable from '@/ui/components/EditableTable';
import { sourceFields } from '../common/sourceFields';
import { SinkInfo } from '../common/SinkInfo';
import NodeSelect from '@/ui/components/NodeSelect';
import CreateTable from '@/ui/components/CreateTable';

const { I18n } = DataWithBackend;
const { FieldDecorator, SyncField, SyncCreateTableField, IngestionField } = RenderRow;
const { ColumnDecorator } = RenderList;

const hudiFieldTypes = [
  'int',
  'long',
  'string',
  'float',
  'double',
  'date',
  'timestamp',
  'time',
  'boolean',
  'decimal',
  'timestamptz',
  'binary',
  'fixed',
  'uuid',
].map(item => ({
  label: item,
  value: item,
}));

const matchPartitionStrategies = fieldType => {
  const data = [
    {
      label: 'None',
      value: 'None',
      disabled: false,
    },
    {
      label: 'Identity',
      value: 'Identity',
      disabled: false,
    },
    {
      label: 'Year',
      value: 'Year',
      disabled: !['timestamp', 'date'].includes(fieldType),
    },
    {
      label: 'Month',
      value: 'Month',
      disabled: !['timestamp', 'date'].includes(fieldType),
    },
    {
      label: 'Day',
      value: 'Day',
      disabled: !['timestamp', 'date'].includes(fieldType),
    },
    {
      label: 'Hour',
      value: 'Hour',
      disabled: fieldType !== 'timestamp',
    },
    {
      label: 'Bucket',
      value: 'Bucket',
      disabled: ![
        'string',
        'boolean',
        'short',
        'int',
        'long',
        'float',
        'double',
        'decimal',
      ].includes(fieldType),
    },
    {
      label: 'Truncate',
      value: 'Truncate',
      disabled: !['string', 'int', 'long', 'binary', 'decimal'].includes(fieldType),
    },
  ];

  return data.filter(item => !item.disabled);
};

export default class HudiSink extends SinkInfo implements DataWithBackend, RenderRow, RenderList {
  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @SyncField()
  @IngestionField()
  @I18n('meta.Sinks.Hudi.DbName')
  dbName: string;

  @FieldDecorator({
    type: CreateTable,
    rules: [{ required: true }],
    props: values => ({
      disabled: [110].includes(values?.status),
      sinkType: values.sinkType,
      inlongGroupId: values.inlongGroupId,
      inlongStreamId: values.inlongStreamId,
      fieldName: 'tableName',
      sinkObj: {
        ...values,
      },
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.Hudi.TableName')
  @SyncField()
  @IngestionField()
  tableName: string;

  @FieldDecorator({
    type: 'radio',
    rules: [{ required: true }],
    initialValue: 1,
    tooltip: i18n.t('meta.Sinks.EnableCreateResourceHelp'),
    props: values => ({
      disabled: [110].includes(values?.status),
      options: [
        {
          label: i18n.t('basic.Yes'),
          value: 1,
        },
        {
          label: i18n.t('basic.No'),
          value: 0,
        },
      ],
    }),
  })
  @IngestionField()
  @I18n('meta.Sinks.EnableCreateResource')
  enableCreateResource: number;

  @FieldDecorator({
    type: NodeSelect,
    rules: [{ required: true }],
    props: values => ({
      disabled: [110].includes(values?.status),
      nodeType: 'HUDI',
    }),
  })
  @I18n('meta.Sinks.DataNodeName')
  @SyncField()
  @IngestionField()
  dataNodeName: string;

  @FieldDecorator({
    type: 'select',
    rules: [{ required: true }],
    initialValue: 'Parquet',
    props: values => ({
      disabled: [110].includes(values?.status),
      options: [
        {
          label: 'Parquet',
          value: 'Parquet',
        },
        // {
        //   label: 'Orc',
        //   value: 'Orc',
        // },
        // {
        //   label: 'Avro',
        //   value: 'Avro',
        // },
      ],
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.Hudi.FileFormat')
  @SyncField()
  @IngestionField()
  fileFormat: string;

  @FieldDecorator({
    type: EditableTable,
    rules: [{ required: false }],
    initialValue: [],
    tooltip: i18n.t('meta.Sinks.Hudi.ExtListHelper'),
    props: values => ({
      size: 'small',
      columns: [
        {
          title: 'Key',
          dataIndex: 'keyName',
          props: {
            disabled: [110].includes(values?.status),
          },
        },
        {
          title: 'Value',
          dataIndex: 'keyValue',
          props: {
            disabled: [110].includes(values?.status),
          },
        },
      ],
    }),
  })
  @ColumnDecorator()
  @SyncField()
  @IngestionField()
  @I18n('meta.Sinks.Hudi.ExtList')
  extList: string;

  @FieldDecorator({
    type: 'select',
    rules: [{ required: true }],
    initialValue: 'EXACTLY_ONCE',
    isPro: true,
    props: values => ({
      disabled: [110].includes(values?.status),
      options: [
        {
          label: 'EXACTLY_ONCE',
          value: 'EXACTLY_ONCE',
        },
        {
          label: 'AT_LEAST_ONCE',
          value: 'AT_LEAST_ONCE',
        },
      ],
    }),
  })
  @ColumnDecorator()
  @SyncField()
  @IngestionField()
  @I18n('meta.Sinks.Hudi.DataConsistency')
  dataConsistency: string;

  @FieldDecorator({
    type: EditableTable,
    props: values => ({
      size: 'small',
      canBatchAdd: true,
      upsetByFieldKey: true,
      editing: ![110].includes(values?.status),
      columns: getFieldListColumns(values),
    }),
  })
  @IngestionField()
  sinkFieldList: Record<string, unknown>[];

  @FieldDecorator({
    type: EditableTable,
    initialValue: [],
    props: values => ({
      size: 'small',
      editing: ![110].includes(values?.status),
      columns: getFieldListColumns(values).filter(
        item => item.dataIndex !== 'sourceFieldName' && item.dataIndex !== 'sourceFieldType',
      ),
      canBatchAdd: true,
      upsertByFieldKey: true,
    }),
  })
  @SyncCreateTableField()
  createTableField: Record<string, unknown>[];

  @FieldDecorator({
    type: 'input',
    tooltip: i18n.t('meta.Sinks.Hudi.PrimaryKeyHelper'),
    props: values => ({
      disabled: [110].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @SyncField()
  @IngestionField()
  @I18n('meta.Sinks.Hudi.PrimaryKey')
  primaryKey: string;

  @FieldDecorator({
    type: 'input',
    tooltip: i18n.t('meta.Sinks.Hudi.PartitionKeyHelper'),
    rules: [{ required: false }],
    props: values => ({
      disabled: [110].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @SyncField()
  @IngestionField()
  @I18n('meta.Sinks.Hudi.PartitionKey')
  partitionKey: string;
}

const getFieldListColumns = sinkValues => {
  return [
    ...sourceFields,
    {
      title: i18n.t('meta.Sinks.SinkFieldName'),
      width: 110,
      dataIndex: 'fieldName',
      rules: [
        { required: true },
        {
          pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/,
          message: i18n.t('meta.Sinks.SinkFieldNameRule'),
        },
      ],
      props: (text, record, idx, isNew) => ({
        disabled: [110].includes(sinkValues?.status as number) && !isNew,
      }),
    },
    {
      title: i18n.t('meta.Sinks.SinkFieldType'),
      dataIndex: 'fieldType',
      width: 130,
      initialValue: hudiFieldTypes[0].value,
      type: 'select',
      rules: [{ required: true, message: `${i18n.t('meta.Sinks.FieldTypeMessage')}` }],
      props: (text, record, idx, isNew) => ({
        options: hudiFieldTypes,
        onChange: value => {
          const partitionStrategies = matchPartitionStrategies(value);
          if (partitionStrategies.every(item => item.value !== record.partitionStrategy)) {
            return {
              partitionStrategy: partitionStrategies[0].value,
            };
          }
        },
        disabled: [110].includes(sinkValues?.status as number) && !isNew,
      }),
    },
    {
      title: 'Length',
      dataIndex: 'fieldLength',
      type: 'inputnumber',
      props: {
        min: 0,
      },
      initialValue: 1,
      rules: [{ type: 'number', required: true }],
      visible: (text, record) => record.fieldType === 'fixed',
    },
    {
      title: 'Precision',
      dataIndex: 'fieldPrecision',
      type: 'inputnumber',
      props: {
        min: 0,
      },
      initialValue: 1,
      rules: [{ type: 'number', required: true }],
      visible: (text, record) => record.fieldType === 'decimal',
    },
    {
      title: 'Scale',
      dataIndex: 'fieldScale',
      type: 'inputnumber',
      props: {
        min: 0,
      },
      initialValue: 1,
      rules: [{ type: 'number', required: true }],
      visible: (text, record) => record.fieldType === 'decimal',
    },
    {
      title: i18n.t('meta.Sinks.FieldDescription'),
      dataIndex: 'fieldComment',
    },
  ];
};
