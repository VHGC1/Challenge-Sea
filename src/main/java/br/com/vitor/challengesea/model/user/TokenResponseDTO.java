package br.com.vitor.challengesea.model.user;

public record TokenResponseDTO(String accessToken, String tokenType, long expiresIn) {}
