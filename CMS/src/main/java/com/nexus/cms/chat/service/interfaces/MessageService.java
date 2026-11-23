package com.nexus.cms.chat.service.interfaces;

import com.nexus.cms.chat.model.ChatMessageRequestDto;
import com.nexus.cms.chat.model.ConversationMessagesDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MessageService {
    ResponseEntity<ConversationMessagesDto> sendMessage(@Valid ChatMessageRequestDto message, List<MultipartFile> files,
            Long participantId);

    ResponseEntity<?> editMessage(@Valid ChatMessageRequestDto message);

    ResponseEntity<?> sendMultimediaMessage(List<MultipartFile> files, Long participantId);
}
