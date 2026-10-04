package com.jamsell.gethics.veterinary.interfaces.rest.resources;

import java.util.List;

public record AssignedClientsResource(List<AssignedClientResource> clients, String message) {
}

