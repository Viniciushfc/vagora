package com.viniciushfc.infra.adapter.out.pdf.gateway;

import com.viniciushfc.domain.exceptions.ValidationException;
import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.vo.Attachment;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;

/** Extrai o texto do PDF com PDFBox. Que o arquivo é um PDF já foi validado no upload (CurriculumValidator). */
@ApplicationScoped
public class PdfGatewayImpl implements PdfGateway {

    @Override
    public String extractTextPdf(Attachment attachment) {
        try (PDDocument document = Loader.loadPDF(attachment.content())) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new ValidationException("Não foi possível ler o PDF " + attachment.name() + ".", e);
        }
    }
}
