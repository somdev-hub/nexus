package com.nexus.cms.chat.model;

import com.nexus.cms.chat.enums.ChatParticipantStatus;
import com.nexus.cms.chat.enums.ChatParticipantType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

@Data
public class ChatConversationParticipantDto {

    @NotNull
    private Long participantId;

    @NotNull
    private String participantName;

    @NotNull
    @Email
    private String participantEmail;

    @NotNull
    @Length(min = 10, max = 12)
    private String participantMob;

    @NotNull
    private String participantRole;

    private Boolean isChatCreator;

    @URL
    private String participantAvatar;

    @NotNull
    private ChatParticipantType chatParticipantType;

    private ChatParticipantStatus chatParticipantStatus;
}
