package com.rgbconsulting.jwt.controller;

import com.rgbconsulting.jwt.model.Jwt;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST Controller per a la gestió d'autenticació JWT amb control de permisos
 * per rol.
 *
 * Proporciona endpoints per a: - Login d'usuaris amb assignació automàtica de
 * rol (ADMIN o USER) - Refresc de tokens expirats mantenint el rol original -
 * Accés a recursos segurs per a qualsevol usuari autenticat - Accés a recursos
 * d'administració exclusius per al rol ADMIN - Accés a recursos públics sense
 * autenticació
 *
 * Els rols disponibles són: - ADMIN: accés a /jwt/secure/* i /jwt/admin/* -
 * USER: accés només a /jwt/secure/*
 *
 * Tots els tokens s'emmagatzemen a la base de dades PostgreSQL amb un temps
 * d'expiració de 30 segons.
 *
 * @author sergi
 */
@RestController
public class JwtController {

    private static String URL = "jdbc:postgresql://localhost:5432/jwt_db";
    private static String USER = "sergi";
    private static String PASS = "odoo1234";
    private static int MAXTIMEXPIRES = 30 * 1000;  // 30,000 milliseconds

    /**
     * Realitza el login d'un usuari i genera un token d'accés UUID.
     *
     * Assigna el rol automàticament segons el nom d'usuari: - "sergi" → rol
     * ADMIN - qualsevol altre → rol USER
     *
     * Emmagatzema les credencials, el token i el rol a la base de dades, i
     * retorna la informació d'autenticació al client.
     *
     * @param request Objecte Jwt amb les credencials de l'usuari (username,
     * password)
     * @return Objecte Jwt amb el token generat, temps d'expiració (30 segons),
     * nom d'usuari, timestamp de generació i rol assignat
     */
    @PostMapping("/jwt/auth/login")
    public Jwt login(@RequestBody Jwt request) {
        String role;
        // objecte de resposta
        Jwt response = new Jwt();

        // generate token
        String token = UUID.randomUUID().toString();

        // preparo la resposta
        response.setAccess_token(token);
        response.setExpires_in(MAXTIMEXPIRES);
        response.setUsername(request.getUsername());
        response.setTime_generated(System.currentTimeMillis());

        if (request.getUsername().equals("sergi")) {
            role = "ADMIN";
        } else {
            role = "USER";
        }
        response.setRole(role);

        // aqui ho ficaria dins la base de dades
        doDbLogin(request.getUsername(), request.getPassword(), token, 30, System.currentTimeMillis(), role);

        return response;
    }

    /**
     * Refresca un token d'accés existent generant-ne un de nou.
     *
     * Cerca el token antic a la base de dades, el substitueix pel nou i
     * actualitza el timestamp de generació per reiniciar el temps d'expiració.
     * El rol de l'usuari es conserva sense canvis.
     *
     * Si el token antic no existeix a la base de dades, la operació no afecta
     * cap registre i retorna el nou token igualment.
     *
     * @param request Objecte Jwt amb el token actual (access_token) a refrescar
     * @return String amb el nou token UUID generat
     */
    @PostMapping("/jwt/auth/refresh")
    public String refresh(@RequestBody Jwt request) {
        String oldToken = request.getAccess_token();
        String newToken = UUID.randomUUID().toString();

        updateToken(oldToken, newToken);

        return newToken;
    }

