package com.classync.controller;

import com.classync.dto.LoginRequest;
import com.classync.entity.User;
import com.classync.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    
    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody LoginRequest request) {
        User user = userService.login(request.getAccount(), request.getName());
        return ResponseEntity.ok(user);
    }
}

