package com.viniciushfc.domain.vo;

import java.util.List;

public record Email(String from,
                    String to,
                    String subject,
                    String body,
                    List<Attachment> attachments) {

}
