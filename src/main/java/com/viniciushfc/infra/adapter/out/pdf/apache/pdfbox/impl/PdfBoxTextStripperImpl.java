package com.viniciushfc.infra.adapter.out.pdf.apache.pdfbox.impl;

import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.infra.adapter.out.pdf.apache.pdfbox.contracts.PdfBoxTextStripper;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;

@ApplicationScoped
public class PdfBoxTextStripperImpl implements PdfBoxTextStripper {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    @Override
    public String extractTextByPdf(Attachment attachment) throws IOException {
        if (!PDF_CONTENT_TYPE.equalsIgnoreCase(attachment.contentType())) {
            throw new IllegalArgumentException("Attachment não é um PDF: " + attachment.contentType());
        }

        try (PDDocument pdDocument = Loader.loadPDF(attachment.content())) {
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            return pdfTextStripper.getText(pdDocument);

        } catch (Exception e) {
            throw new RuntimeException("Falha ao ler o PDF " + attachment.name(), e);
        }
    }
}
