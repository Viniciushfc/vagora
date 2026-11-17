package com.viniciushfc.infra.adapter.in.rest.dto;

import com.viniciushfc.domain.enums.AlertSource;

public record EmailFolderRequest(String folder, AlertSource source) {
}
