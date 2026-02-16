package com.rgbconsulting.jwt.restClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Scanner;

/**
 * REST Client for JWT authentication management.
 *
 * This class provides functionalities for: - Performing login and obtaining a
 * JWT token - Making secure requests using the token - Refreshing the token
 * when it expires
 *
 * @author sergi
 */
public class jwtRestClient {

    private static String token;
    private static String username = "sergi";
    private static String password = "pepitodelospalotes1234";

    /**
     * Main method that displays an interactive menu for managing JWT
     * operations.
     *
     * Available options: - 0: Exit the application - 1: Perform login and
     * obtain token - 2: Perform secure request (ping)
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        int option;
        Scanner key = new Scanner(System.in);

        do {
            System.out.println("----------------- JWT -----------------");
            System.out.println("0. Sortir");
            System.out.println("1. Fer login");
            System.out.println("2. Fer secure ping");
            System.out.print("Option: ");
            option = key.nextInt();
            switch (option) {
                case 0:
                    System.out.println("Sortint...");
                    break;
                case 1:
                    System.out.println("Fent el login");
                    login();
                    break;
                case 2:
                    System.out.println("Fent secure ping");
                    petitionSecurePing();
                    break;
                default:
                    System.out.println("Error. Opcio no valida");
                    break;
            }
        } while (option != 0);
    }

    /**
     * Performs the login process by sending credentials to the server.
     *
     * This method: - Creates a POST request with credentials in JSON format -
     * Sends the request to the /jwt/auth/login endpoint - Extracts and stores
     * the JWT token from the response - Displays the status code and response
     * body
     *
     * The obtained token is stored in the static 'token' variable for use in
     * subsequent requests.
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
     * Performs a secure request to the /secure/ping endpoint.
     *
     * This method: - Sends a GET request to the secure endpoint - Includes the
     * JWT token in the Authorization header - Verifies the response code - If
     * it receives a 401 (unauthorized), automatically refreshes the token and
     * retries the request
     *
     * Requires that login has been previously performed and a valid token
     * obtained.
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
     * Refreshes the JWT token when it has expired.
     *
     * This method: - Sends a POST request to the /jwt/auth/refresh endpoint -
     * Includes the current token in the request body - Extracts and stores the
     * new token from the response - Displays the status code and response body
     *
     * It is automatically invoked when a secure request receives a 401
     * (unauthorized) status code.
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

        getToken(response);
    }

    /**
     * Extracts the JWT token from the HTTP response body.
     *
     * This method parses the JSON response assuming a specific format where the
     * token is located in the fourth comma-separated field. Removes quotes and
     * whitespace from the extracted token.
     *
     * @param response the HTTP response containing the token in its body in
     * JSON format
     *
     * @throws NullPointerException if the response is null
     * @throws ArrayIndexOutOfBoundsException if the response format does not
     * match the expected format
     */
    private static void getToken(HttpResponse<String> response) {
        String resposta[] = response.body().split(",");
        String tok[] = resposta[3].split(":");
        token = tok[1].replace("\"", "").strip();
    }
}
