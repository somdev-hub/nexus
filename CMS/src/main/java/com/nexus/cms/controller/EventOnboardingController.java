package com.nexus.cms.controller;

import com.nexus.cms.model.entities.EventTemplate;
import com.nexus.cms.model.entities.TemplateParam;
import com.nexus.cms.payload.MailTriggerDto;
import com.nexus.cms.service.EventOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cms/event-onboarding")
@RequiredArgsConstructor
public class EventOnboardingController {

    private final EventOnboardingService eventOnboardingService;

    @PostMapping("/template")
    public ResponseEntity<?> addEventTemplate(@RequestBody EventTemplate eventTemplate) {
        return eventOnboardingService.addEventTemplate(eventTemplate);
    }

    @PutMapping("/template")
    public ResponseEntity<?> updateEventTemplate(@RequestBody EventTemplate eventTemplate, @RequestParam(required = false) Boolean templateUpdate) {
        return eventOnboardingService.updateEventTemplate(eventTemplate, templateUpdate);
    }

    @PostMapping("/template/params")
    public ResponseEntity<?> addTemplateParams(@RequestBody List<TemplateParam> templateParams, @RequestParam Long eventTemplateId) {
        return eventOnboardingService.addTemplateParams(templateParams, eventTemplateId);
    }

    @PutMapping("/template/params")
    public ResponseEntity<?> updateTemplateParams(@RequestBody List<TemplateParam> templateParams, @RequestParam Long eventTemplateId) {
        return eventOnboardingService.updateTemplateParams(templateParams, eventTemplateId);
    }

    @GetMapping("/template")
    public ResponseEntity<?> getEventTemplates(@RequestParam Long orgId){
        return eventOnboardingService.getEventTemplates(orgId);
    }

    @GetMapping("/template/name")
    public ResponseEntity<?> getEventTemplateByName(@RequestParam String templateName, @RequestParam Long orgId) {
        return eventOnboardingService.getEventTemplateByName(templateName, orgId);
    }

    @GetMapping("/template/{eventTemplateId}")
    public ResponseEntity<?> getEventTemplateById(@PathVariable Long eventTemplateId) {
        return eventOnboardingService.getEventTemplateById(eventTemplateId);
    }

    @PostMapping("/trigger")
    public ResponseEntity<?> triggerMail(@RequestBody MailTriggerDto mailTriggerDto) {
        return eventOnboardingService.triggerMail(mailTriggerDto);
    }
}
