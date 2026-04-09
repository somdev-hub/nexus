package com.nexus.hr.controller;

import com.nexus.hr.exception.UnauthorizedException;
import com.nexus.hr.model.entities.WOWOConfig;
import com.nexus.hr.service.interfaces.WOWOConfigService;
import com.nexus.hr.utils.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hr/wowo")
public class WOWOController {
    private final WOWOConfigService wowoConfigService;
    private final CommonUtils commonUtils;

    @PostMapping("/")
    public ResponseEntity<?> addWowoConfig(@RequestBody WOWOConfig wowoConfig, @RequestHeader("Authorization") String auth) {
        if (ObjectUtils.isEmpty(auth) || !commonUtils.validateToken(auth)) {
            throw new UnauthorizedException(
                    "Unauthorized: Invalid or missing token",
                    "Unauthorized"
            );
        }
        return wowoConfigService.addWOWOConfig(wowoConfig);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getWowoConfig(@PathVariable Long id, @RequestHeader("Authorization") String auth) {
        if (ObjectUtils.isEmpty(auth) || !commonUtils.validateToken(auth)) {
            throw new UnauthorizedException(
                    "Unauthorized: Invalid or missing token",
                    "Unauthorized"
            );
        }

        return wowoConfigService.getWOWOConfig(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> toggleWowoConfig(@PathVariable Long id, @RequestHeader("Authorization") String auth) {
        if (ObjectUtils.isEmpty(auth) || !commonUtils.validateToken(auth)) {
            throw new UnauthorizedException(
                    "Unauthorized: Invalid or missing token",
                    "Unauthorized"
            );
        }
        return wowoConfigService.toggleWOWOConfig(id);
    }
}
