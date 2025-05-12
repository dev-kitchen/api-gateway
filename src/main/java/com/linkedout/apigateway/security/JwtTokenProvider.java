package com.linkedout.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JWT 토큰 생성 및 검증, 인증 정보를 추출하는 유틸리티 클래스.
 *
 * <p>이 클래스는 애플리케이션 설정에 정의된 시크릿 키와 토큰 유효 기간을 사용하여 JWT 토큰을 생성하고, 토큰의 유효성을 검사하거나 토큰에 포함된 정보를 기반으로 인증
 * 객체를 생성하는 기능을 제공합니다. Spring 컴포넌트로 등록되어 DI를 통해 사용 가능합니다.
 */
@Component
public class JwtTokenProvider {

  private final SecretKey secretKey;
  private final long validityInMilliseconds;

  /**
   * 주어진 시크릿 키와 토큰 유효시간으로 JwtTokenProvider 인스턴스를 생성합니다.
   *
   * @param secretKeyString 애플리케이션 프로퍼티에서 가져온 문자열 형식의 시크릿 키
   * @param validityInMilliseconds 애플리케이션 프로퍼티에서 가져온 JWT 토큰의 유효시간(밀리초)
   */
  public JwtTokenProvider(
      @Value("${jwt.secret}") String secretKeyString,
      @Value("${jwt.expiration}") long validityInMilliseconds) {
    this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
    this.validityInMilliseconds = validityInMilliseconds;
  }

  /**
   * JWT 토큰의 서명을 검증하고 만료일을 확인하여 토큰의 유효성을 검사합니다.
   *
   * @param token 검증할 JWT 토큰
   * @return 토큰이 유효하고 만료되지 않았으면 {@code true}, 그렇지 않으면 {@code false}
   */
  public boolean validateToken(String token) {
    try {
      Claims claims =
          Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();

      return !claims.getExpiration().before(new Date());
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * JWT 토큰에서 인증 정보를 추출하여 Authentication 객체를 생성합니다. 이 메서드는 토큰의 클레임에서 사용자 이름과 역할을 추출하여 인증된 사용자를 나타내는
   * 인증 객체를 생성합니다.
   *
   * @param token 사용자의 인증 정보가 포함된 JWT 토큰
   * @return 토큰으로 인증된 사용자를 나타내는 Authentication 객체
   */
  public Authentication getAuthentication(String token) {
    Claims claims =
        Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();

    String accountId = claims.getSubject();

    // roles 클레임에서 권한 정보 추출
    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    if (claims.containsKey("roles")) {
      @SuppressWarnings("unchecked")
      List<String> roles = (List<String>) claims.get("roles", List.class);
      if (roles != null) {
        authorities = roles.stream().map(SimpleGrantedAuthority::new).toList();
      }
    }

    Map<String, Object> accountDetails = new HashMap<>();

    if (claims.containsKey("email")) {
      accountDetails.put("email", claims.get("email"));
    }
    if (claims.containsKey("name")) {
      accountDetails.put("name", claims.get("name"));
    }
    if (claims.containsKey("accountId")) {
      accountDetails.put("accountId", claims.get("accountId"));
    }

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(accountId, token, authorities);

    // Details에 사용자 정보 추가
    authentication.setDetails(accountDetails);

    return authentication;
  }
}
