package org.programming.utkarsh;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final RespDecoder respDecoder;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.respDecoder = new RespDecoder();
    }

    @Override
    public void run() {
        try {
            OutputStream writer = clientSocket.getOutputStream();

            while (true) {
                // 1. Decode the command
                List<String> command = respDecoder.decode(clientSocket.getInputStream());
                if (command == null) break;

                System.out.println("Received Command: " + command);

                // 2. Dispatch the command
                String cmdName = command.get(0).toUpperCase();

                switch (cmdName) {
                    case "PING":
                        writer.write("+PONG\r\n".getBytes());
                        break;

                    case "ECHO":
                        if (command.size() < 2) {
                            writer.write("-ERR wrong number of arguments for 'echo' command\r\n".getBytes());
                        } else {
                            String message = command.get(1);
                            // Construct the Bulk String response: $length\r\nmessage\r\n
                            String response = "$" + message.length() + "\r\n" + message + "\r\n";
                            writer.write(response.getBytes());
                        }
                        break;

                    default:
                        writer.write("-ERR unknown command\r\n".getBytes());
                        break;
                }

                // 3. IMPORTANT: Always flush to push bytes to the client immediately
                writer.flush();
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