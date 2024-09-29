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

import React, { useMemo } from 'react';
import { Modal, message } from 'antd';
import { ModalProps } from 'antd/es/modal';
import FormGenerator, { useForm } from '@/ui/components/FormGenerator';
import { useUpdateEffect } from '@/ui/hooks';
import i18n from '@/i18n';
import request from '@/core/utils/request';

export interface Props extends ModalProps {
  name?: string;
  record?: Record<string, any>;
}

const Comp: React.FC<Props> = ({ name, ...modalProps }) => {
  const [form] = useForm();

  const formContent = useMemo(() => {
    return [
      {
        type: 'input',
        label: i18n.t('pages.Tenant.config.Name'),
        name: 'name',
        rules: [{ required: true }],
      },
      {
        type: 'textarea',
        label: i18n.t('pages.Tenant.config.Description'),
        name: 'description',
        rules: [{ required: true }],
        props: {
          showCount: true,
          maxLength: 256,
          autoSize: { minRows: 4, maxRows: 8 },
        },
      },
    ];
  }, []);

  const onOk = async () => {
    const values = await form.validateFields();
    await request({
      url: '/tenant/save',
      method: 'POST',
      data: { ...values },
    });
    await modalProps?.onOk(values);
    message.success(i18n.t('basic.OperatingSuccess'));
  };

  useUpdateEffect(() => {
    if (modalProps.open) {
      form.resetFields();
    }
  }, [modalProps.open]);

  return (
    <Modal {...modalProps} title={i18n.t('pages.Tenant.New')} width={600} onOk={onOk}>
      <FormGenerator
        labelCol={{ span: 5 }}
        wrapperCol={{ span: 20 }}
        content={formContent}
        form={form}
        useMaxWidth
      />
    </Modal>
  );
};

export default Comp;
