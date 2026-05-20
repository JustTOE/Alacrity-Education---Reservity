package dev.tmmc.reservity.common.storage;

public record StoredObject(String key, String url, String contentType, long size) {
}
