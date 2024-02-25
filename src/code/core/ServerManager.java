package code.core;

import code.entities.Hotel;
import code.entities.Ratings;
import code.entities.User;
import code.entities.UserReview;
import code.exceptions.IncorrectPasswordException;
import code.exceptions.UserAlreadyLoggedException;
import code.exceptions.UserNotFoundException;
import code.utils.AppConfig;
import code.utils.PasswordUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Map;
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

    private Gson gson;

    private ServerManager() {
        databaseManager = DatabaseManager.getInstance();
        gson = new GsonBuilder().setPrettyPrinting().create();
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

    /**
     * Funzione chiamata periodicamente in background per aggiornare i ranking locali
     * Chiama la funzione di aggiornamento in DatabaseManager e, se ci sono cambiamenti, crea un array json contenente
     * le nuove prime posizioni da spedire a tutti i client in ascolto sul gruppo multicast
     */
    private void updateRatings() {
        /* Aggiorno i ranking e, se cambiano, prendo le nuove prime posizioni*/
        HashMap<String, Hotel> newFirstPositions = databaseManager.updateLocalRankings();

        if (!newFirstPositions.isEmpty()) {
            // costruisco il json con le nuove prime posizioni
            JsonArray jsonArray = new JsonArray();
            for (Map.Entry<String, Hotel> entry : newFirstPositions.entrySet()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("citta", entry.getKey());
                obj.addProperty("nomeHotel", entry.getValue().getName());

                jsonArray.add(obj);
            }

            String message = gson.toJson(jsonArray);

            try {
                DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, group, AppConfig.getMulticastPort());
                ms.send(packet);
                System.out.println("Classifica aggiornata, inviata correttamente notifica ai client connessi. Messaggio:\n" + message);
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

    /**
     * Permette il login di un utente sul servizio
     * @param username username dell'utente
     * @param password password (in chiaro) passata al server
     * @return l'utente loggato correttamente
     * @throws UserNotFoundException se non esiste nessun utente nel database con lo username passato
     * @throws UserAlreadyLoggedException se l'utente è già loggato sul server
     * @throws IncorrectPasswordException se l'utente esiste, ma la password è incorretta
     */
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

    /**
     * Permette di inserire una recensione per un hotel
     * @param user
     * @param nomeHotel
     * @param citta
     * @param globalScore
     * @param ratings
     */
    public void insertReview(User user, String nomeHotel, String citta, Double globalScore, Ratings ratings) {
        Hotel hotel = databaseManager.getHotelByNameAndCity(nomeHotel, citta);
        if (hotel == null) throw new NullPointerException();
        UserReview review = new UserReview(user.getUsername(), hotel.getId(), globalScore, ratings, System.currentTimeMillis());

        databaseManager.insertReview(review);
    }

}
