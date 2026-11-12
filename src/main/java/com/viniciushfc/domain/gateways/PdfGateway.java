package com.viniciushfc.domain.gateways;

import com.viniciushfc.domain.vo.Attachment;

import java.io.IOException;

public interface PdfGateway {

    byte[] generatePdf();

    String extractTextPdf(Attachment attachment) throws IOException;
}
