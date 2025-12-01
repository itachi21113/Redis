package org.programming.utkarsh;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RespDecoder {

    // Helper: Reads until \r\n and returns the number as int
    private int readInteger(InputStream in) throws IOException {
        int count = 0;
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            char c = (char) b;
            if (c == '\r') {
                in.read(); // Skip \n
                break;
            }
            sb.append(c);
        }
        return Integer.parseInt(sb.toString());
    }

    // --- NEW METHOD ---
    // Helper: Just consumes the \r\n without parsing anything
    private void readCRLF(InputStream in) throws IOException {
        int b1 = in.read();
        int b2 = in.read();

        if (b1 != '\r' || b2 != '\n') {
            throw new RuntimeException("Protocol Error: Expected CRLF (\\r\\n)");
        }
    }

    private String decodeBulkString(InputStream in) throws IOException {
        int length = readInteger(in);
        byte[] bytes = new byte[length];

        int bytesRead = 0;
        while (bytesRead < length) {
            bytesRead += in.read(bytes, bytesRead, length - bytesRead);
        }

        String result = new String(bytes);

        // --- FIX IS HERE ---
        // Instead of readInteger(), we call our new safe skipper
        readCRLF(in);

        return result;
    }

    public List<String> decode(InputStream in) throws IOException {
        int b = in.read();
        if (b == -1) return null;

        char type = (char) b;
        if (type != '*') {
            throw new RuntimeException("Unknown RESP type: " + type);
        }

        int count = readInteger(in);
        List<String> command = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int nextByte = in.read();
            if (nextByte != '$') {
                throw new RuntimeException("Expected '$', got " + (char)nextByte);
            }
            command.add(decodeBulkString(in));
        }
        return command;
    }
}