package com.duoc.bbfweb.dto;

public record LoginResponse(String token, long expiresInSeconds) {
}
