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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DatabaseManager {
    private static final DatabaseManager instance = new DatabaseManager();

    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Hotel> hotels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UserReview> reviews = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Hotel>> localRankings = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> firstPositionLocalRankings = new ConcurrentHashMap<>();

    private final AtomicBoolean isUserListModified = new AtomicBoolean(false);
    private final AtomicBoolean isHotelListModified = new AtomicBoolean(false);
    private final AtomicBoolean isRatingsListModified = new AtomicBoolean(false);

    private DatabaseManager() {
        initializeUsersMap();
        initializeHotelsMap();
        initializeRatingsMap();
        initializeRankingMap();

        startBackgroundUpdater();
    }

    public static DatabaseManager getInstance() {
        return instance;
    }
    // TODO se non esiste il file Users/Reviews deve crearli
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



    private void initializeRankingMap() {
        // creo la hashMap che a come chiave la città e come valore una lista ordinata sul rank degli hotel
        hotels.values().forEach(h ->
                localRankings.compute(h.getCity(), (k, list) -> {
                    // se non esiste ancora, creo un treeSet che tiene la collezione ordinata sulla proprietà rank
                    if (list == null) list = new ArrayList<>();
                    list.add(h);

                    return list;
                })
        );

        // ordino per rank
        localRankings.values().forEach(list -> list.sort(Comparator.comparingInt(Hotel::getRank)));

        // una volta creata, salvo i primi posti di ogni città
        for (Map.Entry<String, List<Hotel>> entry : localRankings.entrySet()) {
            firstPositionLocalRankings.put(entry.getKey(), entry.getValue().get(0).getId());
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
        else { // TODO probabilmente da mettere in un computeIfAbsent
            User user = new User();
            user.setUsername(username);
            user.setPassword(PasswordUtils.hashPassword(password));

            users.put(username, user);

            this.isUserListModified.set(true);

            return user;
        }
    }

    public Hotel getHotelById(Integer id) {
        return hotels.get(id);
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

    /**
     * Permette di inserire un nuovo hotel o, se esiste già, di modificarlo
     * @param hotel l'hotel da inserire o aggiornare
     */
    public void insertOrUpdateHotel(Hotel hotel) {
        hotels.put(hotel.getId(), hotel);
    }

    public void insertReview(UserReview review) {
        // salvo la recensione
        reviews.put(getReviewMapKey(review), review);
        isRatingsListModified.set(true);

        // aggiorno il counter delle recensioni per questo utente
        getUserByUsername(review.getUsername()).addRecensione();
        isUserListModified.set(true);

        // aggiorno le medie delle recensioni per questo hotel (atomicamente)
        hotels.computeIfPresent(review.getHotelID(), (k, h) -> {
            List<UserReview> reviews = getHotelReviews(k);
            int numReviews = reviews.size();
            double rate = 0.0;

            double cleaning = 0.0;
            double position = 0.0;
            double services = 0.0;
            double quality = 0.0;
            for (UserReview ur : reviews) {
                rate += ur.getRating();

                Ratings ratings = ur.getRatings();
                cleaning += ratings.getCleaning();
                position += ratings.getPosition();
                services += ratings.getServices();
                quality += ratings.getQuality();
            }

            rate /= numReviews;
            cleaning /= numReviews;
            position /= numReviews;
            services /= numReviews;
            quality /= numReviews;

            Ratings ratings = new Ratings(cleaning, position, services, quality);

            h.setRate(rate);
            h.setRatings(ratings);
            return h;
        });

        isHotelListModified.set(true);
    }

    public List<UserReview> getHotelReviews(Integer hotelID) {
        return reviews.values().stream().filter(r -> r.getHotelID().equals(hotelID)).collect(Collectors.toList());
    }

    private String getReviewMapKey(UserReview review) {
        return review.getUsername() + "_" + review.getHotelID();
    }

    /**
     * Funzione che permette di aggiornare i rank locali ri-eseguendo il sort sulle liste nella map
     * @return map che contiene come chiave le città nelle quali è cambiata la prima posizione e come valore l'hotel
     * che occupa la nuova prima posizione
     */
    public synchronized HashMap<String, Hotel> updateLocalRankings() {
        updateRanks();

        HashMap<String, Hotel> newFirstPos = new HashMap<>();

        localRankings.forEach((k, list) -> {
            // non serve re-inserire gli hotel nella lista perchè puntano agli stessi oggetti nella mappa 'hotel' (quindi sono automaticamente aggiornati)
            // ordino in base alle medie calcolate, e a parità per id
            list.sort(Comparator.comparingDouble(Hotel::getRankValue).reversed().thenComparing(Hotel::getId));

            // dopo aver ordinato in base alla media pesata, avvaloro il campo rank
            for (int i = 0; i < list.size(); i++) {
                list.get(i).setRank(i+1);
            }

            Hotel firstPos = list.get(0);

            if (firstPos.getId() != firstPositionLocalRankings.get(k)) {
                // la prima posizione è nuova, la aggiungo alla map ritornata
                firstPositionLocalRankings.put(k, firstPos.getId());
                newFirstPos.put(k, firstPos);
            }
        });

        return newFirstPos;
    }

    private void updateRanks() {
        // dovendo fare calcoli e possibili modifiche su ogni hotel, sincronizzo la map degli hotel e delle review per evitare aggiornamenti esterni non voluti
        // non sincronizzo localRankings perchè questa struttura viene solo acceduta internamente dalla classe nella funzione sincronizzata updateLocalRankings()
        synchronized (this.reviews) {
            synchronized (this.hotels) {
                // per dare peso alle recensioni in base a quanto sono recenti
                long dateNow = System.currentTimeMillis();
                for (Hotel hotel : hotels.values()) {
                    List<UserReview> hotelReviews = getHotelReviews(hotel.getId());
                    int numReviews = hotelReviews.size();
                    if (numReviews > 0) {
                        double media = getMedia(hotelReviews, dateNow, numReviews);

                        hotel.setRankValue(media);
                    } else {
                        hotel.setRankValue(0.0);
                    }
                }
            }
        }
    }

    private double getMedia(List<UserReview> hotelReviews, long dateNow, int numReviews) {
        double sumValori = 0;
        double sumPesi = 0;
        double media = 0;

        for (UserReview review : hotelReviews) {
            double diffTime = dateNow - review.getTimestamp();
            sumValori += review.getRating() / diffTime;
            sumPesi += 1.0/diffTime;
        }

        // calcolo la media pesata a cui aggiungo il numero di recensioni (a parità di media vincono quelle con più recensioni)
        media = (sumValori/sumPesi) + numReviews;
        return media;
    }

}
