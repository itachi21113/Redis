package org.programming.utkarsh;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final RespDecoder respDecoder; // Add the decoder

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.respDecoder = new RespDecoder(); // Initialize it
    }

    @Override
    public void run() {
        try {
            OutputStream writer = clientSocket.getOutputStream();

            while (true) {
                // 1. Parse the input into a list of strings
                // This blocks until a full command is received
                List<String> command = respDecoder.decode(clientSocket.getInputStream());

                if (command == null) break; // Client disconnected

                System.out.println("Received Command: " + command);

                // 2. Handle the command (Phase 2 logic)
                if (command.size() > 0 && command.get(0).equalsIgnoreCase("PING")) {
                    writer.write("+PONG\r\n".getBytes());
                    writer.flush();
                } else {
                    // Start handling ECHO or unknown commands
                    writer.write("-ERR unknown command\r\n".getBytes());
                    writer.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected.");
        } catch (RuntimeException e) {
            System.err.println("Protocol Error: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}