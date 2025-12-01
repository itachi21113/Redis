package org.programming.utkarsh;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RespDecoder {

    // Helper method: Reads bytes until it sees '\r\n' and returns the number inside.
    // Example: Input stream has ":500\r\n" -> returns 500
    private int readInteger(InputStream in) throws IOException {
        int count = 0;
        StringBuilder sb = new StringBuilder();
        int b;

        // Read byte by byte
        while ((b = in.read()) != -1) {
            char c = (char) b;

            if (c == '\r') {
                // If we see \r, the next byte MUST be \n. Read it and skip it.
                in.read();
                break;
            }
            sb.append(c);
        }

        return Integer.parseInt(sb.toString());
    }

    // Reads a standard Redis string: "$4\r\nPING\r\n" -> returns "PING"
    private String decodeBulkString(InputStream in) throws IOException {
        // 1. Read the length (The number after '$')
        int length = readInteger(in);

        // 2. Allocate a byte array of that exact size
        byte[] bytes = new byte[length];

        // 3. Read the bytes
        int bytesRead = 0;
        while (bytesRead < length) {
            bytesRead += in.read(bytes, bytesRead, length - bytesRead);
        }

        // 4. Convert to String
        String result = new String(bytes);

        // 5. Read the trailing \r\n (Redis always adds this after the string)
        readInteger(in); // We reuse this just to consume the \r\n

        return result;
    }



// ... inside the class ...

    public List<String> decode(InputStream in) throws IOException {
        // 1. Check the first byte. It should be '*'
        int b = in.read();
        if (b == -1) return null; // End of stream

        char type = (char) b;
        if (type != '*') {
            throw new RuntimeException("Unknown RESP type: " + type);
        }

        // 2. Read how many strings are in this command
        int count = readInteger(in);

        // 3. Read each string
        List<String> command = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // Each item in the array is a Bulk String starting with '$'
            int nextByte = in.read(); // Read the '$'
            if (nextByte != '$') {
                throw new RuntimeException("Expected '$', got " + (char)nextByte);
            }

            command.add(decodeBulkString(in));
        }

        return command;
    }
}