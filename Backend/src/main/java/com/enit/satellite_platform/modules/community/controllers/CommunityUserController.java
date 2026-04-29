package com.enit.satellite_platform.modules.community.controllers;

import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.lang.reflect.Field;

@RestController
@RequestMapping("/api/community")
public class CommunityUserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getCommunityUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(this::toPublicUser)
                .toList();
        return ResponseEntity.ok(users);
    }

    private Map<String, Object> toPublicUser(User user) {
        String displayName = readField(user, "name");
        String email = readField(user, "email");

        return Map.of(
                "id", user.getId(),
                "username", displayName != null ? displayName : user.getUsername(),
                "email", email != null ? email : user.getUsername(),
                "roles", user.getAuthorities() == null ? List.of() : user.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .toList(),
                "enabled", user.isEnabled()
        );
    }

    private String readField(User user, String fieldName) {
        try {
            Field field = User.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(user);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
