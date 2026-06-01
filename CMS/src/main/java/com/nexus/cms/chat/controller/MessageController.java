package com.nexus.cms.chat.controller;

import com.nexus.cms.chat.model.ChatMessageRequestDto;
import com.nexus.cms.chat.service.interfaces.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.security.Principal;

@RestController
@RequestMapping("/cms/chat/v2/message")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendMessage(@RequestPart @Valid ChatMessageRequestDto message,
            @RequestPart(required = false) List<MultipartFile> files,
            Principal principal) {
        // Extract participantId from JWT principal
        Long participantId = Long.parseLong(principal.getName().split(":")[0]);
        return messageService.sendMessage(message, files, participantId);
    }

    @PostMapping(value = "/multimedia", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendMultimedia(
            @RequestPart List<MultipartFile> files,
            @RequestParam Long participantId) {
        return messageService.sendMultimediaMessage(files, participantId);
    }

    @PutMapping("/")
    public ResponseEntity<?> editMessage(@RequestBody @Valid ChatMessageRequestDto message) {
        return messageService.editMessage(message);
    }
}
