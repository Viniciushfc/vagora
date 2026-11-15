package com.viniciushfc.infra.adapter.out.pdf;

import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.infra.adapter.out.pdf.apache.pdfbox.impl.PdfBoxTextStripperImpl;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PdfBoxTextStripperImplTest {

    private final PdfBoxTextStripperImpl stripper = new PdfBoxTextStripperImpl();

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

        assertTrue(stripper.extractTextByPdf(attachment).contains("Desenvolvedor Java Senior"));
    }

    @Test
    void rejeitaArquivoQueNaoEhPdf() {
        var attachment = new Attachment("cv.txt", "text/plain", new byte[]{1});

        assertThrows(IllegalArgumentException.class, () -> stripper.extractTextByPdf(attachment));
    }

    @Test
    void pdfCorrompidoLancaErro() {
        var attachment = new Attachment("cv.pdf", "application/pdf", "lixo".getBytes());

        assertThrows(RuntimeException.class, () -> stripper.extractTextByPdf(attachment));
    }
}
