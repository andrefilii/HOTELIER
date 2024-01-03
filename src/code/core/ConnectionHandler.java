package code.core;

import code.entities.User;
import code.exceptions.UsernameConflictException;
import code.utils.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ConnectionHandler implements Runnable{
    private final Socket socket;
    private final DatabaseManager databaseManager;
    private User curUser;

    public ConnectionHandler(Socket socket) {
        this.socket = socket;
        this.curUser = null;
        this.databaseManager = DatabaseManager.getInstance();
    }

    private String register(String bodyString) {
        JsonObject body;
        try {
            body = toJsonObject(bodyString);
        } catch (JsonSyntaxException e) {
            return "400 BAD REQUEST";
        }

        String username;
        String password;
        try {
            username = body.get("username").getAsString();
            password = body.get("password").getAsString();
        } catch (NullPointerException e) {
            return "400 BAD REQUEST";
        }

        try {
            curUser = databaseManager.registerUser(username, password);

            return "201 CREATED";
        } catch (UsernameConflictException e) {
            return "409 CONFLICT";
        } catch (NullPointerException e) {
            return "400 BAD REQUEST";
        }
    }

    private String getRequestBody(Scanner in) {
        StringBuilder bodyBuilder = new StringBuilder();
        while (in.hasNextLine()) {
            String line = in.nextLine();
            if (line.isEmpty()) break;
            bodyBuilder.append(line);
        }

        return bodyBuilder.toString();
    }

    private JsonObject toJsonObject(String jsonString) throws JsonSyntaxException {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, JsonObject.class);
    }

    @Override
    public void run() {
        System.out.println("Connected: " + socket);
        try (Scanner in = new Scanner(socket.getInputStream());
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            while (in.hasNextLine()) {
                String command = in.nextLine().toLowerCase().trim();
                switch (command) {
                    case "register":
                        String body = getRequestBody(in);
                        out.println(register(body));
                        break;
                    case "login":
                        break;
                    case "logout":
                        break;
                    case "searchHotel":
                        break;
                    case "searchAllHotels":
                        break;
                    case "insertReview":
                        break;
                    case "showMyBadges":
                        break;
                    default:
                        out.println("400 Bad Request");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Connection closed: " + socket);
    }
}
