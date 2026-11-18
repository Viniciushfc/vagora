package com.viniciushfc.domain.validators;

import com.viniciushfc.domain.exceptions.ValidationException;
import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.vo.Attachment;

public class CurriculumValidator {

    public static final int MAX_SIZE_BYTES = 5 * 1024 * 1024;
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private CurriculumValidator() {
    }

    public static void validate(Attachment curriculum, PdfGateway pdfGateway) {
        if (curriculum == null || curriculum.content() == null || curriculum.content().length == 0) {
            throw new ValidationException("Envie o currículo em PDF.");
        }

        if (!PDF_CONTENT_TYPE.equalsIgnoreCase(curriculum.contentType())) {
            throw new ValidationException("O currículo precisa ser um PDF (recebido: " + curriculum.contentType() + ").");
        }

        if (curriculum.content().length > MAX_SIZE_BYTES) {
            throw new ValidationException("O currículo tem " + toMb(curriculum.content().length)
                    + " MB; o limite é " + toMb(MAX_SIZE_BYTES) + " MB.");
        }

        if (pdfGateway.extractTextPdf(curriculum).isBlank()) {
            throw new ValidationException("O PDF não tem texto selecionável (parece uma imagem escaneada). "
                    + "Envie o currículo exportado do editor de texto.");
        }
    }

    private static String toMb(int bytes) {
        return String.format(java.util.Locale.ROOT, "%.1f", bytes / (1024.0 * 1024.0));
    }
}
