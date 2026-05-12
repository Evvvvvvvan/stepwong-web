package com.example.stepwong.controller.api;

import com.example.stepwong.dto.RegisterForm;
import com.example.stepwong.entity.AppUser;
import com.example.stepwong.security.CurrentUserService;
import com.example.stepwong.service.UserService;
import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthApiController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    public AuthApiController(UserService userService, CurrentUserService currentUserService) {
        this.userService = userService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterForm form) {
        AppUser appUser = userService.register(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(toUserResponse(appUser));
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        return toUserResponse(currentUserService.currentUser());
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

    private Map<String, Object> toUserResponse(AppUser appUser) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", appUser.getId());
        result.put("username", appUser.getUsername());
        result.put("nickname", appUser.getNickname());
        result.put("enabled", appUser.getEnabled());
        result.put("createdAt", appUser.getCreatedAt());
        return result;
    }
}
