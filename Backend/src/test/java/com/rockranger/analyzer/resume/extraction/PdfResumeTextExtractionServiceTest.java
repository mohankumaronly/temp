package com.rockranger.analyzer.resume.extraction;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PdfResumeTextExtractionServiceTest {

    private PdfResumeTextExtractionService extractionService;

    @BeforeEach
    void setUp() {
        extractionService = new PdfResumeTextExtractionService();
    }

    @Test
    void testExtractTextFromValidPdf() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText("Mohan Kumar");
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("Java Developer");
                contentStream.endText();
            }
            document.save(baos);
        }

        byte[] pdfBytes = baos.toByteArray();
        String extractedText = extractionService.extractText(pdfBytes);

        assertNotNull(extractedText);
        assertTrue(extractedText.contains("Mohan Kumar"));
        assertTrue(extractedText.contains("Java Developer"));
    }

    @Test
    void testExtractTextWithEmptyBytesThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> extractionService.extractText(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> extractionService.extractText(null));
    }
}
