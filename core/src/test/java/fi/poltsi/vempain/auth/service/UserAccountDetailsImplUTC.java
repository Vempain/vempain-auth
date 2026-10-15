package fi.poltsi.vempain.auth.service;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.tools.TestUTCTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAccountDetailsImplUTC {

    @Test
    void testEqualsOk() {
        var unit = TestUTCTools.generateUnit(1);
        var userDetails1 = new UserDetailsImpl(1L, "login", "nick", "email@address", "password", Set.of(unit), null);
        var userDetails2 = new UserDetailsImpl(1L, "login", "nick", "email@address", "password", Set.of(unit), null);
        boolean equals = userDetails1.equals(userDetails2);
        assertTrue(equals);
    }

    @Test
    void hashCodeOk() {
        var userDetails1 = new UserDetailsImpl(1L, "login", "nick", "email@address", "password", null, null);
        assertTrue(userDetails1.hashCode() > 0);
    }

    @Test
    void buildWithoutUnitsHasNoAuthorities() {
        var account = UserAccount.builder()
            .id(3L)
            .loginName("login")
            .nick("nick")
            .email("email@address")
            .password("password")
            .build();

        var details = UserDetailsImpl.build(account, null);

        assertNull(details.getUnits());
        assertTrue(details.getAuthorities()
            .isEmpty());
        assertEquals("login", details.getUsername());
        assertEquals("nick", details.getNickName());
        assertEquals("email@address", details.getEmail());
        assertEquals("password", details.getPassword());
        assertTrue(details.isAccountNonExpired());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isCredentialsNonExpired());
        assertTrue(details.isEnabled());
    }

    @Test
    void buildFromAccountExposesItsDirectUnitsAsAuthorities() {
        var unit = TestUTCTools.generateUnit(1);
        var account = UserAccount.builder()
            .id(3L)
            .loginName("login")
            .units(Set.of(unit))
            .build();

        var details = UserDetailsImpl.build(account);

        assertEquals(Set.of(unit), details.getUnits());
        assertEquals(List.of("Test unit 1"), details.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .toList());
    }

    @Test
    void equalsComparesTheIdOnly() {
        var details = new UserDetailsImpl(1L, "login", "nick", "email@address", "password", Set.of(), null);

        assertEquals(details, details);
        assertNotEquals(details, null);
        assertNotEquals(details, "not a principal");
        assertNotEquals(details, new UserDetailsImpl(2L, "login", "nick", "email@address", "password", Set.of(), null));
        assertEquals(details, new UserDetailsImpl(1L, "other", "other", "other@address", "other", null, null));
    }
}
