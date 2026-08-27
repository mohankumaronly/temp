package com.rockranger.analyzer.resume.extraction.pdf;

import com.rockranger.analyzer.resume.extraction.ResumeTextExtractor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class PdfResumeTextExtractor implements ResumeTextExtractor {

    @Override
    public String extractText(byte[] fileBytes) {

        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("PDF file is empty.");
        }

        try (PDDocument document = Loader.loadPDF(fileBytes)) {

            PDFTextStripper textStripper = new PDFTextStripper();

            String extractedText = textStripper.getText(document);

            return extractedText
                    .replace("\r\n", "\n")
                    .replace("\r", "\n")
                    .replaceAll("[ \t]+", " ")
                    .trim();

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to extract text from PDF.",
                    e
            );
        }
    }
}