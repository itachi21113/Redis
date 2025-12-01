package org.programming.utkarsh;
import java.util.concurrent.ConcurrentHashMap;

public class KVStore {
    // This is the "RAM" of our Redis
    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    public void set(String key, String value) {
        store.put(key, value);
    }

    public String get(String key) {
        return store.get(key);
    }
}