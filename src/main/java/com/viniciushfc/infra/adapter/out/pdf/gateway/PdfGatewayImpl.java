package com.viniciushfc.infra.adapter.out.pdf.gateway;

import com.viniciushfc.domain.exceptions.ValidationException;
import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.vo.Attachment;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Collectors;

@ApplicationScoped
public class PdfGatewayImpl implements PdfGateway {

    static final float MARGIN = 50;
    static final float SIZE = 10.5f;
    static final float HEADING_SIZE = 12;
    static final String BULLET = "• ";

    private static final PDFont REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    @Override
    public String extractTextPdf(Attachment attachment) {
        try (PDDocument document = Loader.loadPDF(attachment.content())) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new ValidationException("Não foi possível ler o PDF " + attachment.name() + ".", e);
        }
    }

    @Override
    public byte[] createPdf(String text) {
        try (PDDocument document = new PDDocument()) {
            try (Writer writer = new Writer(document)) {
                for (String raw : text.strip().split("\\R")) {
                    String line = encodable(raw.replace('\t', ' ').strip());
                    if (line.isEmpty()) {
                        writer.space(SIZE * 0.6f);
                    } else if (isHeading(line)) {
                        writer.space(SIZE * 0.8f);
                        writer.paragraph(line, BOLD, HEADING_SIZE, "");
                        writer.space(2);
                    } else if (line.startsWith("- ")) {
                        writer.paragraph(BULLET + line.substring(2).strip(), REGULAR, SIZE, BULLET);
                    } else {
                        writer.paragraph(line, REGULAR, SIZE, "");
                    }
                }
            }
            if (document.getNumberOfPages() == 0) {
                document.addPage(new PDPage(PDRectangle.A4));
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Erro gerando o PDF do currículo", e);
        }
    }

    static boolean isHeading(String line) {
        return !line.startsWith("- ") && line.equals(line.toUpperCase()) && line.chars().anyMatch(Character::isLetter);
    }

    private static String encodable(String text) {
        return text.codePoints()
                .mapToObj(Character::toString)
                .filter(PdfGatewayImpl::canEncode)
                .collect(Collectors.joining());
    }

    private static boolean canEncode(String character) {
        try {
            REGULAR.encode(character);
            return true;
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
    }

    private static float width(String text, PDFont font, float size) throws IOException {
        return font.getStringWidth(text) / 1000 * size;
    }

    private static final class Writer implements Closeable {

        private final PDDocument document;
        private final float maxWidth = PDRectangle.A4.getWidth() - 2 * MARGIN;
        private PDPageContentStream stream;
        private float y;

        Writer(PDDocument document) {
            this.document = document;
        }

        void paragraph(String text, PDFont font, float size, String hangingIndent) throws IOException {
            float indent = 0;
            StringBuilder line = new StringBuilder();
            for (String word : text.split(" +")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && width(candidate, font, size) > maxWidth - indent) {
                    show(line.toString(), font, size, indent);
                    indent = width(hangingIndent, font, size);
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            show(line.toString(), font, size, indent);
        }

        void space(float points) {
            y -= points;
        }

        private void show(String text, PDFont font, float size, float indent) throws IOException {
            if (stream == null || y - size < MARGIN) {
                newPage();
            }
            y -= size;
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(MARGIN + indent, y);
            stream.showText(text);
            stream.endText();
            y -= size * 0.4f;
        }

        private void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PDRectangle.A4.getHeight() - MARGIN;
        }

        @Override
        public void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }
    }
}
