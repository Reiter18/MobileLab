import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class CatalogServer {
    public static void main(String[] args) throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8765), 0);
        server.createContext("/cars", exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            String term = query == null ? "" : URLDecoder.decode(query.replaceFirst("^q=", ""),
                StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT);

            List<String> cars = new ArrayList<>();
            String searchable = "mercedes-benz s 500 sedan";
            if (searchable.contains(term)) {
                for (int id = 1; id <= 8; id++) {
                    cars.add("""
                        {"id":"%d","model":"S 500 Sedan","brand":"Mercedes-Benz",
                         "price":2500,"transmission":"A/T","fuel":"Бензин"}
                        """.formatted(id));
                }
            }
            byte[] body = ("[" + String.join(",", cars) + "]").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        System.out.println("DriveNext catalog: http://127.0.0.1:8765/cars");
    }
}
