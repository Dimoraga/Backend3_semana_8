package com.duoc.bbfmobile.dto;

public record LoginResponse(String accessToken, String refreshToken, long expiresInSeconds) {
}
