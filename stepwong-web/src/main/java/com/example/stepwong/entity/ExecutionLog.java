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
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(
        name = "execution_logs",
        indexes = {
                @Index(name = "idx_execution_logs_owner", columnList = "owner_id"),
                @Index(name = "idx_execution_logs_account", columnList = "account_id"),
                @Index(name = "idx_execution_logs_started_at", columnList = "started_at")
        }
)
public class ExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private StepAccount account;

    @Column(name = "account_no_snapshot", nullable = false, length = 128)
    private String accountNoSnapshot;

    @Column(name = "trigger_type", nullable = false, length = 32)
    private String triggerType;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "step_count")
    private Integer stepCount;

    @Column(name = "message", nullable = false, length = 2048)
    private String message;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "finished_at", nullable = false)
    private Instant finishedAt;

    @PrePersist
    public void prePersist() {
        if (startedAt == null) {
            startedAt = Instant.now();
        }
        if (finishedAt == null) {
            finishedAt = Instant.now();
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

    public StepAccount getAccount() {
        return account;
    }

    public void setAccount(StepAccount account) {
        this.account = account;
    }

    public String getAccountNoSnapshot() {
        return accountNoSnapshot;
    }

    public void setAccountNoSnapshot(String accountNoSnapshot) {
        this.accountNoSnapshot = accountNoSnapshot;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Integer getStepCount() {
        return stepCount;
    }

    public void setStepCount(Integer stepCount) {
        this.stepCount = stepCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getStartedAtText() {
        return DateTimeFormatUtils.formatDateTime(startedAt);
    }

    public String getFinishedAtText() {
        return DateTimeFormatUtils.formatDateTime(finishedAt);
    }
}
