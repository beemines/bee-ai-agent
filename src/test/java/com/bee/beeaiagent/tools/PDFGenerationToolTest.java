package com.bee.beeaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PDFGenerationToolTest {

    @Test
    public void testGeneratePDF() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "beemines.pdf";
        String content = "测试fengxue2323";
        String result = tool.generatePDF(fileName, content);
        assertNotNull(result);
    }
}
