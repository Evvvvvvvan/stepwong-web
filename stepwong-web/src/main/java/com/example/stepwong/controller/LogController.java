package com.example.stepwong.controller;

import com.example.stepwong.repository.ExecutionLogRepository;
import com.example.stepwong.security.CurrentUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LogController {

    private final ExecutionLogRepository executionLogRepository;
    private final CurrentUserService currentUserService;

    public LogController(ExecutionLogRepository executionLogRepository, CurrentUserService currentUserService) {
        this.executionLogRepository = executionLogRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/logs")
    public String logs(Model model) {
        model.addAttribute("logs", executionLogRepository.findTop100ByOwnerIdOrderByStartedAtDesc(currentUserService.currentUserId()));
        return "accounts/logs";
    }
}
