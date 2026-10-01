package com.devannalu.tsworkspace.rbac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;

class RbacCompatibilityTest {
    static Stream<Arguments> baselineMatrix() throws IOException {
        try (var input = RbacCompatibilityTest.class.getResourceAsStream("/rbac-baseline.json")) {
            JsonNode fixture = new ObjectMapper().readTree(input);
            Set<String> catalog = new HashSet<>();
            fixture.get("permissions").forEach(p -> catalog.add(p.asText()));
            var builder = Stream.<Arguments>builder();
            fixture.get("roles").fields().forEachRemaining(role -> {
                Set<String> expected = new HashSet<>();
                role.getValue().forEach(p -> expected.add(p.asText()));
                for (String permission : catalog) builder.add(Arguments.of(role.getKey(), permission, expected.contains(permission), catalog));
            });
            return builder.build();
        }
    }
    @ParameterizedTest(name = "{0}: {1} = {2}") @MethodSource("baselineMatrix")
    void reproducesEveryLegacyRolePermission(String role, String permission, boolean expected, Set<String> catalog) {
        var context = new PermissionPolicy.Context(true, role, catalog, Set.copyOf(RbacBaseline.GRANTS.get(role)), Map.of());
        assertThat(PermissionPolicy.resolve(context, permission)).isEqualTo(expected);
    }
}
