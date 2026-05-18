package com.nexus.cms.chat.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.nexus.cms.chat.enums.AttachmentType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Data
@Entity
@Table(name = "t_chat_message_attachment", schema = "cms")
public class ChatMessageAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatMessageAttachmentId;

    private String fileName;

    private String dmsId;

    private String filePath;

    @Enumerated(EnumType.STRING)
    private AttachmentType attachmentType;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    private Boolean isActive;

    @PrePersist
    public void prePersist() {
        this.isActive = true;
    }

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonBackReference("message-attachments")
    @ManyToOne
    @JoinColumn(name = "chat_message_chat_message_id")
    private ChatMessage chatMessage;
}