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

import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { getCurrentLocale } from '@/configs/locales';
import { isDevelopEnv } from '@/core/utils';

const resources = {
  en: {
    translation: {
      'configs.menus.Process': 'Approval',
      'configs.menus.Groups': 'Ingestion',
      'configs.menus.GroupsManagement': 'Ingestion Management',
      'configs.menus.Groups.Template': 'Template Management',
      'configs.menus.Subscribe': 'Subscription',
      'configs.menus.Clusters': 'Clusters',
      'configs.menus.ClusterTags': 'ClusterTags',
      'configs.menus.SystemManagement': 'System',
      'configs.menus.UserManagement': 'User Management',
      'configs.menus.ProcessManagement': 'Process Management',
      'configs.menus.Nodes': 'DataNodes',
      'configs.menus.DataSynchronize': 'Synchronization',
      'configs.menus.TenantManagement': 'Tenant Management',
      'configs.menus.SystemOperation': 'Operation',
      'configs.menus.ModuleAudit': 'Module audit',
      'configs.menus.agentModule': 'Version Management',
      'configs.menus.agentPackage': 'Package',
    },
  },
  cn: {
    translation: {
      'configs.menus.Process': '审批管理',
      'configs.menus.Groups': '数据接入',
      'configs.menus.GroupsManagement': '接入管理',
      'configs.menus.Groups.Template': '模板管理',
      'configs.menus.Subscribe': '数据订阅',
      'configs.menus.Clusters': '集群管理',
      'configs.menus.ClusterTags': '标签管理',
      'configs.menus.SystemManagement': '系统管理',
      'configs.menus.UserManagement': '用户管理',
      'configs.menus.ProcessManagement': '流程管理',
      'configs.menus.Nodes': '数据节点',
      'configs.menus.DataSynchronize': '数据同步',
      'configs.menus.TenantManagement': '租户管理',
      'configs.menus.SystemOperation': '系统运维',
      'configs.menus.ModuleAudit': '模块审计',
      'configs.menus.agentModule': '版本管理',
      'configs.menus.agentPackage': '安装包',
    },
  },
};

i18n
  // .use(lngDetector)
  // pass the i18n instance to react-i18next.
  .use(initReactI18next)
  // init i18next
  // for all options read: https://www.i18next.com/overview/configuration-options
  .init({
    fallbackLng: 'en',
    resources,
    lng: getCurrentLocale(),
    debug: isDevelopEnv(),

    interpolation: {
      escapeValue: false, // not needed for react as it escapes by default
    },

    react: {
      useSuspense: false,
    },
  });

export default i18n;
