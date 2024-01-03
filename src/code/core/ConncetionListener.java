package code.core;

import code.utils.AppConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConncetionListener {

    public void start() {
        try(ServerSocket listener = new ServerSocket(AppConfig.getPort())) {
            printApplicationCoordinates(listener.getInetAddress());

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

    private void printApplicationCoordinates(InetAddress address) {
        System.out.println("Application started at //" +
                address.getHostName() + ":" + AppConfig.getPort());
    }
}
