package com.example.stepwong.controller.api;

import com.example.stepwong.dto.AccountForm;
import com.example.stepwong.entity.ExecutionLog;
import com.example.stepwong.entity.StepAccount;
import com.example.stepwong.repository.ExecutionLogRepository;
import com.example.stepwong.security.CurrentUserService;
import com.example.stepwong.service.StepAccountService;
import com.example.stepwong.service.StepExecutionService;
import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AccountApiController {

    private final StepAccountService stepAccountService;
    private final StepExecutionService stepExecutionService;
    private final ExecutionLogRepository executionLogRepository;
    private final CurrentUserService currentUserService;

    public AccountApiController(
            StepAccountService stepAccountService,
            StepExecutionService stepExecutionService,
            ExecutionLogRepository executionLogRepository,
            CurrentUserService currentUserService
    ) {
        this.stepAccountService = stepAccountService;
        this.stepExecutionService = stepExecutionService;
        this.executionLogRepository = executionLogRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/accounts")
    public List<Map<String, Object>> accounts() {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (StepAccount account : stepAccountService.listCurrentUserAccounts()) {
            result.add(toAccountResponse(account));
        }
        return result;
    }

    @PostMapping("/accounts")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody AccountForm form) {
        StepAccount account = stepAccountService.create(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(toAccountResponse(account));
    }

    @GetMapping("/accounts/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        return toAccountResponse(stepAccountService.getCurrentUserAccount(id));
    }

    @PutMapping("/accounts/{id}")
    public Map<String, Object> update(@PathVariable Long id, @Valid @RequestBody AccountForm form) {
        return toAccountResponse(stepAccountService.update(id, form));
    }

    @DeleteMapping("/accounts/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        stepAccountService.delete(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "账号已删除");
        return result;
    }

    @PostMapping("/accounts/{id}/execute")
    public Map<String, Object> execute(@PathVariable Long id) {
        return toLogResponse(stepExecutionService.executeCurrentUserAccount(id));
    }

    @PostMapping("/accounts/{id}/reveal")
    public Map<String, Object> reveal(@PathVariable Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("password", stepAccountService.revealPassword(id));
        return result;
    }

    @GetMapping("/logs")
    public List<Map<String, Object>> logs() {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (ExecutionLog log : executionLogRepository.findTop100ByOwnerIdOrderByStartedAtDesc(currentUserService.currentUserId())) {
            result.add(toLogResponse(log));
        }
        return result;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            fields.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", "参数校验失败");
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    private Map<String, Object> toAccountResponse(StepAccount account) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", account.getId());
        result.put("displayName", account.getDisplayName());
        result.put("accountNo", account.getAccountNo());
        result.put("minStep", account.getMinStep());
        result.put("maxStep", account.getMaxStep());
        result.put("autoEnabled", account.getAutoEnabled());
        result.put("runHour", account.getRunHour());
        result.put("runMinute", account.getRunMinute());
        result.put("enabled", account.getEnabled());
        result.put("lastRunDate", account.getLastRunDate());
        result.put("lastRunAt", account.getLastRunAtText());
        result.put("tokenCached", account.getEncryptedLoginToken() != null && account.getZeppUserId() != null);
        result.put("createdAt", account.getCreatedAtText());
        result.put("updatedAt", account.getUpdatedAtText());
        return result;
    }

    private Map<String, Object> toLogResponse(ExecutionLog log) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", log.getId());
        result.put("accountNo", log.getAccountNoSnapshot());
        result.put("triggerType", log.getTriggerType());
        result.put("success", log.getSuccess());
        result.put("stepCount", log.getStepCount());
        result.put("message", log.getMessage());
        result.put("startedAt", log.getStartedAtText());
        result.put("finishedAt", log.getFinishedAtText());
        return result;
    }
}
