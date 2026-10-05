package com.jamsell.gethics.notifications.domain.model.valueobjects;

public record PushNotificationDispatchResult(PushNotificationDispatchOutcome outcome) {

    public boolean wasSent() {
        return outcome == PushNotificationDispatchOutcome.SENT;
    }
}
