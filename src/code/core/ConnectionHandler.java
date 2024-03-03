package code.core;

import code.entities.Hotel;
import code.entities.Ratings;
import code.entities.User;
import code.exceptions.IncorrectPasswordException;
import code.exceptions.UserAlreadyLoggedException;
import code.exceptions.UserNotFoundException;
import code.exceptions.UsernameConflictException;
import code.utils.AppConfig;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

/**
 * @author Andrea Filippi
 *
 * Questa classe viene utilizzata dal listener quando accetta una connessione per iniziare a ricevere comandi e inviare risposte
 *
 * @see ConncetionListener
 *
 */
public class ConnectionHandler implements Runnable{
    private final Socket socket;
    private final DatabaseManager databaseManager;
    private final ServerManager serverManager;
    private final Gson gson;
    private User curUser;

    public ConnectionHandler(Socket socket) {
        this.socket = socket;
        this.curUser = null;
        this.databaseManager = DatabaseManager.getInstance();
        this.serverManager = ServerManager.getInstance();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Metodo che gestisce una richiesta di registrazione da parte di un client. Il metodo si aspetta di ricevere una stringa
     * rappresentante un oggetto json formato così: {"username":"","password":""}.<br>
     *
     *
     * @param bodyString il corpo della richiesta che conterrà l'oggetto json
     * @return - "201 CREATED" se l'utente viene correttamente inserito nel sistema<br>
     * - "409 CONFLICT" se esiste già un utente con lo stesso username<br>
     * - "400 BAD REQUEST" se l'oggetto non contiene le proprietà richieste
     *
     * @see DatabaseManager#registerUser(String, String)
     */
    private String register(String bodyString) {
        JsonObject body;
        try {
            body = toJsonObject(bodyString);
        } catch (JsonSyntaxException e) {
            return "400 BAD REQUEST";
        }

        // recupero le proprietà dell'oggetto che mi servono
        String username;
        String password;
        try {
            username = body.get("username").getAsString();
            password = body.get("password").getAsString();
        } catch (NullPointerException e) {
            return "400 BAD REQUEST";
        }

        try {
            databaseManager.registerUser(username, password);
            return "201 CREATED";
        } catch (UsernameConflictException e) {
            return "409 CONFLICT";
        } catch (NullPointerException e) {
            return "400 BAD REQUEST";
        }
    }

    /**
     * Metodo che gestisce una richiesta di login da parte di un client
     * Il metodo si aspetta di ricevere una stringa rappresentante un oggetto json formato così:<br>
     * {
     *  "username":"",<br>
     *  "password":"",<br>
     * }<br><br>
     *
     * @param bodyString il corpo della richiesta che conterrà l'oggetto json
     * @return - "200 OK" se il login è effettuato con successo. Viene inviato anche un oggetto json contenente il
     * gruppo multicast per ricevere aggiornamenti sui ranking locali<br>
     * - "409 CONFLICT" se l'utente è già loggato sul server<br>
     * - "404 NOT FOUND" se non viene trovato nessun utente con lo username passato<br>
     * - "401 UNAUTHORIZED" se le credenziali passate sono errate
     * - "400 BAD REQUEST" se l'oggetto non contiene le proprietà richieste
     *
     * @see ServerManager#loginUser(String, String)
     */
    private String login(String bodyString) {
        // su questa sessione c'è già un utente connesso
        if (curUser != null) return "409 CONFLICT";

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
            // effettuo il login sul server e se ha successo salvo l'utente loggato sulla connessione in this.curUser
            curUser = serverManager.loginUser(username, password);

            // cro l'oggetto contenente le informazioni necessarie per iscriversi al gruppo multicast per il ranking
            JsonObject multicast = new JsonObject();
            multicast.addProperty("group", AppConfig.getMulticastGroup());
            multicast.addProperty("port", AppConfig.getMulticastPort());

            return "200 OK\n" + gson.toJson(multicast);
        } catch (UserAlreadyLoggedException e) {
            return "409 CONFLICT";
        } catch (UserNotFoundException e) {
            return "404 NOT FOUND";
        } catch (IncorrectPasswordException e) {
            return "401 UNAUTHORIZED";
        }
    }

