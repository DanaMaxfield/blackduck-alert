/**
 * hub-alert
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.alert.accumulator;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;

import com.blackducksoftware.integration.hub.alert.NotificationManager;
import com.blackducksoftware.integration.hub.alert.channel.ChannelTemplateManager;
import com.blackducksoftware.integration.hub.alert.event.DBStoreEvent;
import com.blackducksoftware.integration.hub.alert.event.RealTimeEvent;
import com.blackducksoftware.integration.hub.alert.hub.model.NotificationModel;

@Transactional
public class AccumulatorWriter implements ItemWriter<DBStoreEvent> {
    private static final String COMPONENT_VERSION_UNKNOWN = "UNKNOWN";
    private final static Logger logger = LoggerFactory.getLogger(AccumulatorWriter.class);
    private final NotificationManager notificationManager;
    private final ChannelTemplateManager channelTemplateManager;

    public AccumulatorWriter(final NotificationManager notificationManager, final ChannelTemplateManager channelTemplateManager) {
        this.notificationManager = notificationManager;
        this.channelTemplateManager = channelTemplateManager;
    }

    @Override
    public void write(final List<? extends DBStoreEvent> itemList) throws Exception {
        try {
            if (itemList != null && !itemList.isEmpty()) {
                logger.info("Writing {} notifications", itemList.size());
                itemList.forEach(item -> {
                    final List<NotificationModel> notificationList = item.getNotificationList();
                    final List<NotificationModel> entityList = new ArrayList<>();
                    notificationList.forEach(notification -> {
                        notificationManager.saveNotification(notification);
                        entityList.add(notification);
                    });
                    final RealTimeEvent realTimeEvent = new RealTimeEvent(entityList);
                    channelTemplateManager.sendEvent(realTimeEvent);
                });
            } else {
                logger.info("No notifications to write");
            }
        } catch (final Exception ex) {
            logger.error("Error occurred writing notification data", ex);
        }
    }

    // private Optional<NotificationModel> createNotificationModel(final NotificationEvent notification) {
    // try {
    // final String eventKey = notification.getEventKey();
    // final NotificationContentItem content = (NotificationContentItem) notification.getDataSet().get(NotificationEvent.DATA_SET_KEY_NOTIFICATION_CONTENT);
    // final Date createdAt = content.getCreatedAt();
    // final NotificationCategoryEnum notificationType = notification.getCategoryType();
    // final String projectName = content.getProjectVersion().getProjectName();
    // final String projectUrl = content.getProjectVersion().getProjectLink();
    // final String projectVersion = content.getProjectVersion().getProjectVersionName();
    // final String projectVersionUrl = content.getProjectVersion().getUrl();
    // final String componentName = content.getComponentName();
    // final String componentVersion = getComponentVersionName(content.getComponentVersion());
    // final String policyRuleName = getPolicyRule(notification);
    // final String person = getPerson(notification);
    //
    // final NotificationEntity entity = new NotificationEntity(eventKey, createdAt, notificationType, projectName, projectUrl, projectVersion, projectVersionUrl, componentName, componentVersion, policyRuleName, person);
    // final Collection<VulnerabilityEntity> vulnerabilityList = getVulnerabilities(notification, entity);
    // final NotificationModel model = new NotificationModel(entity, vulnerabilityList);
    // return Optional.of(model);
    // } catch (final Exception ex) {
    // // component version could be null if a versionless component is added.
    // // catch the exception and continue writing notifications that can be handled.
    // logger.error("Accumulator writer failed to create a notification model...");
    // logger.error("NotificationEvent {}", notification);
    // logger.error("Exception caused by: ", ex);
    //
    // return Optional.empty();
    // }
    // }

    // private String getComponentVersionName(final ComponentVersionView componentVersionView) {
    // if (componentVersionView == null) {
    // return COMPONENT_VERSION_UNKNOWN;
    // } else {
    // return componentVersionView.versionName;
    // }
    // }
    //
    // private String getPolicyRule(final NotificationEvent notification) {
    // final String key = ItemTypeEnum.RULE.name();
    // if (notification.getDataSet().containsKey(key)) {
    // final String rule = (String) notification.getDataSet().get(key);
    // return rule;
    // }
    //
    // return "";
    // }
    //
    // private String getPerson(final NotificationEvent notification) {
    // final String key = ItemTypeEnum.PERSON.name();
    // if (notification.getDataSet().containsKey(key)) {
    // final String person = (String) notification.getDataSet().get(key);
    // return person;
    // }
    //
    // return "";
    // }

    // // The dataset contains string keys and object values. Therefore we need to type cast because the contents are various types.
    // @SuppressWarnings("unchecked")
    // private Collection<VulnerabilityEntity> getVulnerabilities(final NotificationEvent notification, final NotificationEntity entity) {
    // final List<VulnerabilityEntity> vulnerabilityList = new ArrayList<>();
    // final String key = VulnerabilityCache.VULNERABILITY_OPERATION;
    // if (notification.getDataSet().containsKey(key)) {
    // final String operationName = (String) notification.getDataSet().get(key);
    // final Set<String> vulnerabilitySet = (Set<String>) notification.getDataSet().get(VulnerabilityCache.VULNERABILITY_ID_SET);
    //
    // if (!vulnerabilitySet.isEmpty()) {
    // vulnerabilitySet.forEach(vulnerability -> {
    // final VulnerabilityEntity vulnerabilityEntity = new VulnerabilityEntity(vulnerability, VulnerabilityOperationEnum.valueOf(operationName), entity.getId());
    // vulnerabilityList.add(vulnerabilityEntity);
    // });
    // }
    // }
    //
    // return vulnerabilityList;
    // }
}
