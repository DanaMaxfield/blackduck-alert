/**
 * workflow
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.alert.workflow.message;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.synopsys.integration.alert.common.channel.ChannelEventManager;
import com.synopsys.integration.alert.common.enumeration.FrequencyType;
import com.synopsys.integration.alert.common.event.AlertEventListener;
import com.synopsys.integration.alert.common.event.DistributionEvent;
import com.synopsys.integration.alert.common.event.NotificationEvent;
import com.synopsys.integration.alert.common.persistence.accessor.NotificationAccessor;
import com.synopsys.integration.alert.common.rest.model.AlertNotificationModel;
import com.synopsys.integration.alert.common.workflow.MessageReceiver;
import com.synopsys.integration.alert.common.workflow.processor.notification.NotificationProcessor;

@Component(value = NotificationReceiver.COMPONENT_NAME)
public class NotificationReceiver extends MessageReceiver<NotificationEvent> implements AlertEventListener {
    public static final String COMPONENT_NAME = "notification_receiver";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final NotificationAccessor notificationAccessor;
    private final NotificationProcessor notificationProcessor;
    private final ChannelEventManager eventManager;

    @Autowired
    public NotificationReceiver(Gson gson, NotificationAccessor notificationAccessor, NotificationProcessor notificationProcessor, ChannelEventManager eventManager) {
        super(gson, NotificationEvent.class);
        this.notificationAccessor = notificationAccessor;
        this.notificationProcessor = notificationProcessor;
        this.eventManager = eventManager;
    }

    @Override
    public void handleEvent(NotificationEvent event) {
        if (NotificationEvent.NOTIFICATION_EVENT_TYPE.equals(event.getDestination())) {
            logger.debug("Event {}", event);
            logger.info("Processing event for notifications.");

            int numPagesProcessed = 0;

            /*
            //pseudo code
            //look for the first 10 notifications
            Page<AlertNotificationModel> notifications = notificationAccessor.findNotificationsNotProcessed(PageRequest pageRequest);
            //some loop condition, check if there are existing notifications
            while (moreNotificationsToProcess) {
                List<DistributionEvent> distributionEvents = notificationProcessor.processNotifications(FrequencyType.REAL_TIME, notifications);
                notificationAccessor.processedNotification(notifications);
                eventManager.sendEvents(distributionEvents);
                notifications = notificationAccessor.findNotificationsNotProcessed(PageRequest pageRequest);
            }*/
            // ###############
            logger.info("====== RECEIVED ====== Processing event for notifications."); //TODO: Delete this log

            Page<AlertNotificationModel> pageOfAlertNotificationModels = notificationAccessor.findNotificationsNotProcessed();
            logger.info("====== Initial total pages before loop: {} ======", pageOfAlertNotificationModels.getTotalPages());
            //get content, if not null and not empty, then go into the loop
            //Idea: MAX_NUMBER_OF_PAGES, set to "1000" upper bound so that this loop is not stuck forever
            while (pageOfAlertNotificationModels.getTotalPages() > 0 && numPagesProcessed < 1000) {
                List<AlertNotificationModel> notifications = pageOfAlertNotificationModels.getContent();
                logger.info("====== SIZE OF NOTIFICATIONS ====== Sending {} notifications.", notifications.size()); //TODO clean up this log message
                List<DistributionEvent> distributionEvents = notificationProcessor.processNotifications(FrequencyType.REAL_TIME, notifications);
                logger.info("====== SENDING DISTRIBUTION EVENTS ====== Sending {} events for notifications.", distributionEvents.size()); //TODO clean up this log message
                eventManager.sendEvents(distributionEvents); //TODO: investigate this, does sendEvents need to be @Transactional
                //TODO: Put a sleep?
                logger.info("====== FINISHED SENDING EVENTS ======"); //TODO clean up this log message
                notificationAccessor.setNotificationsProcessed(notifications);
                logger.info("====== Setting Notifications to processed =====");
                pageOfAlertNotificationModels = notificationAccessor.findNotificationsNotProcessed();
                logger.info("====== New total pages: {} ======", pageOfAlertNotificationModels.getTotalPages());
                numPagesProcessed++;
            }
            logger.info("===== Exiting While loop, no pages should remain =====");
            // ###############

            /*
            //TODO: Old code with my modifications
            if (null == event.getNotificationIds() || event.getNotificationIds().isEmpty()) {
                logger.warn("Can not process a notification event without notification Id's.");
                return;
            }
            logger.debug("Event {}", event);
            logger.info("====== PROCESSING ====== Processing event for {} notifications.", event.getNotificationIds().size());
            //TODO Delete me

            //instead of findingById's we want to get a page of notifications sorted by date, by oldest, limit 10
            //Page<AlertNotificationModel> notifications = notificationAccessor.findNotificationsNotProcessed(PageRequest pageRequest);
            //pull the models out of the page
            List<AlertNotificationModel> notifications = notificationAccessor.findByIds(event.getNotificationIds());
            List<DistributionEvent> distributionEvents = notificationProcessor.processNotifications(FrequencyType.REAL_TIME, notifications);
            logger.info("====== SENDING ====== Sending {} events for notifications.", distributionEvents.size());
            eventManager.sendEvents(distributionEvents);

             */

        } else {
            logger.warn("Received an event of type '{}', but this listener is for type '{}'.", event.getDestination(), NotificationEvent.NOTIFICATION_EVENT_TYPE);
        }
    }

    @Override
    public String getDestinationName() {
        return NotificationEvent.NOTIFICATION_EVENT_TYPE;
    }

    /*
    //TODO create a method to get a PageRequest of notifications with processed = false, sorted by oldest Date
    // update: maybe not, we shouldn't manage paging here, instead do it in the NotificationAccessor
    private PageRequest getPageRequest() {

    }*/
}
