package com.blogcms.user;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponseDto> getUsers() {
        return userService.getUsers();
    }

    @GetMapping("/reporters")
    public List<WriterOptionDto> getReporters() {
        return userService.getActiveWriters();
    }

    @PostMapping
    public UserResponseDto createUser(@RequestBody UserRequestDto request) {
        return userService.createUser(request);
    }

    @PatchMapping("/{id}/role")
    public UserResponseDto updateRole(@PathVariable Long id, @RequestBody Map<String, String> request) {
        return userService.updateRole(id, request);
    }

    @PatchMapping("/{id}/status")
    public UserResponseDto updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        return userService.updateStatus(id, request);
    }

    @PatchMapping("/{id}/profile")
    public UserResponseDto updateProfile(@PathVariable Long id, @RequestBody UserRequestDto request) {
        return userService.updateProfile(id, request);
    }
}
