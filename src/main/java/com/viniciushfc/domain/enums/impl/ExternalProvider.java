package com.viniciushfc.domain.enums.impl;


import com.viniciushfc.domain.enums.contracts.DataEnum;
import com.viniciushfc.domain.enums.contracts.IEnum;

public enum ExternalProvider implements IEnum<String, String> {
    LINKEDIN("LINKEDIN", "Linkedin"),
    ;

    private final DataEnum<String, String> data;

    ExternalProvider(String key, String value) {
        this.data = new DataEnum<>(key, value);
    }

    @Override
    public DataEnum<String, String> data() {
        return data;
    }
}
