package org.endeavourhealth.imapi.security;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CasdoorSettingsTest {
  private static final Map<String, String> FULL = Map.of(
    "CASDOOR_URL", "https://auth.example.org/",
    "CASDOOR_ORGANISATION_NAME", "Endeavour",
    "CASDOOR_CLIENT_ID", "id",
    "CASDOOR_CLIENT_SECRET", "secret",
    "CASDOOR_ENFORCER_ID", "Endeavour/TestEnforcer",
    "CASDOOR_ALLOWED_CLIENT_IDS", "a, b ,,c"
  );

  @Test
  void readsSettingsAndNormalises() {
    CasdoorSettings settings = CasdoorSettings.from(FULL);
    assertEquals("https://auth.example.org", settings.url());
    assertEquals("https://auth.example.org", settings.issuer(), "issuer defaults to the url");
    assertEquals(java.util.List.of("a", "b", "c"), settings.allowedClientIds());
    assertEquals("RS256", settings.jwsAlgorithm());
    assertEquals("https://auth.example.org/.well-known/jwks", settings.jwkSetUri());
    assertEquals("https://auth.example.org/account", settings.profileUrl());
    assertSame(settings, settings.requireConfigured());
  }

  @Test
  void issuerCanDifferFromTheUrl() {
    var env = new java.util.HashMap<>(FULL);
    env.put("CASDOOR_ISSUER", "https://public.example.org");
    assertEquals("https://public.example.org", CasdoorSettings.from(env).issuer());
  }

  @Test
  void namesTheMissingSetting() {
    var env = new java.util.HashMap<>(FULL);
    env.remove("CASDOOR_ENFORCER_ID");
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> CasdoorSettings.from(env).requireConfigured());
    assertTrue(e.getMessage().contains("CASDOOR_ENFORCER_ID"));
  }

  @Test
  void requiresAtLeastOneAcceptedClient() {
    var env = new java.util.HashMap<>(FULL);
    env.remove("CASDOOR_ALLOWED_CLIENT_IDS");
    assertThrows(IllegalStateException.class, () -> CasdoorSettings.from(env).requireConfigured());
  }

  @Test
  void neverReturnsImapisOwnClientId() {
    var env = new java.util.HashMap<>(FULL);
    env.put("CASDOOR_APP_IMAPI_CLIENT_ID", "id"); // the same as CASDOOR_CLIENT_ID
    CasdoorSettings settings = CasdoorSettings.from(env);
    assertNull(settings.appAuthConfig("IMAPI").clientId());
    assertNull(settings.appAuthConfig("CLIENT").clientId(), "CASDOOR_CLIENT_ID is not an app variable");
    IllegalStateException e = assertThrows(IllegalStateException.class, settings::requireConfigured);
    assertTrue(e.getMessage().contains("CASDOOR_APP_IMAPI_CLIENT_ID"));
  }

  @Test
  void noAppConfigWithoutACasdoorUrl() {
    var env = new java.util.HashMap<>(FULL);
    env.remove("CASDOOR_URL");
    env.put("CASDOOR_APP_IMDIRECTORY_CLIENT_ID", "12345");
    assertNull(CasdoorSettings.from(env).appAuthConfig("IMDirectory").clientId());
  }

  @Test
  void returnsTheAppsClientIdWithTheCasdoorUrl() {
    var env = new java.util.HashMap<>(FULL);
    env.put("CASDOOR_APP_IMDIRECTORY_CLIENT_ID", "12345");
    CasdoorSettings settings = CasdoorSettings.from(env);
    assertEquals(new CasdoorSettings.AppAuthConfig("https://auth.example.org", "12345"), settings.appAuthConfig("IMDirectory"));
    assertEquals(new CasdoorSettings.AppAuthConfig("https://auth.example.org", null), settings.appAuthConfig("Unknown"),
      "an unconfigured app gets the URL but no client id, so sign-in is off");
  }

  @Test
  void writesANullClientIdRatherThanLeavingItOut() throws Exception {
    String json = new com.fasterxml.jackson.databind.ObjectMapper()
      .setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
      .writeValueAsString(new CasdoorSettings.AppAuthConfig("https://auth.example.org", null));
    assertEquals("{\"url\":\"https://auth.example.org\",\"clientId\":null}", json);
  }
}
