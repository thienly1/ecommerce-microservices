// Long version with client roles required, go to check the short version for only realm roles in product-service and order-service
package com.ecommerce.user_service.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Realm roles
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            addRoles(authorities, getRolesFromMap(realmAccess));
        }

        // Client roles
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            resourceAccess.values().forEach(access -> {
                if (access instanceof Map<?, ?> clientAccess) {
                    addRoles(authorities, getRolesFromMap(clientAccess));
                }
            });
        }

        return authorities;
    }
    // Safely extracts roles from a Keycloak role map

    private List<String> getRolesFromMap(Map<?, ?> accessMap) {
        Object roles = accessMap.get("roles");
        if (roles instanceof List<?>) {
            return ((List<?>) roles).stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    // Converts role names into Spring Security authorities
    private void addRoles(Set<GrantedAuthority> authorities, List<String> roles) {
        authorities.addAll(
                roles.stream()
                        .map(role -> new SimpleGrantedAuthority(
                                ROLE_PREFIX + role.toUpperCase(Locale.ROOT)
                        ))
                        .collect(Collectors.toSet())
        );
    }
}
