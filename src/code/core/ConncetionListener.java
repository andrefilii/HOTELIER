package code.core;

import code.utils.AppConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Andrea Filippi
 */
public class ConncetionListener {

    /**
     * Funzione di partenza del server: si mette in ascolto sulla porta specificata in application.properties e crea un
     * threadpool di dimensione specificata sempre nelle proprietà
     *
     * @see AppConfig
     */
    public void start() {
        try(ServerSocket listener = new ServerSocket(AppConfig.getPort())) {
            printApplicationCoordinates();

            ExecutorService pool = Executors.newFixedThreadPool(AppConfig.getMaxUsers());
            while (true) {
                pool.execute(new ConnectionHandler(listener.accept()));
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
