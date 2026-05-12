package com.example.stepwong.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class AccountForm {

    @NotBlank(message = "展示名称不能为空")
    @Size(max = 64, message = "展示名称不能超过 64 位")
    private String displayName;

    @NotBlank(message = "账号不能为空")
    @Size(max = 128, message = "账号不能超过 128 位")
    private String accountNo;

    @Size(max = 128, message = "密码不能超过 128 位")
    private String password;

    @NotNull(message = "最小步数不能为空")
    @Min(value = 1, message = "最小步数不能小于 1")
    @Max(value = 200000, message = "最小步数不能超过 200000")
    private Integer minStep = 18000;

    @NotNull(message = "最大步数不能为空")
    @Min(value = 1, message = "最大步数不能小于 1")
    @Max(value = 200000, message = "最大步数不能超过 200000")
    private Integer maxStep = 25000;

    private Boolean autoEnabled = false;

    @NotNull(message = "小时不能为空")
    @Min(value = 0, message = "小时不能小于 0")
    @Max(value = 23, message = "小时不能大于 23")
    private Integer runHour = 8;

    @NotNull(message = "分钟不能为空")
    @Min(value = 0, message = "分钟不能小于 0")
    @Max(value = 59, message = "分钟不能大于 59")
    private Integer runMinute = 35;

    private Boolean enabled = true;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}
