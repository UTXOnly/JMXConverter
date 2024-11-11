import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class JsonServer {

    public static void main(String[] args) throws IOException {
        // Create an HTTP server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Set a default handler for root requests
        server.createContext("/", new HelloHandler());

        // Start the server
        server.setExecutor(null); // Use default executor
        System.out.println("Server started on port 8080");
        server.start();
    }

    // Define a handler for HTTP requests
    static class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // JSON response
            String response = "{\"message\": \"Hello, World!\"}";

            // Set response headers
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);

            // Write the response
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