    /**
     * Endpoint segur accessible per a qualsevol usuari autenticat (USER o
     * ADMIN).
     *
     * Valida el token rebut a la capçalera Authorization, comprova que no hagi
     * expirat (màxim 30 segons des de la generació), i retorna confirmació amb
     * el nom d'usuari associat al token.
     *
     * @param authHeader Capçalera Authorization en format "Bearer {token}"
     * @return String de confirmació amb el nom d'usuari si el token és vàlid
     * @throws ResponseStatusException 401 UNAUTHORIZED si el token és invàlid,
     * expirat o no present
     */
    @GetMapping("/jwt/secure/ping")
    public String getSecurePing(@RequestHeader("Authorization") String authHeader) {
        String token = null;
        String username = "";

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // Quita "Bearer "
            username = getValidToken(token, System.currentTimeMillis());
            if (username != null) {
                return "ok: true, user: " + username;
            } else {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expired");
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expired");

    }

    /**
     * Endpoint públic accessible sense cap mena d'autenticació.
     *
     * Retorna un missatge de confirmació genèric sense requerir token. Útil per
     * comprovar que el servidor està operatiu.
     *
     * @return String de confirmació amb usuari desconegut
     */
    @GetMapping("/jwt/public/ping")
    public String getPublicPing() {

        return "ok: " + true + ", user: " + "unknown";
    }

    /**
     * Endpoint d'administració exclusiu per a usuaris amb rol ADMIN.
     *
     * Extreu el token de la capçalera Authorization, consulta el rol associat a
     * la base de dades i verifica que sigui ADMIN. No valida l'expiració del
     * token, només el rol.
     *
     * A diferència dels endpoints segurs, retorna el codi d'estat dins el cos
     * de la resposta en lloc de llançar una excepció HTTP.
     *
     * @param authHeader Capçalera Authorization en format "Bearer {token}"
     * @return String amb codi d'estat 202 i missatge d'èxit si el rol és ADMIN,
     * o codi d'estat 401 i missatge d'error si el rol és USER
     */
    @GetMapping("/jwt/admin/test")
    public String getAdminTest(@RequestHeader("Authorization") String authHeader) {
        // agafo el rol del usuari
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        String role = getRoleFromToken(token);

        if (role.equals("ADMIN")) {
            return "STATUS CODE: " + HttpStatus.ACCEPTED + "\nHAS ACCEDIT COM ADMIN!";
        } else {
            return "STATUS CODE: " + HttpStatus.UNAUTHORIZED + " / NO ETS ADMIN!";
        }
    }

    /*
        METODOS BASE DE DATOS
     */
    /**
     * Actualitza un token existent a la base de dades substituint-lo per un de
     * nou.
     *
     * Cerca el registre pel token antic i l'actualitza amb el nou token i el
     * timestamp actual, reiniciant així el temps d'expiració.
     *
     * @param oldToken Token actual que es vol substituir
     * @param newToken Nou token UUID que substituirà l'antic
     * @return El nou token si l'actualització ha tingut èxit, null si no s'ha
     * trobat el token antic o s'ha produït un error de base de dades
     */
    private String updateToken(String oldToken, String newToken) {
        String query = "UPDATE jwt SET acces_token = ?, time_generated = ? WHERE acces_token = ?";

        try (Connection connection = DriverManager.getConnection(URL, USER, PASS); PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, newToken);
            preparedStatement.setLong(2, System.currentTimeMillis());
            preparedStatement.setString(3, oldToken);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Token actualizado correctamente");
                return newToken;
            } else {
                System.out.println("Token no encontrado");
                return null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Insereix un nou registre de login a la base de dades.
     *
     * Emmagatzema les credencials de l'usuari, el token generat, el temps
     * d'expiració, el timestamp de generació i el rol assignat.
     *
     * @param username Nom d'usuari
     * @param password Contrasenya de l'usuari
     * @param access_token Token UUID generat per aquest login
     * @param expires_in Temps d'expiració en segons
     * @param time_generated Timestamp en mil·lisegons del moment de generació
     * @param role Rol assignat a l'usuari ("ADMIN" o "USER")
     */
    private void doDbLogin(String username, String password, String access_token, int expires_in, long time_generated, String role) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASS)) {
            String query = "INSERT INTO jwt (username, password, acces_token, expires_in, time_generated, role) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(query);

            // Setear los parámetros
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            preparedStatement.setString(3, access_token);
            preparedStatement.setInt(4, expires_in);
            preparedStatement.setLong(5, time_generated);
            preparedStatement.setString(6, role);

            // Ejecutar la consulta
            int rowsAffected = preparedStatement.executeUpdate();

            // Procesar los resultados
            if (rowsAffected > 0) {
                System.out.println("Login saved");
            }

            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Valida un token comprovant que existeix a la base de dades i no ha
     * expirat.
     *
     * Executa una consulta que filtra pel token i verifica que la diferència
     * entre el temps actual i el temps de generació sigui inferior a
     * MAXTIMEXPIRES.
     *
     * @param token Token UUID a validar
     * @param time Timestamp actual en mil·lisegons per calcular l'expiració
     * @return Nom d'usuari associat al token si és vàlid i no ha expirat, null
     * si el token no existeix o ha expirat
     */
    private String getValidToken(String token, long time) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASS)) {
            String query = "SELECT * FROM jwt WHERE acces_token = ? AND (? - time_generated) < ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            // Setear los parámetros
            preparedStatement.setString(1, token);
            preparedStatement.setLong(2, time);
            preparedStatement.setInt(3, MAXTIMEXPIRES);
            // Ejecutar la consulta
            ResultSet resultSet = preparedStatement.executeQuery();
            // Procesar los resultados
            if (resultSet.next()) {
                String user = resultSet.getString("username");
                System.out.println("User found: " + user);
                return user;
            } else {
                System.out.println("User no encontrado");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Obté el rol associat a un token de la base de dades.
     *
     * Consulta únicament el camp "role" sense verificar l'expiració del token.
     * S'utilitza per als endpoints d'administració on cal comprovar el rol
     * independentment de la validesa temporal del token.
     *
     * @param token Token UUID del qual es vol obtenir el rol
     * @return String amb el rol ("ADMIN" o "USER") si el token existeix, null
     * si no es troba o s'ha produït un error de base de dades
     */
    private String getRoleFromToken(String token) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASS)) {
            String query = "SELECT role FROM jwt WHERE acces_token = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            // Setear los parámetros
            preparedStatement.setString(1, token);
            // Ejecutar la consulta
            ResultSet resultSet = preparedStatement.executeQuery();
            // Procesar los resultados
            if (resultSet.next()) {
                String role = resultSet.getString("role");
                System.out.println("Role found: " + role);
                return role;
            } else {
                System.out.println("Role no encontrado");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
