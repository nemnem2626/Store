package com.poly.asm.entitys;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User, OidcUser {

    private final Object principal; // Có thể là OAuth2User hoặc OidcUser
    private final User user;
    private OidcUser oidcUser;

    public CustomOAuth2User(Object principal, User user) {
        this.principal = principal;
        this.user = user;
        if (principal instanceof OidcUser) {
            this.oidcUser = (OidcUser) principal;
        }
    }

    @Override
    public Map<String, Object> getAttributes() {
        if (principal instanceof OAuth2User) {
            return ((OAuth2User) principal).getAttributes();
        } else if (oidcUser != null) {
            return oidcUser.getAttributes();
        }
        return Collections.emptyMap();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    @Override
    public String getName() {
        if (user.getFullname() != null) {
            return user.getFullname();
        } else if (oidcUser != null) {
            return oidcUser.getFullName();
        } else if (principal instanceof OAuth2User) {
            return ((OAuth2User) principal).getAttribute("name");
        }
        return user.getUsername();
    }

    public User getUser() {
        return user;
    }

    // Các phương thức OidcUser
    @Override
    public OidcIdToken getIdToken() {
        return oidcUser != null ? oidcUser.getIdToken() : null;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return oidcUser != null ? oidcUser.getUserInfo() : null;
    }

    @Override
    public Map<String, Object> getClaims() {
        return oidcUser != null ? oidcUser.getClaims() : Collections.emptyMap();
    }
}