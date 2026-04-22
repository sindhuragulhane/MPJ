package com.project.stegano;

import com.project.stegano.model.AuthRequest;
import com.project.stegano.model.AuthResponse;
import com.project.stegano.service.AuthService;
import com.project.stegano.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final RateLimiterService rateLimiterService;

    public AuthController(AuthService authService, RateLimiterService rateLimiterService) {
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        try {
            rateLimiterService.checkLimit("auth-register", httpRequest.getRemoteAddr(), 10, 300);
            String username = authService.register(request.username(), request.password());
            return ResponseEntity.ok(AuthResponse.success(username));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AuthResponse.failure(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request, HttpSession session, HttpServletRequest httpRequest) {
        try {
            rateLimiterService.checkLimit("auth-login", httpRequest.getRemoteAddr(), 15, 300);
            String username = authService.login(request.username(), request.password(), session);
            return ResponseEntity.ok(AuthResponse.success(username));
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
