package com.wildbeyond.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("")
    public void getAllUsers() {
        // ...method signature only
    }

    @GetMapping("/{id}")
    public void getUserById(@PathVariable Long id) {
        // ...method signature only
    }

    @PostMapping("")
    public void createUser(/* @RequestBody UserDTO userDTO */) {
        // ...method signature only
    }

    @PutMapping("/{id}")
    public void updateUser(@PathVariable Long id/*, @RequestBody UserDTO userDTO */) {
        // ...method signature only
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        // ...method signature only
    }
}