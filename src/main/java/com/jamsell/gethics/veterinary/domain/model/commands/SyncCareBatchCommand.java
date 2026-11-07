package com.jamsell.gethics.veterinary.domain.model.commands;

import java.util.List;

public record SyncCareBatchCommand(List<RegisterCareCommand> items) {
}
