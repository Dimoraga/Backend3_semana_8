package com.duoc.bbfcajeros.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Convierte el claim "roles" (CSV) en GrantedAuthority normales y agrega
 * una autoridad sintetica CANAL_&lt;valor-del-claim-canal&gt;. La regla de
 * autorizacion en SecurityConfig exige CANAL_CAJERO: un JWT valido pero
 * emitido para otro canal (ej. MOBILE) queda autenticado igual, solo que
 * sin esa autoridad -&gt; 403, no 401. Asi se cumple el requisito de auth
 * especifica por canal de forma verificable (firma valida != autorizado
 * en este canal).
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
