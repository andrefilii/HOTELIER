package code.utils;

import code.entities.Hotel;
import code.entities.User;
import code.exceptions.UsernameConflictException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DatabaseManager {
    private static final DatabaseManager instance = new DatabaseManager();

    private ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Hotel> hotels = new ConcurrentHashMap<>();

    private AtomicBoolean isUserListModified = new AtomicBoolean(false);
    private AtomicBoolean isHotelListModified = new AtomicBoolean(false);

    private DatabaseManager() {
        initializeUsersMap();
        initializeHotelsMap();

        startBackgroundUpdater();
    }

    private void initializeUsersMap() {
        try (FileReader reader = new FileReader(AppConfig.getDatabaseUrl()+"Users.json")) {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<User>>(){}.getType();
            ArrayList<User> users = gson.fromJson(reader, listType);

            if (users != null) users.forEach(u -> this.users.put(u.getUsername(), u));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeHotelsMap() {
        try (FileReader reader = new FileReader(AppConfig.getDatabaseUrl()+"Hotels.json")) {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Hotel>>(){}.getType();
            ArrayList<Hotel> hotels = gson.fromJson(reader, listType);

            if (hotels != null) hotels.forEach(h -> this.hotels.put(h.getId(), h));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundUpdater() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> persistData(), 0, 5, TimeUnit.SECONDS);
    }

    private void persistData() {
        System.out.println("Hey");
        if (isUserListModified.get()) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            String json = gson.toJson(users.values());

            try (FileWriter writer = new FileWriter(AppConfig.getDatabaseUrl()+"Users.json")) {
                writer.write(json);

                isUserListModified.set(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (isUserListModified.get()) {

        }
    }

    public static DatabaseManager getInstance() {
        return instance;
    }

    public User getUserByUsername(String username) {
        return users.get(username);
    }

    public User registerUser(String username, String password) throws UsernameConflictException, NullPointerException {
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) throw new NullPointerException();
        if (users.containsKey(username)) throw new UsernameConflictException();
        else {
            User user = new User();
            user.setUsername(username);
            user.setPassword(PasswordUtils.hashPassword(password));

            users.put(username, user);

            this.isUserListModified.set(true);

            return user;
        }
    }

}
