package com.viniciushfc.infra.adapter.out.pdf.gateway;

import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.infra.adapter.out.pdf.apache.pdfbox.contracts.PdfBoxTextStripper;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;

@ApplicationScoped
public class PdfGatewayImpl implements PdfGateway {

    private final PdfBoxTextStripper pdfBoxTextStripper;

    public PdfGatewayImpl(PdfBoxTextStripper pdfBoxTextStripper) {
        this.pdfBoxTextStripper = pdfBoxTextStripper;
    }

    @Override
    public byte[] generatePdf() {
        return new byte[0];
    }

    @Override
    public String extractTextPdf(Attachment attachment) throws IOException {
        return pdfBoxTextStripper.extractTextByPdf(attachment);
    }
}
