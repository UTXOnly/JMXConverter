package org.example;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimpleJettyApp {
    // Shared list to store all generated random numbers
    private static final List<Integer> randomNumbers = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        // Create a basic Jetty server on port 8080
        Server server = new Server(8080);

        // Set up the servlet context handler
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Add servlets for different API paths
        context.addServlet(new ServletHolder(new RandomNumberServlet()), "/api/random-number");
        context.addServlet(new ServletHolder(new StoreRandomNumberServlet()), "/api/store-random-number");
        context.addServlet(new ServletHolder(new AllRandomNumberServlet()), "/api/all-random-number");

        // Start the server
        server.start();
        server.join();
    }

    // Servlet to generate a random number and store it by calling /api/store-random-number
    public static class RandomNumberServlet extends HttpServlet {
        private final Random random = new Random();

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            int randomNumber = random.nextInt(100) + 1;

            // Call /api/store-random-number to store the random number
            URL url = new URL("http://localhost:8080/api/store-random-number");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            // Send the random number in JSON format
            String jsonInputString = "{ \"randomNumber\": " + randomNumber + " }";
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Read the response from /api/store-random-number
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                resp.setContentType("application/json");
                resp.getWriter().println("{ \"randomNumber\": " + randomNumber + " }");
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to store random number");
            }
        }
    }

    // Servlet to store the random number and respond with the list of stored numbers
    public static class StoreRandomNumberServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream(), "utf-8"));
            StringBuilder jsonInput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonInput.append(line);
            }

            // Extract the random number from the JSON input and store it
            String json = jsonInput.toString();
            if (json.contains("\"randomNumber\":")) {
                int randomNumber = Integer.parseInt(json.replaceAll("[^0-9]", ""));
                synchronized (randomNumbers) {
                    randomNumbers.add(randomNumber);
                }
            }

            resp.setContentType("application/json");
            resp.getWriter().println("{ \"status\": \"success\" }");
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Return the list of all stored random numbers as JSON
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{ \"allRandomNumbers\": [");

            synchronized (randomNumbers) {
                for (int i = 0; i < randomNumbers.size(); i++) {
                    jsonBuilder.append(randomNumbers.get(i));
                    if (i < randomNumbers.size() - 1) {
                        jsonBuilder.append(", ");
                    }
                }
            }

            jsonBuilder.append("] }");
            resp.setContentType("application/json");
            resp.getWriter().println(jsonBuilder.toString());
        }
    }

    // Servlet to retrieve all stored random numbers by calling /api/store-random-number
    public static class AllRandomNumberServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Call /api/store-random-number to get the list of all random numbers
            URL url = new URL("http://localhost:8080/api/store-random-number");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Read the response from /api/store-random-number
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();

                // Respond with the output from /api/store-random-number
                resp.setContentType("application/json");
                resp.getWriter().println(content.toString());
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve stored random numbers");
            }
        }
    }
}
