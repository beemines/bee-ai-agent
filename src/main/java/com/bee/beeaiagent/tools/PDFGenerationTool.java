package com.bee.beeaiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.bee.beeaiagent.constant.FileConstant;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PDFGenerationTool {

    @Tool(description = "生成 PDF 文件。除非用户明确要求英文，否则 PDF 内容应使用简体中文。")
    public String generatePDF(
            @ToolParam(description = "要保存的 PDF 文件名") String fileName,
            @ToolParam(description = "要写入 PDF 的内容，默认使用简体中文") String content) {
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String filePath = fileDir + "/" + fileName;
        try {
            FileUtil.mkdir(fileDir);
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                document.setFont(createChineseFont());
                document.add(new Paragraph(content));
            }
            return "PDF generated successfully to: " + filePath;
        } catch (IOException e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }

    private PdfFont createChineseFont() throws IOException {
        String[] fontPaths = {
                "src/main/resources/static/fonts/NotoSansCJKsc-Regular.otf",
                "src/main/resources/static/fonts/simsun.ttf",
                "C:/Windows/Fonts/msyh.ttc,0",
                "C:/Windows/Fonts/simsun.ttc,0",
                "C:/Windows/Fonts/simhei.ttf"
        };

        for (String fontPath : fontPaths) {
            String pathToCheck = fontPath.contains(",")
                    ? fontPath.substring(0, fontPath.indexOf(','))
                    : fontPath;
            Path path = Paths.get(pathToCheck);
            if (Files.exists(path)) {
                return PdfFontFactory.createFont(
                        fontPath,
                        PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
                );
            }
        }

        return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
    }
}
