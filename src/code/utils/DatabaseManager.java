package code.utils;

import code.entities.Hotel;
import code.entities.Ratings;
import code.entities.User;
import code.entities.UserReview;
import code.exceptions.UsernameConflictException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DatabaseManager {
    private static final DatabaseManager instance = new DatabaseManager();

    private ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Hotel> hotels = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, UserReview> reviews = new ConcurrentHashMap<>();

    private AtomicBoolean isUserListModified = new AtomicBoolean(false);
    private AtomicBoolean isHotelListModified = new AtomicBoolean(false);
    private AtomicBoolean isRatingsListModified = new AtomicBoolean(false);

    private DatabaseManager() {
        initializeUsersMap();
        initializeHotelsMap();
        initializeRatingsMap();

        startBackgroundUpdater();
    }

    public static DatabaseManager getInstance() {
        return instance;
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

    private void initializeRatingsMap() {
        try (FileReader reader = new FileReader(AppConfig.getDatabaseUrl()+"Reviews.json")) {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<UserReview>>(){}.getType();
            ArrayList<UserReview> reviews = gson.fromJson(reader, listType);

            if (reviews != null) reviews.forEach(r -> this.reviews.put(getReviewMapKey(r), r));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundUpdater() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> persistData(), 0, AppConfig.getDatabaseUpdatePeriod(), TimeUnit.SECONDS);
    }

    private void persistData() {
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
        if (isHotelListModified.get()) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            String json = gson.toJson(hotels.values());

            try (FileWriter writer = new FileWriter(AppConfig.getDatabaseUrl()+"Hotels.json")) {
                writer.write(json);
                isHotelListModified.set(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (isRatingsListModified.get()) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            String json = gson.toJson(reviews.values());

            try (FileWriter writer = new FileWriter(AppConfig.getDatabaseUrl()+"Reviews.json")) {
                writer.write(json);
                isRatingsListModified.set(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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


    public Hotel getHotelByNameAndCity(String nomeHotel, String citta) {
        for (Hotel h : hotels.values()) {
            if (h.getCity().equals(citta) && h.getName().equals(nomeHotel)) return h;
        }
        return null;
    }

    public List<Hotel> getHotelsByCity(String citta) {
        return hotels.values()
                .stream()
                .filter(h -> h.getCity().equals(citta))
                .sorted(Comparator.comparingInt(Hotel::getRank))
                .collect(Collectors.toList());
    }

    public void insertReview(UserReview review) {
        reviews.put(getReviewMapKey(review), review);
        isRatingsListModified.set(true);
        getUserByUsername(review.getUsername()).addRecensione();
        isUserListModified.set(true);
    }

    public List<UserReview> getHotelReviews(Integer hotelID) {
        return reviews.values().stream().filter(r -> r.getHotelID().equals(hotelID)).collect(Collectors.toList());
    }

    private String getReviewMapKey(UserReview review) {
        return review.getUsername() + "_" + review.getHotelID();
    }

}
