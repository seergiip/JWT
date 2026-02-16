package com.rgbconsulting.jwt.controller;

import com.rgbconsulting.jwt.model.Jwt;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST Controller for JWT authentication management. Provides endpoints for
 * login, token refresh, and authentication verification. Maintains an in-memory
 * list of active tokens.
 *
 * @author sergi
 */
@RestController
public class JwtController {

    private List<Jwt> tokens = new ArrayList<>();

    /**
     * Performs user login and generates a JWT access token. Creates a new UUID
     * token, stores it in the active tokens list, and returns authentication
     * information including the token, expiration time, and username.
     *
     * @param request Jwt object with user credentials (username, password)
     * @return Jwt object with the generated access token, expiration time (30
     * seconds), username, and generation timestamp
     */
    @PostMapping("/jwt/auth/login")
    Jwt login(@RequestBody Jwt request) {
        // Crear objeto de respuesta
        Jwt response = new Jwt();

        // Generar token (puedes usar UUID o cualquier string)
        String token = UUID.randomUUID().toString();

        response.setAccess_token(token);
        // Establecer expiración en 900 segundos
        response.setExpires_in(30);
        response.setUsername(request.getUsername());
        response.setTime_generated(System.currentTimeMillis());

        tokens.add(response);
        // Opcionalmente puedes incluir el username en la respuesta
        // response.setUsername(request.getUsername());

        return response;
    }

    /**
     * Refreshes an existing access token by generating a new one. Searches for
     * the old token in the active tokens list, replaces it with a new one, and
     * updates the generation timestamp. If the token is invalid or doesn't
     * exist, throws an unauthorized exception.
     *
     * @param request Jwt object with the current token (access_token) to
     * refresh
     * @return Jwt object with the new access token and updated timestamp
     * @throws ResponseStatusException with 401 UNAUTHORIZED status if token is
     * invalid or expired
     */
    @PostMapping("/jwt/auth/refresh")
    Jwt refresh(@RequestBody Jwt request) {
        Jwt response = null;

        String oldToken = request.getAccess_token();
        String newToken = UUID.randomUUID().toString();

        for (Jwt j : tokens) {
            if (j.getAccess_token().equals(oldToken)) {
                long currentTime = System.currentTimeMillis();
                j.setAccess_token(newToken);
                j.setTime_generated(currentTime);

                return response = new Jwt(j);

            } else {
                tokens.remove(j);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Refresh token expired. Please login again.");
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }

    /**
     * Secure endpoint that requires JWT token authentication. Validates the
     * token received in the Authorization header, verifies it hasn't expired,
     * and returns a confirmation with the username associated with the token.
     *
     * @param authHeader Authorization header in "Bearer {token}" format
     * @return String with confirmation message and username if the token is
     * valid
     * @throws ResponseStatusException with 401 UNAUTHORIZED status if token is
     * invalid, expired, or not present
     */
    @GetMapping("/jwt/secure/ping")
    String getSecurePing(@RequestHeader("Authorization") String authHeader) {
        String token = null;
        String username = "";
        Boolean ok = false;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // Quita "Bearer "

            for (Jwt j : tokens) {
                if (j.getAccess_token().equals(token)) {
                    long currentTime = System.currentTimeMillis();
                    long elapsedSeconds = (currentTime - j.getTime_generated()) / 1000;
                    if (elapsedSeconds < j.getExpires_in()) {
                        username = j.getUsername();
                        return "ok: true, user: " + username;
                    } else {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expired");
                    }
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expired");

    }

    /**
     * Public endpoint that doesn't require authentication. Returns a generic
     * confirmation message without requiring a token.
     *
     * @return String with confirmation message and unknown user
     */
    @GetMapping("/jwt/public/ping")
    String getPublicPing() {

        return "ok: " + true + ", user: " + "unknown";
    }
}
