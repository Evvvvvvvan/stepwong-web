package com.example.stepwong.service;

import com.example.stepwong.entity.StepAccount;
import com.example.stepwong.repository.StepAccountRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StepScheduleRunner {

    private final StepAccountRepository stepAccountRepository;
    private final StepExecutionService stepExecutionService;
    private final ZoneId zoneId;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public StepScheduleRunner(
            StepAccountRepository stepAccountRepository,
            StepExecutionService stepExecutionService,
            @Value("${app.scheduler.zone-id}") String zoneId
    ) {
        this.stepAccountRepository = stepAccountRepository;
        this.stepExecutionService = stepExecutionService;
        this.zoneId = ZoneId.of(zoneId);
    }

    @Scheduled(cron = "0 * * * * *")
    public void runDueAccounts() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now(zoneId);
            LocalDate today = now.toLocalDate();
            List<StepAccount> dueAccounts = stepAccountRepository.findDueAccounts(today, now.getHour(), now.getMinute());
            for (StepAccount account : dueAccounts) {
                stepExecutionService.execute(account, "SCHEDULED", today);
            }
        } finally {
            running.set(false);
        }
    }
}
