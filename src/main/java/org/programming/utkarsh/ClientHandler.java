package org.programming.utkarsh;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final KVStore kvStore; // Reference to the shared DB
    private final RespDecoder respDecoder;

    // Update Constructor to accept KVStore
    public ClientHandler(Socket socket, KVStore store) {
        this.clientSocket = socket;
        this.kvStore = store;
        this.respDecoder = new RespDecoder();
    }

    @Override
    public void run() {
        try {
            OutputStream writer = clientSocket.getOutputStream();

            while (true) {
                List<String> command = respDecoder.decode(clientSocket.getInputStream());
                if (command == null) break;

                String cmdName = command.get(0).toUpperCase();

                switch (cmdName) {
                    case "PING":
                        writer.write("+PONG\r\n".getBytes());
                        break;

                    case "ECHO":
                        if (command.size() < 2) {
                            writer.write("-ERR wrong number of arguments\r\n".getBytes());
                        } else {
                            String msg = command.get(1);
                            writer.write(("$" + msg.length() + "\r\n" + msg + "\r\n").getBytes());
                        }
                        break;

                    case "SET":
                        if (command.size() < 3) {
                            writer.write("-ERR wrong number of arguments for 'set'\r\n".getBytes());
                        } else {
                            String key = command.get(1);
                            String value = command.get(2);
                            kvStore.set(key, value);
                            writer.write("+OK\r\n".getBytes()); // Simple String OK
                        }
                        break;



                    case "GET":
                        if (command.size() < 2) {
                            writer.write("-ERR wrong number of arguments for 'get'\r\n".getBytes());
                        } else {
                            String key = command.get(1);
                            String val = kvStore.get(key);

                            if (val == null) {
                                writer.write("$-1\r\n".getBytes()); // Null Bulk String (nil)
                            } else {
                                writer.write(("$" + val.length() + "\r\n" + val + "\r\n").getBytes());
                            }
                        }
                        break;

                    case "EXPIRE":
                        if (command.size() < 3) {
                            writer.write("-ERR wrong number of arguments for 'expire'\r\n".getBytes());
                        } else {
                            String key = command.get(1);
                            try {
                                long seconds = Long.parseLong(command.get(2));
                                int result = kvStore.expire(key, seconds);
                                // Return Integer response (Colon :)
                                writer.write((":" + result + "\r\n").getBytes());
                            } catch (NumberFormatException e) {
                                writer.write("-ERR value is not an integer or out of range\r\n".getBytes());
                            }
                        }
                        break;

                    // ... inside switch(cmdName) ...

                    case "LPUSH":
                        if (command.size() < 3) {
                            writer.write("-ERR wrong number of arguments for 'lpush'\r\n".getBytes());
                        } else {
                            try {
                                String key = command.get(1);
                                // Get all values after the key
                                String[] values = command.subList(2, command.size()).toArray(new String[0]);

                                int size = kvStore.lpush(key, values);
                                writer.write((":" + size + "\r\n").getBytes());
                            } catch (RuntimeException e) {
                                writer.write(("-ERR " + e.getMessage() + "\r\n").getBytes());
                            }
                        }
                        break;

                    case "LRANGE":
                        if (command.size() < 4) {
                            writer.write("-ERR wrong number of arguments for 'lrange'\r\n".getBytes());
                        } else {
                            try {
                                String key = command.get(1);
                                int start = Integer.parseInt(command.get(2));
                                int stop = Integer.parseInt(command.get(3));

                                List<String> result = kvStore.lrange(key, start, stop);

                                // FORMATTING RESP ARRAY
                                StringBuilder sb = new StringBuilder();
                                sb.append("*").append(result.size()).append("\r\n"); // Header *Count

                                for (String val : result) {
                                    sb.append("$").append(val.length()).append("\r\n"); // $Length
                                    sb.append(val).append("\r\n");                      // Value
                                }

                                writer.write(sb.toString().getBytes());

                            } catch (NumberFormatException e) {
                                writer.write("-ERR value is not an integer or out of range\r\n".getBytes());
                            } catch (RuntimeException e) {
                                writer.write(("-ERR " + e.getMessage() + "\r\n").getBytes());
                            }
                        }
                        break;
                    default:
                        writer.write("-ERR unknown command\r\n".getBytes());
                        break;
                }
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