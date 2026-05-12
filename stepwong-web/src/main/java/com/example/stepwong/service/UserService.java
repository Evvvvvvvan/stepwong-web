package com.example.stepwong.service;

import com.example.stepwong.dto.RegisterForm;
import com.example.stepwong.entity.AppUser;
import com.example.stepwong.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser register(RegisterForm form) {
        String username = form.getUsername().trim();
        if (appUserRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("登录账号已存在");
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        AppUser appUser = new AppUser();
        appUser.setUsername(username);
        appUser.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        appUser.setNickname(normalizeNickname(form.getNickname(), username));
        appUser.setEnabled(true);
        return appUserRepository.save(appUser);
    }

    private String normalizeNickname(String nickname, String username) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return username;
        }
        return nickname.trim();
    }
}
