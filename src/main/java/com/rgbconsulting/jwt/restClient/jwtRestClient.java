package com.rgbconsulting.jwt.restClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Scanner;

/**
 * Client REST per a la gestió d'autenticació JWT amb control de permisos per
 * rol.
 *
 * Proporciona les següents funcionalitats: - Login d'usuari i obtenció del
 * token d'accés - Peticions segures a endpoints autenticats - Refresc automàtic
 * del token quan expira - Peticions a endpoints exclusius d'administrador
 *
 * El menú interactiu requereix fer login (opció 1) abans de poder accedir a les
 * opcions 2 i 3. L'usuari configurat determina el rol assignat pel servidor
 * (ADMIN o USER).
 *
 * @author sergi
 */
public class jwtRestClient {

    private static String token = "";
    private static String username = "manelet";
    private static String password = "pepitodelospalotes1234";
    private static boolean logInDone = false;

    /**
     * Mètode principal que mostra un menú interactiu per gestionar les
     * operacions JWT.
     *
     * Opcions disponibles: - 0: Sortir de l'aplicació - 1: Fer login i obtenir
     * el token - 2: Fer petició segura (requereix login previ) - 3: Fer test
     * d'administrador (requereix login previ)
     *
     * Les opcions 2 i 3 mostren un missatge d'error si es criden sense haver
     * fet login.
     *
     * @param args arguments de línia de comandes (no s'utilitzen)
     */
    public static void main(String[] args) {
        int option;
        Scanner key = new Scanner(System.in);

        do {
            System.out.println("----------------- JWT -----------------");
            System.out.println("0. Sortir");
            System.out.println("1. Fer login");
            System.out.println("2. Fer secure ping");
            System.out.println("3. Fer admin test");
            System.out.print("Option: ");
            option = key.nextInt();
            switch (option) {
                case 0:
                    System.out.println("Sortint...");
                    break;
                case 1:
                    System.out.println("Fent el login");
                    login();
                    logInDone = true;
                    break;
                case 2:
                    if (logInDone) {
                        System.out.println("Fent secure ping");
                        petitionSecurePing();
                    } else {
                        System.out.println("Has de fer el login primer!");
                    }
                    break;
                case 3:
                    if (logInDone) {
                        System.err.println("Fent el admin test");
                        getAdminTest();
                    } else {
                        System.out.println("Has de fer el login primer!");
                    }
                    break;
                default:
                    System.out.println("Error. Opcio no valida");
                    break;
            }
        } while (option != 0);
    }

    /**
     * Realitza el procés de login enviant les credencials al servidor.
     *
     * Envia una petició POST a /jwt/auth/login amb les credencials en format
     * JSON. Si la petició té èxit, extreu i emmagatzema el token UUID de la
     * resposta mitjançant el mètode getToken(). Mostra el codi d'estat, el cos
     * de la resposta i el token extret per consola.
     *
     * El token obtingut s'emmagatzema a la variable estàtica 'token' per ser
     * utilitzat en les peticions posteriors.
     */
    private static void login() {
        HttpRequest request = null;
        HttpResponse<String> response = null;

        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();

            //POST
            try {
                String jsonBody = "{\n"
                        + "  \"username\": \"" + username + "\",\n"
                        + "  \"password\": \"" + password + "\"\n"
                        + "}";

                request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:8080/jwt/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
            } catch (URISyntaxException u) {
                u.printStackTrace();
            }

            if (request != null) {
                try {
                    response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("POST Status Code: " + response.statusCode());
                    System.out.println("POST Response Body: " + response.body());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Error: La solicitud no ha pogut ser creada degut a un problema amb la URI.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        getToken(response);

        System.out.println(token);
    }

    /**
     * Realitza una petició autenticada a l'endpoint /jwt/secure/ping.
     *
     * Envia una petició GET incloent el token actual a la capçalera
     * Authorization en format "Bearer {token}". Si rep un 401 (token expirat),
     * refresca el token automàticament cridant authRefresh() i reintenta la
     * petició.
     *
     * Requereix que s'hagi fet login prèviament i que hi hagi un token vàlid.
     */
    private static void petitionSecurePing() {
        HttpRequest request = null;
        HttpResponse<String> response = null;
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        //GET
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/jwt/secure/ping"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
        } catch (URISyntaxException u) {
            u.printStackTrace();
        }

        if (request != null) {
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("GET Status Code: " + response.statusCode());
                //System.out.println("GET Response Body: " + response.body());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Error: La solicitud no ha pogut ser creada degut a un problema amb la URI.");
        }

        if (response.statusCode() == 401) {
            authRefresh();
            petitionSecurePing();
        }
    }

    /**
     * Refresca el token JWT quan ha expirat.
     *
     * Envia una petició POST a /jwt/auth/refresh amb el token actual al cos en
     * format JSON. El servidor retorna un nou token UUID que substitueix
     * l'anterior a la variable estàtica 'token'. El rol de l'usuari es conserva
     * sense canvis al servidor.
     *
     * S'invoca automàticament quan petitionSecurePing() rep un codi 401.
     */
    private static void authRefresh() {
        HttpRequest request = null;
        HttpResponse<String> response = null;

        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();

            //POST
            try {
                String jsonBody = "{\n"
                        + "  \"access_token\": \"" + token + "\"\n"
                        + "}";

                request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:8080/jwt/auth/refresh"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
            } catch (URISyntaxException u) {
                u.printStackTrace();
            }

            if (request != null) {
                try {
                    response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("POST Status Code: " + response.statusCode());
                    System.out.println("POST Response Body: " + response.body());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Error: La solicitud no ha pogut ser creada degut a un problema amb la URI.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        //getToken(response);
        token = response.body();
        System.out.println(token.toString());
    }

    /**
     * Realitza una petició a l'endpoint exclusiu d'administrador
     * /jwt/admin/test.
     *
     * Envia una petició GET incloent el token actual a la capçalera
     * Authorization. El servidor verifica que el rol associat al token sigui
     * ADMIN i retorna el resultat dins el cos de la resposta (no llança
     * excepcions HTTP).
     *
     * Mostra per consola el codi d'estat i el cos de la resposta, que indicarà
     * si l'accés ha estat concedit o denegat segons el rol de l'usuari.
     *
     * Requereix que s'hagi fet login prèviament.
     */
    private static void getAdminTest() {
        HttpRequest request = null;
        HttpResponse<String> response = null;
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        //GET
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/jwt/admin/test"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
        } catch (URISyntaxException u) {
            u.printStackTrace();
        }

        if (request != null) {
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("GET Status Code: " + response.statusCode());
                System.out.println("GET Response Body: " + response.body());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Error: La solicitud no ha pogut ser creada degut a un problema amb la URI.");
        }
    }

    /**
     * Extreu el token JWT del cos de la resposta HTTP del login.
     *
     * Analitza la resposta JSON separant per comes i agafant el tercer camp
     * (índex 2), que correspon a "access_token". Després separa per ":" i
     * neteja les cometes i espais del valor obtingut.
     *
     * Format esperat de la resposta:
     * {"username":"...","password":null,"access_token":"uuid","expires_in":...}
     *
     * @param response Resposta HTTP del login que conté el token en format JSON
     * @throws NullPointerException si la resposta és null
     * @throws ArrayIndexOutOfBoundsException si el format de la resposta no és
     * l'esperat
     */
    private static void getToken(HttpResponse<String> response) {
        String resposta[] = response.body().split(",");
        String tok[] = resposta[2].split(":");
        token = tok[1].replace("\"", "").strip();
    }
}
