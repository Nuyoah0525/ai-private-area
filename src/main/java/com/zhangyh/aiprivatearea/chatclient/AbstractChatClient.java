package com.zhangyh.aiprivatearea.chatclient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

public abstract class AbstractChatClient {
  private final ChatClient client;

  //初始化所要使用的大模型
  public AbstractChatClient(ChatModel chatModel) {
    this.client = ChatClient
      .builder(chatModel)
      .build();
  }
  
  public ChatClient getChatClient() {
    return client;
  }
}
