package com.jamsell.gethics.veterinary.application.internal.outboundservices;

import com.jamsell.gethics.veterinary.domain.model.aggregates.CareRecord;

public interface ClinicalHistoryWriter {

    void append(CareRecord record);
}
