package com.wildbeyond.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/login")
    public void login(/* @RequestBody LoginRequest loginRequest */) {
        // ...method signature only
    }

    @PostMapping("/register")
    public void register(/* @RequestBody RegisterRequest registerRequest */) {
        // ...method signature only
    }
}