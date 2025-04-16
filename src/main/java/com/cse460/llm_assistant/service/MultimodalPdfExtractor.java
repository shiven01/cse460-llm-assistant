package com.cse460.llm_assistant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class MultimodalPdfExtractor {

    /**
     * Extracts text content from a PDF file with page numbers
     */
    public Map<Integer, String> extractText(byte[] pdfData) throws IOException {
        Map<Integer, String> pageTextMap = new HashMap<>();

        try (PDDocument document = Loader.loadPDF(pdfData)) {
            PDFTextStripper stripper = new PDFTextStripper();

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                String pageText = stripper.getText(document);
                pageTextMap.put(i + 1, pageText);
            }
        }

        return pageTextMap;
    }

    /**
     * Extracts images from a PDF file with page numbers
     * Returns a map of page number to list of image byte arrays
     */
    public Map<Integer, List<byte[]>> extractImages(byte[] pdfData) throws IOException {
        Map<Integer, List<byte[]>> pageImagesMap = new HashMap<>();

        try (PDDocument document = Loader.loadPDF(pdfData)) {
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                int pageNum = i + 1;
                PDPage page = document.getPage(i);
                PDResources resources = page.getResources();

                List<byte[]> pageImages = new ArrayList<>();

                // Extract images from the page
                for (COSName name : resources.getXObjectNames()) {
                    PDXObject xObject = resources.getXObject(name);
                    if (xObject instanceof PDImageXObject image) {
                        BufferedImage bufferedImage = image.getImage();

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "PNG", baos);
                        pageImages.add(baos.toByteArray());
                    }
                }

                if (!pageImages.isEmpty()) {
                    pageImagesMap.put(pageNum, pageImages);
                }
            }
        }

        return pageImagesMap;
    }
}