    /**
     * Metodo che gestisce una richiesta di logout da parte di un client. Il metodo si aspetta di ricevere una stringa
     * rappresentante un oggetto json formato così: {"username":""}.<br>
     *
     *
     * @param bodyString il corpo della richiesta che conterrà l'oggetto json
     * @return - "200 OK" se il logout è effettuato con successo<br>
     * - "400 BAD REQUEST" se il corpo della richiesta è errato
     *
     * @see ServerManager#logout(String)
     */
    private String logout(String bodyString) {
        if (curUser == null ) return "401 UNAUTHORIZED";

        serverManager.logout(curUser.getUsername());
        curUser = null;

        return "200 OK";
    }

    /**
     * Metodo che gestisce una richiesta delle informazioni riguardo un hotel. Il metodo si aspetta di ricevere una stringa
     * rappresentante un oggetto json formato così: {"nomeHotel":"","citta":""}<br>
     *
     * @param bodyString il corpo della richiesta che conterrà l'oggetto json
     * @return - "200 OK" se ha trovato l'hotel. Conterrà nel corpo l'oggetto json rappresentante l'entità {@link Hotel}<br>
     * - "400 BAD REQUEST" se il corpo della richiesta è errato
     * - "404 NOT FOUND" se non viene trovato nessun hotel con il nome e città passati
     *
     * @see Hotel
     */
    private String searchHotel(String bodyString) {
        JsonObject body;
        try {
            body = toJsonObject(bodyString);
        } catch (JsonSyntaxException e) {
            return "400 BAD REQUEST";
        }

        String nomeHotel;
        String citta;
        try {
            nomeHotel = body.get("nomeHotel").getAsString();
            citta = body.get("citta").getAsString();
        } catch (NullPointerException e) {
            return "400 BAD REQUEST";
        }

        Hotel h = databaseManager.getHotelByNameAndCity(nomeHotel, citta);
        if (h == null) return "404 NOT FOUND";

        return "200 OK\n" + gson.toJson(h);
    }

    /**
     * Metodo che gestisce una richiesta delle informazioni riguardo gli hotel in una determinatà città. Il metodo si aspetta di ricevere una stringa
     * rappresentante un oggetto json formato così: {"citta":""}<br>
     *
     * @param bodyString il corpo della richiesta che conterrà l'oggetto json
     * @return - "200 OK" se ha trovato gli hotel. Conterrà nel corpo l'oggetto json rappresentante una lista di entità {@link Hotel}<br>
     * - "400 BAD REQUEST" se il corpo della richiesta è errato<br>
     * - "404 NOT FOUND" se non viene trovato nessun hotel nella città passata
     *
     * @see Hotel
     */
    private String searchAllHotels(String bodyString) {
        JsonObject body;
        try {
            body = toJsonObject(bodyString);
        } catch (JsonSyntaxException e) {
            return "400 BAD REQUEST";
        }

        String citta;
        try {
            citta = body.get("citta").getAsString();
        } catch (NullPointerException e) {
            return "400 BAD REQUEST";
        }

        List<Hotel> h = databaseManager.getHotelsByCity(citta);
        if (h == null || h.isEmpty()) return "404 NOT FOUND";

        Type typeHotel = new TypeToken<List<Hotel>>(){}.getType();

        return "200 OK\n" + gson.toJson(h, typeHotel);
    }

    /**
     * Metodo che permette di gestire la richiesta di aggiunta di una recensione da parte di un utente.
     * L'utente deve aver precedentemente effettuato il login sulla sessione per aggiungere una recensione.
     * Il metodo si aspetta di ricevere una stringa rappresentante un oggetto json così formato:<br>
     * {<br>
     *     "nomeHotel": "Hotel Genova 2",<br>
     *     "globalScore": 3.5,<br>
     *     "singleScores": {<br>
     *       "cleaning": 1.3,<br>
     *       "position": 4.5,<br>
     *       "services": 3.5,<br>
     *       "quality": 5.0<br>
     *     }<br>
     * }<br><br>
     *
     * Se il metodo ha successo, inserisce la recensione nel database e aggiorna il counter di recensioni associate
     * all'utente
     *
     * @param bodyString il corpo della richiesta che conterrà l'oggetto json
     * @return - "200 OK" se ha correttamente inserito la recensione<br>
     * - "400 BAD REQUEST" se il corpo della richiesta è errato<br>
     * - "401 UNAUTHORIZED" se l'utente non ha effettuato il login
     * - "404 NOT FOUND" se l'hotel per il quale si vuole aggiungere una recensione non esiste
     *
     * @see Hotel
     */
    private String insertReview(String bodyString) {
        // non ci sono utenti loggati sulla sessione
        if (curUser == null) return "401 UNAUTHORIZED";

        JsonObject body;
        try {
            body = toJsonObject(bodyString);
        } catch (JsonSyntaxException e) {
            return "400 BAD REQUEST";
        }

        String nomeHotel;
        String citta;
        Double globalScore;
        Ratings singleScores;
        try {
            nomeHotel = body.get("nomeHotel").getAsString();
            citta = body.get("citta").getAsString();
            globalScore = body.get("globalScore").getAsDouble();
            singleScores = gson.fromJson(body.get("singleScores"), Ratings.class);
        } catch (NullPointerException | JsonSyntaxException e) {
            return "400 BAD REQUEST";
        }

        try {
            serverManager.insertReview(curUser, nomeHotel, citta, globalScore, singleScores);
            return "200 OK";
        } catch (NullPointerException e) {
            return "404 NOT FOUND";
        }
    }

