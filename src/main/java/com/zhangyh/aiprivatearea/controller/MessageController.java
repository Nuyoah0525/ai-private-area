package com.zhangyh.aiprivatearea.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequestMapping("/message")
@RestController
@AllArgsConstructor

public class MessageController {
  //大模型
  private final ChatModel chatModel;
  //向量数据库
  private final VectorStore vectorStore;
  
  private final ObjectMapper objectMapper;
  
  
  @GetMapping("/chat")
  public String chat(@RequestParam String question) {
    ChatClient chatClient = ChatClient.create(chatModel);
    String response = chatClient
      //提示词
      .prompt()
      // 输入用户提问内容
      .user(question)
      // call代表非流式问答，返回的结果可以是ChatResponse，也可以是Entity（转成java类型），也可以是字符串直接提取回答结果。
      .call()
      .content();
    return response;
  }
  //流式问答
  @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> chatStream(@RequestParam String question) {
    //构造历史消息，用户消息
    SystemMessage historyMemory = new SystemMessage("你是一个Java助手");
    UserMessage userMessage = new UserMessage(question);
    
    Flux<ServerSentEvent<String>> response = ChatClient.create(chatModel).prompt()
      //传入历史消息，用户消息
      .messages(historyMemory, userMessage)
      // 流式返回
      .stream()
      // 构造SSE（ServerSendEvent）格式返回结果
      .chatResponse().map(chatResponse -> ServerSentEvent.builder(toJson(chatResponse))
        .event("message")
        .build());
    return response;
  }
  
  @SneakyThrows
  public String toJson(ChatResponse chatResponse) {
    return objectMapper.writeValueAsString(chatResponse);
  }
  //调用自定义函数回答
  @GetMapping(value = "/chat/stream/function", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> chatStreamWithFunction(@RequestParam String question, @RequestParam String functionName) {
    
    UserMessage userMessage = new UserMessage(question);
    
    Flux<ServerSentEvent<String>> response = ChatClient.create(chatModel)
      .prompt()
      .messages(userMessage)
      // 根据参数functionName 寻找到对应的bean 如果需要触发则该函数则会被调用
      .functions(functionName)
      .stream()
      .chatResponse()
      .map(chatResponse -> ServerSentEvent.builder(toJson(chatResponse))
        .event("message")
        .build());
    return response;
  }
  
  //从向量数据库中查找文档作文上下文来回答
  @GetMapping(value = "/chat/stream/database", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> chatStreamWithDatabase(@RequestParam String question) {
    // question_answer_context是一个占位符，会替换成向量数据库中查询到的文档。QuestionAnswerAdvisor会替换。
    String promptWithContext = """
      下面是上下文信息
      ---------------------
      {question_answer_context}
      ---------------------
      给定的上下文和提供的历史信息，而不是事先的知识，回复用户的意见。如果答案不在上下文中，告诉用户你不能回答这个问题。且不需要额外补充信息，全部依据给定的上下文和提供的历史信息
      """;
    QuestionAnswerAdvisor questionAnswerAdvisor = new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults(), promptWithContext);
    Flux<ServerSentEvent<String>> response = ChatClient
      .create(chatModel)
      .prompt()
      .user(question)
      //从向量数据库中查找文档作文上下文
      .advisors(questionAnswerAdvisor)
      .stream()
      .content()
      .map(chatResponse -> ServerSentEvent.builder(chatResponse).event("message").build());
    return response;
  }
  
  //从本地数据库中查找历史消息作文上下文来回答
  @GetMapping(value = "/chat/stream/history", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> chatStreamWithHistory(@RequestParam String prompt, @RequestParam String sessionId) {
    //数据库存储的会话和消息 暂时使用内存存储
    ChatMemory chatMemoryDB = new InMemoryChatMemory();
    //根据会话id、数据库查找最近10条消息
    MessageChatMemoryAdvisor messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(chatMemoryDB, sessionId, 10);
    Flux<ServerSentEvent<String>> response = ChatClient.create(chatModel)
      .prompt()
      .user(prompt)
      //从本地数据库中查找历史会话作为上下文。
      .advisors(messageChatMemoryAdvisor)
      .stream()
      .content()
      .map(chatResponse -> ServerSentEvent.builder(chatResponse)
        .event("message")
        .build());
    
    
    return response;
  }
  
}
