package com.duoc.bbfmobile.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Convierte "roles" (CSV) en GrantedAuthority y agrega una autoridad
 * sintetica CANAL_&lt;canal&gt;. SecurityConfig exige CANAL_MOBILE: un JWT
 * valido de otro canal queda autenticado pero sin esa autoridad -&gt; 403.
 */
@Component
public class CanalGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        String roles = jwt.getClaimAsString("roles");
        if (roles != null && !roles.isBlank()) {
            for (String role : roles.split(",")) {
                if (!role.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(role.trim()));
                }
            }
        }
        String canal = jwt.getClaimAsString("canal");
        if (canal != null && !canal.isBlank()) {
            authorities.add(new SimpleGrantedAuthority("CANAL_" + canal.trim().toUpperCase()));
        }
        return authorities;
    }
}
