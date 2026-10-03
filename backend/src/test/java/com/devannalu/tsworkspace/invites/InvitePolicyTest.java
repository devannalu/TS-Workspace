package com.devannalu.tsworkspace.invites;

import com.devannalu.tsworkspace.auth.EmailNormalizer;
import com.devannalu.tsworkspace.common.DomainProblem;
import java.time.Instant;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class InvitePolicyTest {
    @Test void secureTokensAre32BytesHexAndDistinct() {
        var tokens=new HashSet<String>();
        for(int i=0;i<100;i++){String token=InvitePolicy.token();assertThat(token).matches("[0-9a-f]{64}");tokens.add(token);}
        assertThat(tokens).hasSize(100);
    }
    @Test void hashMatchesIndependentSha256KnownVector() {
        assertThat(InvitePolicy.hash("abc")).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
    @Test void usedWinsOverCancelledAndExpired() {
        assertThat(InvitePolicy.status(Instant.EPOCH,Instant.EPOCH,Instant.EPOCH,Instant.now())).isEqualTo(InvitePolicy.Status.USED);
    }
    @Test void cancelledWinsOverExpired() {
        assertThat(InvitePolicy.status(null,Instant.EPOCH,Instant.EPOCH,Instant.now())).isEqualTo(InvitePolicy.Status.CANCELLED);
    }
    @Test void expirationBoundaryAndPending() {
        Instant now=Instant.now();
        assertThat(InvitePolicy.status(null,null,now,now)).isEqualTo(InvitePolicy.Status.EXPIRED);
        assertThat(InvitePolicy.status(null,null,now.plusSeconds(1),now)).isEqualTo(InvitePolicy.Status.PENDING);
        assertThat(InvitePolicy.TTL_DAYS).isEqualTo(7);
    }
    @Test void terminalStatesAreRejectedWithSamePublicMessage() {
        Instant now=Instant.now();
        assertThatThrownBy(()->InvitePolicy.pending(now,null,now.plusSeconds(1),now)).isInstanceOf(DomainProblem.class).hasMessage("Convite inválido ou indisponível.");
        assertThatThrownBy(()->InvitePolicy.pending(null,now,now.plusSeconds(1),now)).hasMessage("Convite inválido ou indisponível.");
        assertThatThrownBy(()->InvitePolicy.pending(null,null,now,now)).hasMessage("Convite inválido ou indisponível.");
    }
    @Test void formatRejectsMalformedTokens() {
        assertThat(InvitePolicy.validFormat(null)).isFalse();assertThat(InvitePolicy.validFormat("invalid")).isFalse();
        assertThat(InvitePolicy.validFormat("a".repeat(64))).isTrue();
    }
    @Test void normalizedEmailUsesTrimAndRootLocale() {
        assertThat(EmailNormalizer.normalize(" MEMBER@Example.TEST ")).isEqualTo("member@example.test");
        assertThatThrownBy(()->EmailNormalizer.normalize(" ")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void acceptanceValidatesNameLengthPasswordAndConfirmation() {
        String password="Testing password 2026";
        assertThatCode(()->InvitePolicy.acceptance(" Test ",password,password)).doesNotThrowAnyException();
        assertThatThrownBy(()->InvitePolicy.acceptance(" a ",password,password)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->InvitePolicy.acceptance("Test","short","short")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->InvitePolicy.acceptance("Test",password,"different")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->InvitePolicy.acceptance("Test","é".repeat(40),"é".repeat(40))).isInstanceOf(IllegalArgumentException.class);
    }
}
