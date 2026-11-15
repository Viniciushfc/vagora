package com.viniciushfc.domain.gateways;

import com.viniciushfc.domain.vo.Attachment;

public interface PdfGateway {

    String extractTextPdf(Attachment attachment);
}
