package com.flightapp.controller;

import com.flightapp.model.User;
import com.flightapp.service.AuthService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public Mono<String> register(@RequestBody User user) {
		return authService.register(user).map(savedUser -> "user created with id: " + savedUser.getId());
	}

	@PostMapping("/login")
	public Mono<ResponseEntity<Map<String, String>>> login(@RequestBody User user) {
		return authService.login(user.getEmail(), user.getPassword()).map(
				sessionId -> ResponseEntity.ok().body(Map.of("sessionId", sessionId, "message", "Login successful")))
				.onErrorResume(ex -> Mono
						.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()))));
	}

}
