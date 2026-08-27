package com.rockranger.analyzer.resume.extraction;

import com.rockranger.analyzer.resume.extraction.pdf.PdfOcrResumeTextExtractor;
import com.rockranger.analyzer.resume.extraction.pdf.PdfResumeTextExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ResumeExtractionRouter {

    private static final Logger log =
            LoggerFactory.getLogger(ResumeExtractionRouter.class);

    private final PdfResumeTextExtractor pdfResumeTextExtractor;
    private final PdfOcrResumeTextExtractor pdfOcrResumeTextExtractor;

    public ResumeExtractionRouter(
            PdfResumeTextExtractor pdfResumeTextExtractor,
            PdfOcrResumeTextExtractor pdfOcrResumeTextExtractor
    ) {
        this.pdfResumeTextExtractor = pdfResumeTextExtractor;
        this.pdfOcrResumeTextExtractor = pdfOcrResumeTextExtractor;
    }

    public String extract(
            byte[] fileBytes,
            String fileName
    ) {

        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException(
                    "Resume file is empty."
            );
        }

        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException(
                    "Resume file name is missing."
            );
        }

        String extension = getExtension(fileName);

        log.info(
                "Resume extraction started. File: {}, Type: {}",
                fileName,
                extension
        );

        return switch (extension) {

            case "pdf" ->
                    extractPdfWithFallback(fileBytes, fileName);

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported resume format: " + extension
                    );
        };
    }

    private String extractPdfWithFallback(
            byte[] fileBytes,
            String fileName
    ) {

        log.info(
                "Attempting PDF text extraction using PDFBox. File: {}",
                fileName
        );

        String extractedText;

        try {

            extractedText =
                    pdfResumeTextExtractor.extractText(fileBytes);

        } catch (Exception e) {

            log.warn(
                    "PDFBox extraction failed. Falling back to OCR. File: {}",
                    fileName,
                    e
            );

            return extractUsingOcr(fileBytes, fileName);
        }

        /*
         * PDFBox may technically return some text even when
         * the PDF is mostly scanned/image-based.
         *
         * Therefore, don't check only isBlank().
         */
        if (isUsableText(extractedText)) {

            log.info(
                    "PDFBox extracted usable text. OCR not required. File: {}",
                    fileName
            );

            return extractedText;
        }

        log.info(
                "PDFBox extracted insufficient text. Falling back to OCR. File: {}",
                fileName
        );

        return extractUsingOcr(fileBytes, fileName);
    }

    private String extractUsingOcr(
            byte[] fileBytes,
            String fileName
    ) {

        log.info(
                "Starting PDF OCR extraction. File: {}",
                fileName
        );

        String ocrText =
                pdfOcrResumeTextExtractor.extractText(fileBytes);

        if (!isUsableText(ocrText)) {

            throw new IllegalStateException(
                    "Unable to extract sufficient text from PDF using PDFBox and OCR."
            );
        }

        log.info(
                "PDF OCR extraction successful. Text length: {}",
                ocrText.length()
        );

        return ocrText;
    }

    private boolean isUsableText(String text) {

        if (text == null || text.isBlank()) {
            return false;
        }

        /*
         * Remove whitespace and check the amount of actual content.
         *
         * This is intentionally simple for now.
         * We can improve this later based on real resume data.
         */
        String normalized =
                text.replaceAll("\\s+", "");

        return normalized.length() >= 100;
    }

    private String getExtension(String fileName) {

        int lastDot =
                fileName.lastIndexOf('.');

        if (lastDot == -1 || lastDot == fileName.length() - 1) {

            throw new IllegalArgumentException(
                    "Resume file extension is missing."
            );
        }

        return fileName
                .substring(lastDot + 1)
                .toLowerCase();
    }
}