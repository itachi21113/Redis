package org.programming.utkarsh;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        int port = 6379;
        System.out.println("Starting J-Redis Server...");

        // 1. Initialize the Single Source of Truth (The Database)
        KVStore kvStore = new KVStore();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port: " + port);

            while (true) {
                // 1. Wait for a client
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // 2. Delegate to the separate handler class
                // We create a new ClientHandler object and run it in a new Thread.
                ClientHandler clientHandler = new ClientHandler(clientSocket , kvStore);
                new Thread(clientHandler).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}