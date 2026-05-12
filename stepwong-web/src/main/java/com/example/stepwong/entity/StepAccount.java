package com.example.stepwong.entity;

import com.example.stepwong.util.DateTimeFormatUtils;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "step_accounts",
        indexes = {
                @Index(name = "idx_step_accounts_owner", columnList = "owner_id"),
                @Index(name = "idx_step_accounts_schedule", columnList = "auto_enabled, run_hour, run_minute")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_owner_account_no", columnNames = {"owner_id", "account_no"})
        }
)
public class StepAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;

    @Column(name = "account_no", nullable = false, length = 128)
    private String accountNo;

    @Column(name = "encrypted_password", nullable = false, length = 1024)
    private String encryptedPassword;

    @Column(name = "encrypted_login_token", length = 4096)
    private String encryptedLoginToken;

    @Column(name = "encrypted_app_token", length = 4096)
    private String encryptedAppToken;

    @Column(name = "zepp_user_id", length = 128)
    private String zeppUserId;

    @Column(name = "zepp_device_id", length = 64)
    private String zeppDeviceId;

    @Column(name = "login_token_updated_at")
    private Instant loginTokenUpdatedAt;

    @Column(name = "app_token_updated_at")
    private Instant appTokenUpdatedAt;

    @Column(name = "min_step", nullable = false)
    private Integer minStep = 18000;

    @Column(name = "max_step", nullable = false)
    private Integer maxStep = 25000;

    @Column(name = "auto_enabled", nullable = false)
    private Boolean autoEnabled = false;

    @Column(name = "run_hour", nullable = false)
    private Integer runHour = 8;

    @Column(name = "run_minute", nullable = false)
    private Integer runMinute = 35;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "last_run_date")
    private LocalDate lastRunDate;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = accountNo;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = accountNo;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public String getEncryptedLoginToken() {
        return encryptedLoginToken;
    }

    public void setEncryptedLoginToken(String encryptedLoginToken) {
        this.encryptedLoginToken = encryptedLoginToken;
    }

    public String getEncryptedAppToken() {
        return encryptedAppToken;
    }

    public void setEncryptedAppToken(String encryptedAppToken) {
        this.encryptedAppToken = encryptedAppToken;
    }

    public String getZeppUserId() {
        return zeppUserId;
    }

    public void setZeppUserId(String zeppUserId) {
        this.zeppUserId = zeppUserId;
    }

    public String getZeppDeviceId() {
        return zeppDeviceId;
    }

    public void setZeppDeviceId(String zeppDeviceId) {
        this.zeppDeviceId = zeppDeviceId;
    }

    public Instant getLoginTokenUpdatedAt() {
        return loginTokenUpdatedAt;
    }

    public void setLoginTokenUpdatedAt(Instant loginTokenUpdatedAt) {
        this.loginTokenUpdatedAt = loginTokenUpdatedAt;
    }

    public Instant getAppTokenUpdatedAt() {
        return appTokenUpdatedAt;
    }

    public void setAppTokenUpdatedAt(Instant appTokenUpdatedAt) {
        this.appTokenUpdatedAt = appTokenUpdatedAt;
    }

    public Integer getMinStep() {
        return minStep;
    }

    public void setMinStep(Integer minStep) {
        this.minStep = minStep;
    }

    public Integer getMaxStep() {
        return maxStep;
    }

    public void setMaxStep(Integer maxStep) {
        this.maxStep = maxStep;
    }

    public Boolean getAutoEnabled() {
        return autoEnabled;
    }

    public void setAutoEnabled(Boolean autoEnabled) {
        this.autoEnabled = autoEnabled;
    }

    public Integer getRunHour() {
        return runHour;
    }

    public void setRunHour(Integer runHour) {
        this.runHour = runHour;
    }

    public Integer getRunMinute() {
        return runMinute;
    }

    public void setRunMinute(Integer runMinute) {
        this.runMinute = runMinute;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDate getLastRunDate() {
        return lastRunDate;
    }

    public void setLastRunDate(LocalDate lastRunDate) {
        this.lastRunDate = lastRunDate;
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(Instant lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getLastRunAtText() {
        return DateTimeFormatUtils.formatDateTime(lastRunAt);
    }

    public String getCreatedAtText() {
        return DateTimeFormatUtils.formatDateTime(createdAt);
    }

    public String getUpdatedAtText() {
        return DateTimeFormatUtils.formatDateTime(updatedAt);
    }
}
