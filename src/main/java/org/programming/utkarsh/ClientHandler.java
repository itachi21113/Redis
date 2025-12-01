package org.programming.utkarsh;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream; // Import this
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            // 1. Get the Output Stream so we can write back to the client
            OutputStream writer = clientSocket.getOutputStream();

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Received: " + line);

                // 2. A Simple "Hack" Parser (We will make this real later)
                // We know that RESP sends the command keyword on its own line.
                // If we see "PING", we reply immediately.
                if (line.equals("PING")) {
                    String response = "+PONG\r\n";
                    writer.write(response.getBytes());
                    writer.flush(); // Force push the data immediately
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected.");
            e.printStackTrace();
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}