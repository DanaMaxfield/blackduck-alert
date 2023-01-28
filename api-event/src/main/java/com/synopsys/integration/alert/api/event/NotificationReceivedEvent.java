/*
 * api-event
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.alert.api.event;

import java.util.UUID;

public class NotificationReceivedEvent extends AlertEvent {
    private static final long serialVersionUID = -5072672016837128957L;
    public static final String NOTIFICATION_RECEIVED_EVENT_TYPE = "notification_received_event";

    public final UUID correlationId;
    public final Long searchRangeStart;
    public final Long searchRangeEnd;

    public NotificationReceivedEvent(Long searchRangeStart, Long searchRangeEnd) {
        this(UUID.randomUUID(), searchRangeStart, searchRangeEnd);
    }

    public NotificationReceivedEvent(UUID correlationId, Long searchRangeStart, Long searchRangeEnd) {
        super(NOTIFICATION_RECEIVED_EVENT_TYPE);
        this.correlationId = correlationId;
        this.searchRangeStart = searchRangeStart;
        this.searchRangeEnd = searchRangeEnd;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public Long getSearchRangeStart() {
        return searchRangeStart;
    }

    public Long getSearchRangeEnd() {
        return searchRangeEnd;
    }
}
