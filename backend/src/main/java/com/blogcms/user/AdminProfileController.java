package com.blogcms.user;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/profile")
public class AdminProfileController {

    private final UserService userService;

    public AdminProfileController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/me")
    public UserResponseDto updateMyProfile(@RequestBody UserRequestDto request) {
        return userService.updateMyProfile(request);
    }
}
