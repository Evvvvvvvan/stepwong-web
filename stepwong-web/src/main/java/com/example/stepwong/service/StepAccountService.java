package com.example.stepwong.service;

import com.example.stepwong.dto.AccountForm;
import com.example.stepwong.entity.AppUser;
import com.example.stepwong.entity.StepAccount;
import com.example.stepwong.repository.StepAccountRepository;
import com.example.stepwong.security.CurrentUserService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StepAccountService {

    private final StepAccountRepository stepAccountRepository;
    private final CurrentUserService currentUserService;
    private final CryptoService cryptoService;

    public StepAccountService(
            StepAccountRepository stepAccountRepository,
            CurrentUserService currentUserService,
            CryptoService cryptoService
    ) {
        this.stepAccountRepository = stepAccountRepository;
        this.currentUserService = currentUserService;
        this.cryptoService = cryptoService;
    }

    @Transactional(readOnly = true)
    public List<StepAccount> listCurrentUserAccounts() {
        return stepAccountRepository.findByOwnerIdOrderByCreatedAtDesc(currentUserService.currentUserId());
    }

    @Transactional(readOnly = true)
    public StepAccount getCurrentUserAccount(Long id) {
        return stepAccountRepository.findByIdAndOwnerId(id, currentUserService.currentUserId())
                .orElseThrow(() -> new IllegalArgumentException("账号不存在或无权访问"));
    }

    @Transactional
    public StepAccount create(AccountForm form) {
        AppUser owner = currentUserService.currentUser();
        validateSteps(form);
        String accountNo = form.getAccountNo().trim();
        if (stepAccountRepository.existsByOwnerIdAndAccountNo(owner.getId(), accountNo)) {
            throw new IllegalArgumentException("当前登录用户下已存在相同账号");
        }
        if (form.getPassword() == null || form.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("新增账号时密码不能为空");
        }
        StepAccount account = new StepAccount();
        account.setOwner(owner);
        copyFormToAccount(form, account, true);
        return stepAccountRepository.save(account);
    }

    @Transactional
    public StepAccount update(Long id, AccountForm form) {
        StepAccount account = getCurrentUserAccount(id);
        validateSteps(form);
        String oldAccountNo = account.getAccountNo();
        String newAccountNo = form.getAccountNo().trim();
        boolean accountChanged = !oldAccountNo.equals(newAccountNo);
        boolean passwordChanged = form.getPassword() != null && !form.getPassword().trim().isEmpty();
        if (accountChanged
                && stepAccountRepository.existsByOwnerIdAndAccountNo(account.getOwner().getId(), newAccountNo)) {
            throw new IllegalArgumentException("当前登录用户下已存在相同账号");
        }
        copyFormToAccount(form, account, false);
        if (accountChanged || passwordChanged) {
            clearTokenCache(account);
        }
        return stepAccountRepository.save(account);
    }

    @Transactional
    public void delete(Long id) {
        StepAccount account = getCurrentUserAccount(id);
        stepAccountRepository.delete(account);
    }

    @Transactional(readOnly = true)
    public String revealPassword(Long id) {
        StepAccount account = getCurrentUserAccount(id);
        return cryptoService.decrypt(account.getEncryptedPassword());
    }

    public AccountForm toForm(StepAccount account) {
        AccountForm form = new AccountForm();
        form.setDisplayName(account.getDisplayName());
        form.setAccountNo(account.getAccountNo());
        form.setMinStep(account.getMinStep());
        form.setMaxStep(account.getMaxStep());
        form.setAutoEnabled(account.getAutoEnabled());
        form.setRunHour(account.getRunHour());
        form.setRunMinute(account.getRunMinute());
        form.setEnabled(account.getEnabled());
        return form;
    }

    public String maskPassword() {
        return "********";
    }

    private void copyFormToAccount(AccountForm form, StepAccount account, boolean create) {
        account.setDisplayName(form.getDisplayName().trim());
        account.setAccountNo(form.getAccountNo().trim());
        if (create || (form.getPassword() != null && !form.getPassword().trim().isEmpty())) {
            account.setEncryptedPassword(cryptoService.encrypt(form.getPassword()));
        }
        account.setMinStep(form.getMinStep());
        account.setMaxStep(form.getMaxStep());
        account.setAutoEnabled(Boolean.TRUE.equals(form.getAutoEnabled()));
        account.setRunHour(form.getRunHour());
        account.setRunMinute(form.getRunMinute());
        account.setEnabled(Boolean.TRUE.equals(form.getEnabled()));
    }

    private void clearTokenCache(StepAccount account) {
        account.setEncryptedLoginToken(null);
        account.setEncryptedAppToken(null);
        account.setZeppUserId(null);
        account.setLoginTokenUpdatedAt(null);
        account.setAppTokenUpdatedAt(null);
    }

    private void validateSteps(AccountForm form) {
        if (form.getMinStep() != null && form.getMaxStep() != null && form.getMinStep() > form.getMaxStep()) {
            throw new IllegalArgumentException("最小步数不能大于最大步数");
        }
    }
}
