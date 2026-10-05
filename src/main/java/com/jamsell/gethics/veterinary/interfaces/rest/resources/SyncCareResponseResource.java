package com.jamsell.gethics.veterinary.interfaces.rest.resources;

import java.util.List;

public record SyncCareResponseResource(List<SyncResultResource> results) {
}
