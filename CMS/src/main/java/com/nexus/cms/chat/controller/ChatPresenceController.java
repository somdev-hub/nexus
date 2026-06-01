package com.nexus.cms.chat.controller;

import com.nexus.cms.chat.service.interfaces.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cms/chat/v2/presence")
@RequiredArgsConstructor
public class ChatPresenceController {

    private final PresenceService presenceService;

    @GetMapping("/batch")
    public ResponseEntity<Map<Long, Boolean>> getBatchPresence(@RequestParam List<Long> userIds) {
        return ResponseEntity.ok(presenceService.getOnlineStatuses(userIds));
    }
}
