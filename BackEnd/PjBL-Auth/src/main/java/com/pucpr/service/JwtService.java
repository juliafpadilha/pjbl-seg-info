package com.pucpr.service;
import com.pucpr.model.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtService {

    // 1. Busca da Chave via Variável de Ambiente
    private String getSecretKey() {
        String key = System.getenv("JWT_SECRET");
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalStateException("ERRO CRÍITICO: Variável de ambiente JWT_SECRET não configurada!");
        }
        return key;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(getSecretKey().getBytes());
    }

    /**
     * Gera o token assinado.
     */
    public String generateToken(Usuario user) {
        //  2. Payload Completo com Claims
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole()) // Role injetada no Base64 do payload
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900000)) // 15 min de Validade
                .signWith(getSigningKey()) // Assinatura protegida usando HS256 com a Secret Key
                .compact();
    }

    /**
     * Extrai o e-mail (subject) do token.
     */
    public String extractEmail(String token) {
        // Usa o builder de parser fortemente tipado com a chave. Rejeita manipulações.
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Valida se o token é autêntico e não expirou.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
                
            return true;
        } catch (Exception e) {
            // Qualquer problema de Expirou, Assinatura, ou 'alg:none', cai neste catch.
            return false;
        }
    }
}
