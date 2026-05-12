package com.example.stepwong.security;

import com.example.stepwong.entity.AppUser;
import com.example.stepwong.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public CurrentUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("未登录");
        }
        return appUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("登录用户不存在"));
    }

    public Long currentUserId() {
        return currentUser().getId();
    }
}
