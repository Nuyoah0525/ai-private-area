package com.zhangyh.aiprivatearea.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.context.annotation.Description;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.util.function.Function;

/**
 * 通过@Description描述函数的用途，ai会在多个函数中自动根据描述进行选择使用。
 */
@Description("文档解析函数")
@Service
@Slf4j
public class DocumentAnalyzerFunction implements Function<DocumentAnalyzerFunction.Input, DocumentAnalyzerFunction.Result> {
  /**
   * 通过@JsonProperty  属性名称 是否必填
   * 通过@JsonPropertyDescription描述 便于ai自动传入符合参数的内容。
   */
  @Data
  public static class Input {
    @JsonProperty(required = true, value = "path")
    @JsonPropertyDescription(value = "需要解析的本地文件路径")
    String path;
  }
  @AllArgsConstructor
  @Data
  @ToString
  public static class Result {
    private String result;
  }
  

  @Override
  public Result apply(Input input) {
    // ai解析用户的提问得到path参数，使用tika读取本地文件获取内容。把读取到的内容再返回给ai作为上下文去回答用户的问题。
    TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(new FileSystemResource(input.path));
    return new Result(tikaDocumentReader.read().get(0).getContent());
  }
}
