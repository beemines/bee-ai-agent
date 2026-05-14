package com.bee.beeaiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BeeManusTest {

    @Resource
    private BeeManus beeManus;

    @Test
    void run(){
        String userPrompt = """
                1. 从搜索结果中提取合适的约会地点。
                                                2. 使用 `scrapeWebPage` 抓取相关网页，获取更详细的约会地点信息。
                                                3. 通过 `searchWeb` 寻找这些地点的网络图片，为约会计划增添视觉效果。
                                                4. 制定详细的约会计划，并使用 `generatePDF` 工具将其生成PDF格式。""";
        String result = beeManus.run(userPrompt);
        Assertions.assertNotNull( result);
    }
}