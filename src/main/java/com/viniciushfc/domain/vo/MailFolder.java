package com.viniciushfc.domain.vo;

/**
 * Pasta da caixa de email e quantos alertas de vaga do LinkedIn ela contém.
 */
public record MailFolder(String name,
                         int jobAlerts) {
}
