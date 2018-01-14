package com.livemic.livemicapp;

import java.util.Collection;

/** Log for text version of chat - recent messages, plus the current one being spoken. */
public interface TextChatLog {
  void handleChatText(String currentMessage, Collection<String> previousMessages);
}
