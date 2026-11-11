package com.viniciushfc.infra.adapter.out.pdf.apache.pdfbox.contracts;

import com.viniciushfc.domain.vo.Attachment;

import java.io.IOException;

public interface PdfBoxTextStripper {

    String extractTextByPdf(Attachment attachment) throws IOException;
}