    /**
     * Metodo che restituisce il badge dell'utente collegato. Non si aspetta un corpo ma l'utente deve aver effettuato
     * il login
     *
     * @return - "200 OK" se ha recuperato il badge con successo. Restituisce un oggetto json contenente il badge<br>
     * - "401 UNAUTHORIZED" se l'utente non ha effettuato il login
     */
    private String showMyBadges() {
        // non ci sono utenti loggati sulla sessione
        if (curUser == null) return "401 UNAUTHORIZED";

        return "200 OK\n{\"badge\":\"" + curUser.getBadge() + "\"}";
    }

    /**
     * Costruisce una stringa contenente il corpo della richiesta. Il corpo può essere di lunghezza variabile e viene
     * inteso come concluso quando viene trovata una linea vuota
     *
     * @param in scanner associato allo stream di input del socket
     * @return il corpo della richiesta
     */
    private String getRequestBody(Scanner in) {
        StringBuilder bodyBuilder = new StringBuilder();
        while (in.hasNextLine()) {
            String line = in.nextLine();
            if (line.isEmpty()) break;
            bodyBuilder.append(line);
        }

        return bodyBuilder.toString();
    }

    /**
     * Funzione che trasforma una stringa in {@link JsonObject}
     *
     * @param jsonString stringa rappresentante un oggetto json
     * @return un JsonObject creato a partire da jsonString
     * @throws JsonSyntaxException se l'oggetto è malformato
     */
    private JsonObject toJsonObject(String jsonString) throws JsonSyntaxException {
        return gson.fromJson(jsonString, JsonObject.class);
    }

    /**
     * Apre gli stream da/verso il client e si mette in attesa di ricevere i comandi, con eventuale corpo. Una volta
     * elaborate le richieste invia al client una risposta testuale contenente uno status code ed eventualmente un corpo.<br>
     * Quando la connessione viene interrotta, si occupa di chiudere correttamente gli stream e di eliminare l'utente dalla
     * lista di utenti connessi
     */
    private void waitForCommands() {
        try (Scanner in = new Scanner(socket.getInputStream());
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            while (in.hasNextLine()) {
                System.out.println("comando preso");
                // legge il comando
                String command = in.nextLine().trim();
                // legge il corpo se presente
                String requestBody = getRequestBody(in);
                // Stringa da inviare come risposta
                String response;
                switch (command) {
                    case "register":
                        response = register(requestBody);
                        break;
                    case "login":
                        response = login(requestBody);
                        break;
                    case "logout":
                        response = logout(requestBody);
                        break;
                    case "searchHotel":
                        response = searchHotel(requestBody);
                        break;
                    case "searchAllHotels":
                        response = searchAllHotels(requestBody);
                        break;
                    case "insertReview":
                        response = insertReview(requestBody);
                        break;
                    case "showMyBadges":
                        response = showMyBadges();
                        break;
                    default:
                        response = "400 BAD REQUEST";
                }

                out.println(response + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // se il client si disconnette mentre era loggato, eseguo il logout
        if (curUser != null) {
            serverManager.logout(curUser.getUsername());
        }
    }

    @Override
    public void run() {
        System.out.println("Connected: " + socket);
        waitForCommands();
        System.out.println("Connection closed: " + socket);
    }
}
