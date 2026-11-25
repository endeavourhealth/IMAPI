package org.endeavourhealth.imapi.security;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.stream.Stream;

/**
 * Casdoor connection settings, read from the environment.
 *
 * @param url              Base URL of Casdoor (https), e.g. https://auth.example.org
 * @param issuer           Expected `iss` of access tokens; defaults to {@code url}
 * @param organisation     Casdoor organisation that owns the users
 * @param clientId         Client id of the Casdoor application IMAPI uses for management API calls
 * @param clientSecret     Client secret of that application
 * @param enforcerId       Casdoor enforcer (owner/name) consulted for authorisation
 * @param appClientIds     Client ids of the named frontend applications, keyed by normalised app name (see {@link #appKey}), read from
 *                         {@code CASDOOR_APP_<NAME>_CLIENT_ID}
 * @param allowedClientIds Client ids (token audiences) of the applications whose tokens IMAPI accepts: every {@code appClientIds} value
 *                         plus {@code CASDOOR_ALLOWED_CLIENT_IDS}
 * @param jwsAlgorithm     Algorithm access tokens are signed with
 */
public record CasdoorSettings(
  String url,
  String issuer,
  String organisation,
  String clientId,
  String clientSecret,
  String enforcerId,
  Map<String, String> appClientIds,
  List<String> allowedClientIds,
  String jwsAlgorithm
) {
  static final String APP_PREFIX = "CASDOOR_APP_";
  static final String APP_SUFFIX = "_CLIENT_ID";

  public record AppAuthConfig(String url, String clientId) {
  }

  public static CasdoorSettings fromEnvironment() {
    return from(System.getenv());
  }

  static CasdoorSettings from(Map<String, String> env) {
    String url = stripTrailingSlash(env.get("CASDOOR_URL"));
    Map<String, String> appClientIds = appClientIds(env);
    List<String> allowedClientIds = Stream.concat(appClientIds.values().stream(), split(env.get("CASDOOR_ALLOWED_CLIENT_IDS")).stream()).distinct().toList();
    return new CasdoorSettings(
      url,
      Optional.ofNullable(stripTrailingSlash(env.get("CASDOOR_ISSUER"))).orElse(url),
      env.get("CASDOOR_ORGANISATION_NAME"),
      env.get("CASDOOR_CLIENT_ID"),
      env.get("CASDOOR_CLIENT_SECRET"),
      env.get("CASDOOR_ENFORCER_ID"),
      appClientIds,
      allowedClientIds,
      Optional.ofNullable(env.get("CASDOOR_JWS_ALGORITHM")).filter(s -> !s.isBlank()).orElse("RS256")
    );
  }

  /** @throws IllegalStateException naming the first missing or invalid setting */
  public CasdoorSettings requireConfigured() {
    require("CASDOOR_URL", url);
    require("CASDOOR_ORGANISATION_NAME", organisation);
    require("CASDOOR_CLIENT_ID", clientId);
    require("CASDOOR_CLIENT_SECRET", clientSecret);
    require("CASDOOR_ENFORCER_ID", enforcerId);
    if (allowedClientIds.isEmpty())
      throw new IllegalStateException("No accepted client ids: set CASDOOR_APP_<NAME>_CLIENT_ID or CASDOOR_ALLOWED_CLIENT_IDS");
    appClientIds.forEach((app, id) -> {
      if (id.equals(clientId))
        throw new IllegalStateException(APP_PREFIX + app + APP_SUFFIX + " must not be IMAPI's own CASDOOR_CLIENT_ID");
    });
    return this;
  }

  public Optional<AppAuthConfig> appAuthConfig(String app) {
    if (url == null || app == null) return Optional.empty();
    return Optional.ofNullable(appClientIds.get(appKey(app)))
      .filter(id -> !id.equals(clientId))
      .map(id -> new AppAuthConfig(url, id));
  }

  public String jwkSetUri() {
    return url + "/.well-known/jwks";
  }

  public String profileUrl() {
    return url + "/account";
  }

  static String appKey(String app) {
    return app.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_|_$", "");
  }

  private static Map<String, String> appClientIds(Map<String, String> env) {
    Map<String, String> result = new HashMap<>();
    env.forEach((name, value) -> {
      if (!name.startsWith(APP_PREFIX) || !name.endsWith(APP_SUFFIX) || name.length() <= APP_PREFIX.length() + APP_SUFFIX.length()) return;
      if (value == null || value.isBlank()) return;
      String app = appKey(name.substring(APP_PREFIX.length(), name.length() - APP_SUFFIX.length()));
      if (!app.isEmpty()) result.put(app, value.trim());
    });
    return Map.copyOf(result);
  }

  private static void require(String name, String value) {
    if (value == null || value.isBlank()) throw new IllegalStateException(name + " is not set");
  }

  private static String stripTrailingSlash(String value) {
    if (value == null || value.isBlank()) return null;
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }

  private static List<String> split(String value) {
    if (value == null || value.isBlank()) return List.of();
    return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }
}
