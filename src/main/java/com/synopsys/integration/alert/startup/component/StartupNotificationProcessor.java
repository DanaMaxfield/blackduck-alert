package com.synopsys.integration.alert.startup.component;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.synopsys.integration.alert.api.event.EventManager;
import com.synopsys.integration.alert.api.event.NotificationReceivedEvent;
import com.synopsys.integration.alert.common.persistence.accessor.NotificationAccessor;
import com.synopsys.integration.alert.common.rest.model.AlertNotificationModel;
import com.synopsys.integration.alert.common.util.DateUtils;

@Component
@Order(900)
public class StartupNotificationProcessor extends StartupComponent {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final EventManager eventManager;
    private final NotificationAccessor notificationAccessor;

    @Autowired
    public StartupNotificationProcessor(EventManager eventManager, NotificationAccessor notificationAccessor) {
        this.eventManager = eventManager;
        this.notificationAccessor = notificationAccessor;
    }

    @Override
    protected void initialize() {
        logger.info("Checking for unprocessed notifications at startup.");
        Optional<AlertNotificationModel> startNotification = notificationAccessor.getFirstNotificationNotProcessed();
        Optional<AlertNotificationModel> endNotification = notificationAccessor.getLastNotificationNotProcessed();
        logger.info("Start time found: {}", startNotification.isPresent());
        logger.info("End time found: {}", endNotification.isPresent());
        startNotification
            .map(AlertNotificationModel::getCreatedAt)
            .ifPresent(time -> logger.info("Earliest unprocessed notification at: {}", DateUtils.formatDateAsJsonString(time)));
        endNotification
            .map(AlertNotificationModel::getCreatedAt)
            .ifPresent(time -> logger.info("Latest unprocessed notification at: {}", DateUtils.formatDateAsJsonString(time)));

        if (startNotification.isPresent() && endNotification.isPresent()) {
            OffsetDateTime start = startNotification.get().getCreatedAt();
            OffsetDateTime end = endNotification.get().getCreatedAt();
            eventManager.sendEvent(new NotificationReceivedEvent(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli()));
        }
    }
}
