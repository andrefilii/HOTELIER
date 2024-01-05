package code.core;

import code.entities.Hotel;
import code.entities.Ratings;
import code.entities.User;
import code.entities.UserReview;
import code.exceptions.IncorrectPasswordException;
import code.exceptions.UserAlreadyLoggedException;
import code.exceptions.UserNotFoundException;
import code.utils.AppConfig;
import code.utils.DatabaseManager;
import code.utils.PasswordUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerManager {
    private static final ServerManager instance = new ServerManager();

    private DatabaseManager databaseManager;
    private ConcurrentHashMap<String, User> loggedUsers = new ConcurrentHashMap<>();

    private ServerManager() {
        databaseManager = DatabaseManager.getInstance();

        startBackgroundUpdater();
    }

    public static ServerManager getInstance() {
        return instance;
    }

    private void startBackgroundUpdater() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> updateRatings(), 0, AppConfig.getRatingsUpdatePeriod(), TimeUnit.SECONDS);
    }

    private void updateRatings() {
        // TODO aggiorna il rating e se cambia la prima posizione invia un multicast
    }

    public boolean isUserLogged(String username) {
        return loggedUsers.containsKey(username);
    }

    public boolean isUserLogged(User user) {
        return loggedUsers.containsValue(user);
    }

    public User getUserByUsername(String username) {
        return loggedUsers.get(username);
    }

    public User loginUser(String username, String password) throws UserNotFoundException, UserAlreadyLoggedException, IncorrectPasswordException{
        User user = databaseManager.getUserByUsername(username);

        if(user == null) throw new UserNotFoundException();

        if (!PasswordUtils.checkPassword(password, user.getPassword())) throw new IncorrectPasswordException();

        if (loggedUsers.putIfAbsent(username, user) != null) throw new UserAlreadyLoggedException();

        return user;
    }

    public void logout(String username) {
        loggedUsers.remove(username);
    }

    public void insertReview(User user, String nomeHotel, String citta, Double globalScore, Ratings ratings) {
        Hotel hotel = databaseManager.getHotelByNameAndCity(nomeHotel, citta);
        if (hotel == null) throw new NullPointerException();
        UserReview review = new UserReview(user.getUsername(), hotel.getId(), globalScore, ratings);

        databaseManager.insertReview(review);
    }

}
