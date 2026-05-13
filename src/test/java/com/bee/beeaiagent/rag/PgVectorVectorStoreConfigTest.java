package com.bee.beeaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PgVectorVectorStoreConfigTest {

    @Resource
    VectorStore pgVectorVectorStore;

    @Test
    void test() {
        List<Document> documents = List.of(
                new Document("bee是帅小伙", Map.of("meta1", "meta1")),
                new Document("bee写了很多agent项目，AI智能云图库，超级智能体"),
                new Document("bee可以教你学习编程，后端agent", Map.of("meta2", "meta2")));
        // 添加文档
        pgVectorVectorStore.add(documents);
        // 相似度查询
        List<Document> results = pgVectorVectorStore
                .similaritySearch(SearchRequest.builder()
                        .query("怎么学习编程？")
                        .topK(3)
                        .build());
        Assertions.assertNotNull(results);
    }
}
