package com.chatqueue.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

/**
 * JWT utility using JJWT 0.11.5.
 * For demo only. Move the secret to env or application.yml in production.
 */
public class JWTUtil {

    // Use a BASE64-encoded 256-bit key (>= 32 bytes before base64)
    // Generate a new one for production!
    private static final String BASE64_SECRET = "S0pNV2ZkM0RybjVSWU1oZ0NwU1JtV2p1a0JMR3dFQWZURm1qV1ZQbU1qS0E=";

    private static final Key KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(BASE64_SECRET));
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000L; // 24h

    public static String generateToken(String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + EXPIRATION_MS))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static String extractSubject(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(KEY).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
