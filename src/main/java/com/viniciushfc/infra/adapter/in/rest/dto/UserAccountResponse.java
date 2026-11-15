package com.viniciushfc.infra.adapter.in.rest.dto;

import java.util.List;
import java.util.UUID;

public record UserAccountResponse(UUID id,
                                  String name,
                                  String username,
                                  String curriculumName,
                                  List<LinkedAccountResponse> linkedAccounts) {
}
