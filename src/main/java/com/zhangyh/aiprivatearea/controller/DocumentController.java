package com.zhangyh.aiprivatearea.controller;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RequestMapping("/document")
@RestController
@AllArgsConstructor
@Slf4j
public class DocumentController {
  private final VectorStore vectorStore;
  
  //将文件内容嵌入向量数据库

  @PostMapping("/embedding")
  public Boolean embedding(@RequestParam MultipartFile file) throws IOException {
    // 从IO流中读取文件
    TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(new InputStreamResource(file.getInputStream()));
    // 使用ai文档切割器TokenTextSplitter 将文本内容划分块
    List<Document> splitDocuments = new TokenTextSplitter().apply(tikaDocumentReader.read());
    // 调用嵌入模型embeddingModel后存入向量数据库
    vectorStore.add(splitDocuments);
    
    return true;
  }
  
  //根据用户提问 查询向量数据库匹配到的文档数据
  @GetMapping("/query")
  public List<Document> query(@RequestParam String query) {
    return vectorStore.similaritySearch(query);
  }
}
