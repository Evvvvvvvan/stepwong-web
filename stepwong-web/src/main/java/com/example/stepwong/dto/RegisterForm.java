package com.example.stepwong.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class RegisterForm {

    @NotBlank(message = "登录账号不能为空")
    @Size(min = 3, max = 64, message = "登录账号长度需要在 3 到 64 位之间")
    private String username;

    @NotBlank(message = "登录密码不能为空")
    @Size(min = 6, max = 64, message = "登录密码长度需要在 6 到 64 位之间")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @Size(max = 64, message = "昵称不能超过 64 位")
    private String nickname;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
