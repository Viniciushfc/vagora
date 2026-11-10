package com.viniciushfc.domain.enums.contracts;


public interface IEnum<K, V> {

    DataEnum<K, V> data();

    default K getKey() {
        return data().key();
    }

    default V getValue() {
        return data().value();
    }
}
