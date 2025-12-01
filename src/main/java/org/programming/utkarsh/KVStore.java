package org.programming.utkarsh;
import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class KVStore {

    // 1. Create a minimal class to hold Value + Time
    // We use a private inner class because no one else needs to see this.
    private static class DatabaseEntry {

        // Add a version ID to prevent errors if class changes
        private static final long serialVersionUID = 1L;

        Object value;
        long expiryTime; // The exact timestamp (in ms) when this key dies. -1 means "never dies".

        public DatabaseEntry(Object value) {
            this.value = value;
            this.expiryTime = -1; // Default: No expiry
        }
    }

    // 2. Change the Map to store this Entry object instead of just String
    private ConcurrentHashMap<String, DatabaseEntry> store = new ConcurrentHashMap<>();

    // Standard SET (No expiry)
    public void set(String key, String value) {
        store.put(key, new DatabaseEntry(value));
    }

    // NEW: SET with Expiry (Helper for later if needed)
    public void set(String key, String value, long seconds) {
        DatabaseEntry entry = new DatabaseEntry(value);
        entry.expiryTime = System.currentTimeMillis() + (seconds * 1000);
        store.put(key, entry);
    }

    // NEW: The EXPIRE command logic
    // Returns 1 if set, 0 if key didn't exist
    public int expire(String key, long seconds) {
        DatabaseEntry entry = store.get(key);
        if (entry == null) {
            return 0;
        }

        // Set the absolute time when it should die
        entry.expiryTime = System.currentTimeMillis() + (seconds * 1000);
        return 1;
    }

    // NEW: LPUSH (Push to head)
    public int lpush(String key, String... args) {
        // We use compute to handle concurrent updates safely
        DatabaseEntry entry = store.compute(key, (k, v) -> {
            if (v == null) {
                // Create new list
                LinkedList<String> list = new LinkedList<>();
                for (String arg : args) list.addFirst(arg);
                return new DatabaseEntry(list);
            } else {
                // Update existing list
                if (!(v.value instanceof List)) {
                    throw new RuntimeException("WRONGTYPE Operation against a key holding the wrong kind of value");
                }
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) v.value;
                for (String arg : args) list.addFirst(arg);
                return v;
            }
        });

        // Return size of list
        return ((List<?>) entry.value).size();
    }

    // NEW: LRANGE (Get sublist)
    public List<String> lrange(String key, int start, int stop) {
        DatabaseEntry entry = store.get(key);
        if (entry == null) return java.util.Collections.emptyList();

        if (!(entry.value instanceof List)) {
            throw new RuntimeException("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) entry.value;
        int size = list.size();

        // Handle negative indices (Standard Redis logic)
        // -1 means last element, -2 means second to last
        if (start < 0) start = size + start;
        if (stop < 0) stop = size + stop;

        // Clamp values to valid range
        if (start < 0) start = 0;
        if (stop >= size) stop = size - 1;
        if (start > stop) return java.util.Collections.emptyList();

        return list.subList(start, stop + 1);
    }

    // UPDATED: GET with "Lazy Expiration" check
    public String get(String key) {
        DatabaseEntry entry = store.get(key);
        if (entry == null) return null;

        // Check if it is expired
        if (entry.expiryTime != -1 && System.currentTimeMillis() > entry.expiryTime) {
            // It is dead. Delete it now.
            store.remove(key);
            return null;
        }

        return entry.value.toString();
    }

    public void saveToFile() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("redis.data"))) {
            out.writeObject(store);
            System.out.println("✅ Data saved to redis.data");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // NEW: Load the HashMap from a file
    @SuppressWarnings("unchecked")
    public void loadFromFile() {
        File file = new File("redis.data");
        if (!file.exists()) {
            return; // No existing data, start fresh
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            store = (ConcurrentHashMap<String, DatabaseEntry>) in.readObject();
            System.out.println("✅ Data loaded from redis.data");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("⚠️ Could not load data: " + e.getMessage());
        }
    }
}