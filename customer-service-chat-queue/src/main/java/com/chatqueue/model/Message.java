package com.chatqueue.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="messages")
@Getter @Setter
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="message_id")
    private Integer messageId;

    @Column(name="chat_id")
    private Integer chatId;

    @Column(name="sender_id")
    private UUID senderId;

    @Column(name="message_text")
    private String messageText;

    @Column(name="message_type")
    private String messageType = "text";

    @Column(name="timestamp")
    private Instant timestamp = Instant.now();
}
