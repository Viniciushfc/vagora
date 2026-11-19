package com.viniciushfc.infra.adapter.out.pdf;

import com.viniciushfc.domain.exceptions.ValidationException;
import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.infra.adapter.out.pdf.gateway.PdfGatewayImpl;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PdfGatewayImplTest {

    private final PdfGatewayImpl gateway = new PdfGatewayImpl();

    private byte[] pdf(String text) throws IOException {
        try (var doc = new PDDocument(); var out = new ByteArrayOutputStream()) {
            var page = new PDPage();
            doc.addPage(page);
            try (var content = new PDPageContentStream(doc, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText(text);
                content.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void extraiTextoDoPdf() throws IOException {
        var attachment = new Attachment("cv.pdf", "application/pdf", pdf("Desenvolvedor Java Senior"));

        assertTrue(gateway.extractTextPdf(attachment).contains("Desenvolvedor Java Senior"));
    }

    @Test
    void pdfCorrompidoLancaErro() {
        var attachment = new Attachment("cv.pdf", "application/pdf", "lixo".getBytes());

        assertThrows(ValidationException.class, () -> gateway.extractTextPdf(attachment));
    }

    @Test
    void geraPdfDoCurriculoComAcentosEVariasPaginas() throws IOException {
        String curriculo = "EXPERIÊNCIA PROFISSIONAL\n" + "- Ação em produção com Java e Quarkus 🚀\n".repeat(120);

        byte[] pdf = gateway.createPdf(curriculo);
        String text = gateway.extractTextPdf(new Attachment("cv.pdf", "application/pdf", pdf));

        assertTrue(text.contains("EXPERIÊNCIA PROFISSIONAL"));
        assertTrue(text.contains("• Ação em produção com Java e Quarkus"));
        assertFalse(text.contains("🚀"));
        try (var document = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            assertTrue(document.getNumberOfPages() > 1);
        }
    }
}
