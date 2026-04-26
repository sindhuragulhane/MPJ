package com.project.stegano;

import com.project.stegano.model.AuthRequest;
import com.project.stegano.model.AuthResponse;
import com.project.stegano.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request, HttpSession session) {
        try {
            return ResponseEntity.ok(AuthResponse.success(authService.register(request.username(), request.password(), session)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AuthResponse.failure(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request, HttpSession session) {
        try {
            return ResponseEntity.ok(AuthResponse.success(authService.login(request.username(), request.password(), session)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthResponse.failure(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(HttpSession session) {
        authService.logout(session);
        return ResponseEntity.ok(AuthResponse.success(null));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(HttpSession session) {
        try {
            return ResponseEntity.ok(AuthResponse.success(authService.currentUsername(session)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthResponse.failure(e.getMessage()));
        }
    }
}
