package com.jamsell.gethics.sanitary.interfaces.rest.resources;

import java.util.List;

public record SanitaryCalendarResource(int year, int month, List<ScheduledEventResource> events, String message) {
}
