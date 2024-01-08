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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerManager {
    private static final ServerManager instance = new ServerManager();

    private DatabaseManager databaseManager;
    private MulticastSocket ms;
    private InetAddress group;
    private ConcurrentHashMap<String, User> loggedUsers = new ConcurrentHashMap<>();

    private ServerManager() {
        databaseManager = DatabaseManager.getInstance();

        initBackgroundUpdater();
    }

    public static ServerManager getInstance() {
        return instance;
    }

    private void initBackgroundUpdater() {
        try {
            group = InetAddress.getByName(AppConfig.getMulticastGroup());
            ms = new MulticastSocket(AppConfig.getMulticastPort());

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> updateRatings(), 0, AppConfig.getRatingsUpdatePeriod(), TimeUnit.SECONDS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateRatings() {
        // TODO aggiorna il rating
        // ...
        // if (<prima posizione cambia>) {
        if (true) {
            try {
                // TODO il datagramma diventerà un oggetto json con il primo classificato
                DatagramPacket packet = new DatagramPacket("hello".getBytes(), "hello".getBytes().length, group, AppConfig.getMulticastPort());
                ms.send(packet);
                System.out.println("Classifica aggiornata, inviata correttamente notifica ai client connessi");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        UserReview review = new UserReview(user.getUsername(), hotel.getId(), globalScore, ratings, System.currentTimeMillis());

        databaseManager.insertReview(review);
    }

}
