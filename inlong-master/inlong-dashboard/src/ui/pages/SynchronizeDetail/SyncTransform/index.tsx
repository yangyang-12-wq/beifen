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

import React, { useState, forwardRef, useMemo, useCallback } from 'react';
import { Badge, Button, Card, Modal, List, Segmented, message } from 'antd';
import { PaginationConfig } from 'antd/lib/pagination';
import HighTable from '@/ui/components/HighTable';
import { defaultSize } from '@/configs/pagination';
import { useRequest } from '@/ui/hooks';
import DetailModal from './DetailModal';
import i18n from '@/i18n';
import request from '@/core/utils/request';
import { CommonInterface } from '../common';
import {
  DeleteOutlined,
  EditOutlined,
  TableOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons';

interface Props extends CommonInterface {
  inlongStreamId: string;
}

const Comp = ({ inlongGroupId, inlongStreamId, readonly }: Props, ref) => {
  const [mode, setMode] = useState('list');

  const defaultOptions = {
    pageSize: defaultSize,
    pageNum: 1,
  };

  const [options, setOptions] = useState(defaultOptions);

  const [createModal, setCreateModal] = useState<Record<string, unknown>>({
    open: false,
  });

  const {
    data,
    loading,
    run: getList,
  } = useRequest(
    {
      url: '/transform/list',
      method: 'POST',
      data: {
        ...options,
        inlongGroupId,
        inlongStreamId,
      },
    },
    {
      refreshDeps: [options],
    },
  );

  const onEdit = useCallback(({ id }) => {
    setCreateModal({ open: true, id });
  }, []);

  const onDelete = useCallback(
    ({ transformName }) => {
      Modal.confirm({
        title: i18n.t('pages.GroupDetail.Sources.DeleteConfirm'),
        onOk: async () => {
          await request({
            url: `/transform/delete`,
            method: 'DELETE',
            params: {
              inlongGroupId,
              inlongStreamId,
              transformName,
            },
          });
          await getList();
          message.success(i18n.t('pages.GroupDetail.Sources.DeleteSuccessfully'));
        },
      });
    },
    [getList, inlongGroupId, inlongStreamId],
  );

  const onChange = useCallback(({ current: pageNum, pageSize }) => {
    setOptions(prev => ({
      ...prev,
      pageNum,
      pageSize,
    }));
  }, []);

  const onFilter = useCallback(allValues => {
    setOptions(prev => ({
      ...prev,
      ...allValues,
      pageNum: 1,
    }));
  }, []);

  const pagination: PaginationConfig = {
    pageSize: options.pageSize,
    current: options.pageNum,
    total: data?.total,
    simple: true,
    size: 'small',
  };

  const getFilterFormContent = useCallback(
    defaultValues => [
      {
        type: 'inputsearch',
        name: 'keyword',
      },
    ],
    [],
  );

  const columns = useMemo(() => {
    return [
      {
        title: i18n.t('meta.Transform.Name'),
        dataIndex: 'transformName',
      },
      {
        title: i18n.t('basic.Operating'),
        dataIndex: 'action',
        render: (text, record) =>
          readonly ? (
            '-'
          ) : (
            <>
              <Button type="link" onClick={() => onEdit(record)}>
                {i18n.t('basic.Edit')}
              </Button>
              <Button type="link" onClick={() => onDelete(record)}>
                {i18n.t('basic.Delete')}
              </Button>
            </>
          ),
      },
    ];
  }, [onDelete, onEdit, readonly]);

  return (
    <>
      <Card
        size="small"
        title={
          <Badge size="small" count={data?.total} offset={[12, 3]}>
            {i18n.t('pages.SynchronizeDetail.Transform')}
          </Badge>
        }
        style={{ height: '100%' }}
        extra={[
          !readonly && (
            <Button
              key="create"
              type="link"
              style={{ visibility: data?.total === 1 ? 'hidden' : 'visible' }}
              onClick={() => setCreateModal({ open: true })}
            >
              {i18n.t('basic.Create')}
            </Button>
          ),

          <Segmented
            key="mode"
            onChange={(value: string) => {
              setMode(value);
              setOptions(defaultOptions);
            }}
            options={[
              {
                value: 'list',
                icon: <UnorderedListOutlined />,
              },
              {
                value: 'table',
                icon: <TableOutlined />,
              },
            ]}
            defaultValue={mode}
            size="small"
          />,
        ]}
      >
        {mode === 'list' ? (
          <List
            size="small"
            loading={loading}
            dataSource={data?.list as Record<string, any>[]}
            pagination={pagination}
            split={true}
            renderItem={item => (
              <List.Item
                actions={[
                  <Button key="edit" type="link" onClick={() => onEdit(item)}>
                    <EditOutlined />
                  </Button>,
                  <Button key="del" type="link" onClick={() => onDelete(item)}>
                    <DeleteOutlined />
                  </Button>,
                ]}
              >
                <span>
                  <span style={{ marginRight: 10 }}>{item.transformName}</span>
                </span>
              </List.Item>
            )}
          />
        ) : (
          <HighTable
            filterForm={{
              content: getFilterFormContent(options),
              onFilter,
            }}
            table={{
              columns,
              rowKey: 'id',
              size: 'small',
              dataSource: data?.list,
              pagination,
              loading,
              onChange,
            }}
          />
        )}
      </Card>

      <DetailModal
        {...createModal}
        inlongGroupId={inlongGroupId}
        inlongStreamId={inlongStreamId}
        open={createModal.open as boolean}
        onOk={async () => {
          await getList();
          setCreateModal({ open: false });
        }}
        onCancel={() => setCreateModal({ open: false })}
      />
    </>
  );
};

export default forwardRef(Comp);
