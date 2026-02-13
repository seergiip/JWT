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
 *
 * @author sergi
 */
@RestController
public class JwtController {

    private List<Jwt> tokens = new ArrayList<>();

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

    @PostMapping("/jwt/auth/refresh")
    Jwt refresh(@RequestBody Jwt request) {
        Jwt response = null;
        // Generar token 
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

    @GetMapping("/jwt/secure/ping")
    String getSecurePing(@RequestHeader("Authorization") String authHeader) {
        String token = null;
        String username = "";
        Boolean ok = false;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // Quita "Bearer "

            // VALIDAR el token
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

        // Crear respuesta
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expired");

    }

    @GetMapping("/jwt/public/ping")
    String getPublicPing() {

        return "ok: " + true + ", user: " + "unknown";
    }
}
