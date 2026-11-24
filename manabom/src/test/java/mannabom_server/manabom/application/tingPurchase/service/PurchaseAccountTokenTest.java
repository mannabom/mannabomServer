package mannabom_server.manabom.application.tingPurchase.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseAccountTokenTest {

    @Test
    void tokenIsDeterministicUuidPerUser() {
        String token = PurchaseAccountToken.forUser(42L);

        assertThat(token).isEqualTo(PurchaseAccountToken.forUser(42L));
        assertThat(token).isNotEqualTo(PurchaseAccountToken.forUser(43L));
        assertThat(UUID.fromString(token)).isNotNull();
        assertThat(PurchaseAccountToken.matches(42L, token.toUpperCase())).isTrue();
        assertThat(PurchaseAccountToken.matches(43L, token)).isFalse();
        assertThat(PurchaseAccountToken.matches(42L, null)).isFalse();
    }
}
