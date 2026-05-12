package com.example.stepwong.service;

import com.example.stepwong.dto.StepSubmitResult;
import com.example.stepwong.entity.ExecutionLog;
import com.example.stepwong.entity.StepAccount;
import com.example.stepwong.repository.ExecutionLogRepository;
import com.example.stepwong.repository.StepAccountRepository;
import com.example.stepwong.security.CurrentUserService;
import java.time.Instant;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StepExecutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StepExecutionService.class);

    private final StepGateway stepGateway;
    private final CryptoService cryptoService;
    private final StepAccountRepository stepAccountRepository;
    private final ExecutionLogRepository executionLogRepository;
    private final CurrentUserService currentUserService;

    public StepExecutionService(
            StepGateway stepGateway,
            CryptoService cryptoService,
            StepAccountRepository stepAccountRepository,
            ExecutionLogRepository executionLogRepository,
            CurrentUserService currentUserService
    ) {
        this.stepGateway = stepGateway;
        this.cryptoService = cryptoService;
        this.stepAccountRepository = stepAccountRepository;
        this.executionLogRepository = executionLogRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public ExecutionLog executeCurrentUserAccount(Long accountId) {
        Long ownerId = currentUserService.currentUserId();
        StepAccount account = stepAccountRepository.findWithOwnerByIdAndOwnerId(accountId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("账号不存在或无权访问"));
        LOGGER.info("手动执行步数任务，accountId={}", account.getId());
        clearTokenCache(account);
        return execute(account, "MANUAL", LocalDate.now());
    }

    @Transactional
    public ExecutionLog execute(StepAccount account, String triggerType, LocalDate runDate) {
        Instant startedAt = Instant.now();
        LOGGER.info("步数任务开始，accountId={}，triggerType={}，runDate={}", account.getId(), triggerType, runDate);
        StepSubmitResult result;
        try {
            String password = cryptoService.decrypt(account.getEncryptedPassword());
            LOGGER.info("账号密码解密完成，accountId={}", account.getId());
            result = stepGateway.submit(account, password, account.getMinStep(), account.getMaxStep());
        } catch (Exception e) {
            LOGGER.warn("步数任务异常，accountId={}，message={}", account.getId(), e.getMessage());
            result = new StepSubmitResult(false, 0, "执行异常：" + e.getMessage());
        }

        account.setLastRunDate(runDate);
        account.setLastRunAt(Instant.now());
        stepAccountRepository.save(account);

        ExecutionLog log = new ExecutionLog();
        log.setOwner(account.getOwner());
        log.setAccount(account);
        log.setAccountNoSnapshot(account.getAccountNo());
        log.setTriggerType(triggerType);
        log.setSuccess(result.isSuccess());
        log.setStepCount(result.getStepCount());
        log.setMessage(result.getMessage());
        log.setStartedAt(startedAt);
        log.setFinishedAt(Instant.now());
        ExecutionLog savedLog = executionLogRepository.save(log);
        LOGGER.info("步数任务结束，accountId={}，success={}，stepCount={}，message={}", account.getId(), result.isSuccess(), result.getStepCount(), result.getMessage());
        return savedLog;
    }

    private void clearTokenCache(StepAccount account) {
        account.setEncryptedLoginToken(null);
        account.setEncryptedAppToken(null);
        account.setZeppUserId(null);
        account.setLoginTokenUpdatedAt(null);
        account.setAppTokenUpdatedAt(null);
    }
}
