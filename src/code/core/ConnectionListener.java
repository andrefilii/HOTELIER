package code.core;

import code.utils.AppConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * @author Andrea Filippi
 */
public class ConnectionListener {

    /**
     * Funzione di partenza del server: si mette in ascolto sulla porta specificata in application.properties e crea un
     * threadpool di dimensione specificata sempre nelle proprietà
     *
     * @see AppConfig
     */
    public void start() {
        try(ServerSocket listener = new ServerSocket(AppConfig.getPort())) {
            printApplicationCoordinates();

            int maxUsers = AppConfig.getMaxUsers();

            /*
            Thread pool con le seguenti caratteristiche:
            - devono essere sempre attivi un numero di thread che sia la metà del numero massimo di utenti
            - il massimo di thread deve essere il numero massimo di utenti
            - i thread al di fuori del core possono rimanere inattivi per massimo 60 secondi prima di essere eliminati
            - si usa una SynchronousQueue per indicare che NON possono rimanere task in attesa, se non ci sono thread
                disponibili, fallisce
             */
            ExecutorService pool = new ThreadPoolExecutor((int)Math.ceil(maxUsers/2.0), maxUsers,
                    60L, TimeUnit.SECONDS,
                    new SynchronousQueue<>());

            while (true) {
                Socket clientSocket = listener.accept();
                try {
                    pool.execute(new ConnectionHandler(clientSocket));
                } catch (RejectedExecutionException e) {
                    // ho superato il massimo numero di client contemporanei, chiudo la connessione
                    clientSocket.close();
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printApplicationCoordinates() {
        System.out.println("Application started at localhost:" + AppConfig.getPort());
    }
}
