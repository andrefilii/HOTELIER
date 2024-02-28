package code.core;

import code.entities.Hotel;
import code.entities.Ratings;
import code.entities.User;
import code.entities.UserReview;
import code.exceptions.UsernameConflictException;
import code.utils.AppConfig;
import code.utils.PasswordUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
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


    /**
     * Funzione che inizializza le strutture dati contenenti i dati sugli utenti. Popola la mappa leggendo i dati da
     * Users.json
     * Se il file non esiste, lo crea nella cartella specificata in application.properties
     */
    private void initializeUsersMap() {
        File file = new File(AppConfig.getDatabaseUrl()+"Users.json");

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Gson gson = new Gson();
                Type listType = new TypeToken<ArrayList<User>>() {}.getType();
                ArrayList<User> users = gson.fromJson(reader, listType);

                if (users != null) users.forEach(u -> this.users.put(u.getUsername(), u));

            } catch (IOException e) {
                System.out.println("--- Errore lettura file Users.json ---");
                e.printStackTrace();
            }
        } else {
            // se il file degli Utenti non esiste ancora, lo creo
            try {
                if (!file.createNewFile()) {
                    System.out.println("--- Errore durante la creazione di Users.json ---");
                }
            } catch (IOException e) {
                System.out.println("--- Errore durante la creazione di Users.json ---");
                e.printStackTrace();
            }
        }
    }


    /**
     * Funzione che inizializza le strutture dati contenenti i dati sugli hotel.
     * Popola la mappa leggendo i dati da Hotels.json
     * Se il file non esiste restituisce un'eccezione. Questo file infatti DEVE essere presente all'avvio
     */
    private void initializeHotelsMap() {
        try (FileReader reader = new FileReader(AppConfig.getDatabaseUrl()+"Hotels.json")) {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Hotel>>(){}.getType();
            ArrayList<Hotel> hotels = gson.fromJson(reader, listType);

            if (hotels != null) hotels.forEach(h -> this.hotels.put(h.getId(), h));

        } catch (IOException e) {
            System.out.println("--- Errore lettura file Hotels.json ---");
            e.printStackTrace();
        }
    }


    /**
     * Funzione che inizializza le strutture dati contenenti i dati sulle recensioni. Popola la mappa leggendo i dati da
     * Reviews.json
     * Se il file non esiste, lo crea nella cartella specificata in application.properties
     */
    private void initializeRatingsMap() {
        File file = new File(AppConfig.getDatabaseUrl()+"Reviews.json");

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Gson gson = new Gson();
                Type listType = new TypeToken<ArrayList<UserReview>>(){}.getType();
                ArrayList<UserReview> reviews = gson.fromJson(reader, listType);

                if (reviews != null) reviews.forEach(r -> this.reviews.put(getReviewMapKey(r), r));

            } catch (IOException e) {
                System.out.println("--- Errore lettura file Ratings.json ---");
                e.printStackTrace();
            }
        } else {
            // se il file degli Utenti non esiste ancora, lo creo
            try {
                if (!file.createNewFile()) {
                    System.out.println("--- Errore durante la creazione di Ratings.json ---");
                }
            } catch (IOException e) {
                System.out.println("--- Errore durante la creazione di Ratings.json ---");
                e.printStackTrace();
            }
        }
    }


    /**
     * Funzione che inizializza la struttura dati che contiene una lista delgi hotel ordinati per rank locale.
     */
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
    }


    /**
     * Avvia il task che persiste periodicamente le strutture dati. Viene eseguito ogni database.updatePeriod secondi
     * (proprietà da inserire nel file application.properties, default 60 secondi)
     */
    private void startBackgroundUpdater() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> persistData(), 0, AppConfig.getDatabaseUpdatePeriod(), TimeUnit.SECONDS);
    }


    /**
     * Funzione chiamata dallo scheduledThreadPool. Controlla se ci sono aggiornamenti nelle strutture dati, e nel caso
     * trasforma i valori in json e li salva su disco
     */
    private void persistData() {
        if (isUserListModified.getAndSet(false)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            String json = gson.toJson(users.values());

            try (FileWriter writer = new FileWriter(AppConfig.getDatabaseUrl()+"Users.json")) {
                writer.write(json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (isHotelListModified.getAndSet(false)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            String json = gson.toJson(hotels.values());

            try (FileWriter writer = new FileWriter(AppConfig.getDatabaseUrl()+"Hotels.json")) {
                writer.write(json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (isRatingsListModified.getAndSet(false)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            String json = gson.toJson(reviews.values());

            try (FileWriter writer = new FileWriter(AppConfig.getDatabaseUrl()+"Reviews.json")) {
                writer.write(json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public User getUserByUsername(String username) {
        return users.get(username);
    }


    /**
     * Permette di registrare un nuovo utente. In questa fase esegue anche l'hashing della password per non salvarla
     * in chiaro
     * @param username
     * @param password password in chiaro dell'utente
     * @return l'utente appena creato
     * @throws UsernameConflictException se esiste già un utente con questo username
     * @throws NullPointerException se i parametri passati sono vuoti o null
     */
    public User registerUser(String username, String password) throws UsernameConflictException, NullPointerException {
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) throw new NullPointerException();
        if (users.containsKey(username)) throw new UsernameConflictException();
        else {
            // inserisco l'utente e lo faccio in modo atomico
            users.computeIfAbsent(username, (k) -> {
                User user = new User();
                user.setUsername(username);
                user.setPassword(PasswordUtils.hashPassword(password));
                return user;
            });

            this.isUserListModified.set(true);

            return users.get(username);
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


    /**
     * Permette di inserire una recensione riguardante un hotel. Aggiorna inoltre le medie per quell'hotel e aggiorna il numero
     * di recensioni riguardanti questo utente.<br>
     * NB: un utente può inserire solo una recensione per un determinato hotel, se prova a inserirne una nuova, quella vecchia
     * viene sovrascritta
     * @param review recensione da inserire
     */
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

//            rate /= numReviews;
//            cleaning /= numReviews;
//            position /= numReviews;
//            services /= numReviews;
//            quality /= numReviews;
//
            // calcolo le medie e arrotondo a 2 cifre dopo la virgola
            rate = Math.round(rate/numReviews * 100.0) / 100.0;
            cleaning = Math.round(cleaning/numReviews * 100.0) / 100.0;
            position = Math.round(position/numReviews * 100.0) / 100.0;
            services = Math.round(services/numReviews * 100.0) / 100.0;
            quality = Math.round(quality/numReviews * 100.0) / 100.0;

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

    /**
     * Crea la chiave per la mappa delle recensioni, in questo modo si può trovare velocemente una recensione dato l'utente e l'hotel
     * @param review la recensione che contiene i dati per creare la chiave
     * @return la chiave creata, con la forma {@literal <}username{@literal >}_{@literal <}hotelID{@literal >}
     */
    private String getReviewMapKey(UserReview review) {
        return review.getUsername() + "_" + review.getHotelID();
    }


    /**
     * Funzione che permette di aggiornare i rank locali ri-eseguendo il sort sulle liste nella map
     * @return map che contiene come chiave le città nelle quali è cambiata la prima posizione e come valore l'hotel
     * che occupa la nuova prima posizione
     */
    public synchronized HashMap<String, Hotel> updateLocalRankings() {
        HashMap<String, Hotel> newFirstPos = new HashMap<>();

        // dovendo fare calcoli e possibili modifiche su ogni hotel, sincronizzo la map degli hotel e delle review per evitare aggiornamenti esterni non voluti
        // non sincronizzo localRankings perchè questa struttura viene solo acceduta internamente dalla classe nella funzione sincronizzata updateLocalRankings()
        synchronized (this.reviews) {
            synchronized (this.hotels) {
                updateRanks();

                localRankings.forEach((citta, list) -> {
                    // prima di riordinare prendo il primo hotel (potrebbe cambiare)
                    Hotel oldFirstPos = list.get(0);

                    // non serve re-inserire gli hotel nella lista perché puntano agli stessi oggetti nella mappa 'hotel' (quindi sono automaticamente aggiornati)
                    // ordino in base alle medie calcolate, se sono uguali ordino per recensione più recente e infine in ordine di Id
                    list.sort((h1, h2) -> {
                        Double h1Rank = h1.getRankValue();
                        Double h2Rank = h2.getRankValue();
                        // prima ordino per rankValue DECRESCENTE
                        if ( (h1Rank != null && (h2Rank == null || (h1Rank > h2Rank) )) ) {
                            // h1 > h2
                            return -1;
                        } else if ( (h2Rank != null && (h1Rank == null || (h1Rank < h2Rank) )) ) {
                            // h1 < h2
                            return 1;
                        } else {
                            // h1 == h2
                            // a parità di rankValue, guardo chi ha la recensione più recente
                            Long d1 = h1.getDistanzaUltimaRecensione();
                            Long d2 = h2.getDistanzaUltimaRecensione();
                            if ( (d1 != null && (d2 == null || (d1 < d2) )) ) {
                                // h1 > h2
                                return -1;
                            } else if ( (d2 != null && (d1 == null || (d1 > d2) )) ) {
                                // h1 < h2
                                return 1;
                            } else {
                                // h1 == h2, con stessa distanza di recensioni
                                // ordino semplicemente per id
                                return Integer.compare(h1.getId(), h2.getId());
                            }
                        }
                    });

                    // dopo aver ordinato in base alla media pesata, avvaloro il campo rank
                    for (int i = 0; i < list.size(); i++) {
                        list.get(i).setRank(i + 1);
                    }

                    Hotel firstPos = list.get(0);

                    if (firstPos.getId() != oldFirstPos.getId()) {
                        // la prima posizione è nuova, la aggiungo alla map ritornata
                        newFirstPos.put(citta, firstPos);
                    }
                });

                isHotelListModified.set(true);
            }
        }

        return newFirstPos;
    }


    /**
     * Aggiorna i rankvalue di tutti gli hotel
     */
    private void updateRanks() {
        // per dare peso alle recensioni in base a quanto sono recenti
        long dateNow = System.currentTimeMillis();
        for (Hotel hotel : hotels.values()) {
            List<UserReview> hotelReviews = getHotelReviews(hotel.getId());
            if (!hotelReviews.isEmpty()) {
                double sumValori = 0;
                double sumPesi = 0;
                long minDiffDays = Long.MAX_VALUE;

                for (UserReview review : hotelReviews) {
                    // prendo la differenza di giorni (86400000 = 1000ms * 60s * 60m * 24h), +1 per evitare di avere 0 quando una recensione è nello stesso giorno
                    long diffDays = ((dateNow - review.getTimestamp()) / 86400000) + 1;
                    if (diffDays < minDiffDays) minDiffDays = diffDays;
                    sumValori += review.getRating() / diffDays;
                    sumPesi += 1.0/diffDays;
                }

                double media = (sumValori/sumPesi);
                // per risolvere problemi con la precisione di macchina, se la media contiene riporti in fondo, normalizzo a 5.0
                if (Double.compare(media, 5.0) > 0) media = 5.0;

                // calcolo la media pesata a cui aggiungo il numero di recensioni (a parità di media preferisco quelle con più recensioni)
                hotel.setRankValue( media + hotelReviews.size() );

                hotel.setDistanzaUltimaRecensione(minDiffDays);
            } else {
                hotel.setRankValue(null);
                hotel.setDistanzaUltimaRecensione(null);
            }
        }
    }

}
