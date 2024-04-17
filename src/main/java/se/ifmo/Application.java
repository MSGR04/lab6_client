package se.ifmo;

import se.ifmo.core.io.IOWorker;
import se.ifmo.core.io.console.BufferedConsoleWorker;
import se.ifmo.core.socket.client.Client;
import se.ifmo.core.socket.client.ClientConfiguration;

import java.net.InetAddress;

public class Application {
    public static void main(String[] args) {
        try (IOWorker clientConsole = new BufferedConsoleWorker();
             Client client = new Client(new ClientConfiguration(InetAddress.getLocalHost(), 1313), clientConsole)) {

            client.start();
        } catch (Exception e) {
            System.err.println("Ошибка во время исполнения: " + e.getMessage());
        }
    }
}


