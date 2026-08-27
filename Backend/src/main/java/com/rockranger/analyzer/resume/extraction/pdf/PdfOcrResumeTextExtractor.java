package com.rockranger.analyzer.resume.extraction.pdf;

import com.rockranger.analyzer.resume.extraction.ResumeTextExtractor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PdfOcrResumeTextExtractor implements ResumeTextExtractor {

    private final String tesseractPath;
    private final String language;
    private final int dpi;
    private final long timeoutSeconds;

    public PdfOcrResumeTextExtractor(
            @Value("${ocr.tesseract.path}") String tesseractPath,
            @Value("${ocr.tesseract.language:eng}") String language,
            @Value("${ocr.tesseract.dpi:200}") int dpi,
            @Value("${ocr.tesseract.timeout-seconds:60}") long timeoutSeconds
    ) {
        this.tesseractPath = tesseractPath;
        this.language = language;
        this.dpi = dpi;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String extractText(byte[] fileBytes) {

        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException(
                    "PDF file is empty."
            );
        }

        Path temporaryImage = null;

        try (PDDocument document = Loader.loadPDF(fileBytes)) {

            PDFRenderer renderer = new PDFRenderer(document);

            StringBuilder extractedText = new StringBuilder();

            for (int page = 0; page < document.getNumberOfPages(); page++) {

                BufferedImage image =
                        renderer.renderImageWithDPI(page, dpi);

                temporaryImage = Files.createTempFile(
                        "resume-page-" + page + "-",
                        ".png"
                );

                ImageIO.write(
                        image,
                        "png",
                        temporaryImage.toFile()
                );

                String pageText =
                        runTesseract(temporaryImage);

                if (pageText != null && !pageText.isBlank()) {

                    extractedText
                            .append(pageText)
                            .append("\n");
                }

                Files.deleteIfExists(temporaryImage);
                temporaryImage = null;
            }

            return cleanText(extractedText.toString());

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to perform OCR extraction from PDF.",
                    e
            );

        } finally {

            if (temporaryImage != null) {

                try {
                    Files.deleteIfExists(temporaryImage);
                } catch (IOException ignored) {
                    // Cleanup failure should not hide the original error.
                }
            }
        }
    }

    private String runTesseract(Path imagePath) throws IOException {

        ProcessBuilder processBuilder = new ProcessBuilder(
                tesseractPath,
                imagePath.toAbsolutePath().toString(),
                "stdout",
                "-l",
                language
        );

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     process.getInputStream(),
                                     StandardCharsets.UTF_8
                             )
                     )) {

            String line;

            while ((line = reader.readLine()) != null) {

                output
                        .append(line)
                        .append("\n");
            }
        }

        try {

            boolean completed =
                    process.waitFor(
                            timeoutSeconds,
                            java.util.concurrent.TimeUnit.SECONDS
                    );

            if (!completed) {

                process.destroyForcibly();

                throw new IOException(
                        "Tesseract OCR timed out after "
                                + timeoutSeconds
                                + " seconds."
                );
            }

        } catch (InterruptedException e) {

            process.destroyForcibly();

            Thread.currentThread().interrupt();

            throw new IOException(
                    "Tesseract OCR process was interrupted.",
                    e
            );
        }

        int exitCode = process.exitValue();

        if (exitCode != 0) {

            throw new IOException(
                    "Tesseract OCR failed. Exit code: "
                            + exitCode
                            + ". Output: "
                            + output
            );
        }

        return output.toString();
    }

    private String cleanText(String text) {

        if (text == null || text.isBlank()) {
            return "";
        }

        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[ \t]+", " ")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}