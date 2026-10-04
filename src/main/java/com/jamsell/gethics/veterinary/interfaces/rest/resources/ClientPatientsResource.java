package com.jamsell.gethics.veterinary.interfaces.rest.resources;

import java.util.List;

public record ClientPatientsResource(List<PatientResource> patients, String message) {
}
