package com.nexus.cms.chat.properties;

public class ChatConstants {
    public static final String MESSAGE_SENT_TOPIC     = "chat.message.sent";
    public static final String MESSAGE_RECEIVED_TOPIC = "chat.message.received";
    public static final String MESSAGE_READ_TOPIC     = "chat.message.read";
    public static final String NOTIFICATION_TOPIC     = "chat.notification";

    public static final String PRESENCE_KEY = "presence:";
    public static final long PRESENCE_TTL_MINUTES = 30;

    public static final String TYPING_KEY = "typing:%d:%d";  // typing:conversationId:userId
    public static final long TYPING_TTL_SECONDS = 5;         // auto-expires if client crashes

}
