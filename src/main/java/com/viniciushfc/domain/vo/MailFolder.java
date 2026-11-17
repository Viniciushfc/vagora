package com.viniciushfc.domain.vo;

import com.viniciushfc.domain.enums.AlertSource;

import java.util.Map;

public record MailFolder(String name,
                         Map<AlertSource, Integer> jobAlerts) {

    public int alerts(AlertSource source) {
        return jobAlerts.getOrDefault(source, 0);
    }
}
