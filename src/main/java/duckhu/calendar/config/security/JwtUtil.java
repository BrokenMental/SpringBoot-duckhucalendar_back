package duckhu.calendar.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 기본값 24시간
    private Long expiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        // secret을 그대로 byte[]로 변환하여 SecretKey 생성
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        System.out.println("JWT Secret 초기화: 성공");
        System.out.println("Secret 길이: " + secret.length());
        System.out.println("만료 시간: " + expiration + "ms (" + (expiration / 1000 / 60) + "분)");
    }

    /**
     * 토큰 검증
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expDate = claims.getExpiration();
            Date now = new Date();

            return !expDate.before(now);
        } catch (Exception e) {
            System.out.println("토큰 검증 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 토큰에서 이메일 추출
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * 토큰에서 Role 추출
     */
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return (String) claims.get("role");
    }

    /**
     * 토큰이 관리자 토큰인지 확인
     */
    public boolean isAdminToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String email = claims.getSubject();
            String role = (String) claims.get("role");

            return "ADMIN".equals(role) || "nukabyte@gmail.com".equals(email);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 토큰 생성 - role을 내부에서 설정
     */
    public String generateToken(String email) {
        return generateToken(email, "ADMIN");
    }

    /**
     * 토큰 생성 - role을 파라미터로 받는 오버로드 메서드
     */
    public String generateToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("email", email);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        System.out.println("토큰 생성 - 현재: " + now);
        System.out.println("토큰 생성 - 만료: " + expiryDate);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
