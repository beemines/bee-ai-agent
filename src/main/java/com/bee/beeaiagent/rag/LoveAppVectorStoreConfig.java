package com.bee.beeaiagent.rag;

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeyWordEnricher myKeyWordEnricher;

    @Bean
    VectorStore loveAppVectorStore(DashScopeEmbeddingModel dashscopeEmbeddingModel){
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        //加载文档
        List<Document> documentList = loveAppDocumentLoader.loadMarkdowns();
        //自主切分文档
        //List<Document> splitDocuments = myTokenTextSplitter.splitCustomized(documents);
        List<Document> enrichDocuments = myKeyWordEnricher.enrichDocuments(documentList);
        simpleVectorStore.add(enrichDocuments);
        return simpleVectorStore;
    }


}
