package org.endeavourhealth.imapi.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.endeavourhealth.imapi.config.ConfigManager;
import org.endeavourhealth.imapi.security.CasdoorSettings;
import org.endeavourhealth.imapi.utility.MetricsHelper;
import org.endeavourhealth.imapi.utility.MetricsTimer;
import org.endeavourhealth.imapi.vocabulary.CONFIG;
import org.endeavourhealth.imapi.vocabulary.NAMESPACE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

@RestController
@RequestMapping("api/config/public")
@CrossOrigin(origins = "*")
@Tag(name = "ConfigController")
@RequestScope
@Slf4j
public class ConfigController {

  @Autowired
  ConfigManager configManager;

  // Read on each request like other Casdoor settings. Replaceable in tests.
  Supplier<CasdoorSettings> casdoorSettings = CasdoorSettings::fromEnvironment;

  @GetMapping(value = "/monitoring")
  @Operation(
    summary = "Retrieve monitoring configuration",
    description = "Fetches monitoring configuration details from the config manager"
  )
  public String getMonitoring() throws Exception {
    try (MetricsTimer t = MetricsHelper.recordTime("API.Config.Monitoring.GET")) {
      log.debug("getMonitoring");
      return configManager.getConfig(CONFIG.MONITORING, new TypeReference<>() {
      });
    }
  }

  @GetMapping(value = "/namespaces")
  @Operation(summary = "Get the list of available namespaces")
  public List<NAMESPACE> getNamespaces() {
    try (MetricsTimer t = MetricsHelper.recordTime("API.Config.Namespaces.GET")) {
      log.debug("getNamespaces");
      List<NAMESPACE> namespaces = new ArrayList<>(EnumSet.allOf(NAMESPACE.class));
      return namespaces;
    }
  }

  @GetMapping(value = "/auth")
  @Operation(summary = "Get app sign-in settings",
    description = "Returns CASDOOR_URL and the client id in CASDOOR_APP_<APP>_CLIENT_ID. clientId is null if Casdoor/app not configured.")
  public CasdoorSettings.AppAuthConfig getAuth(@RequestParam(name = "app") String app) {
    try (MetricsTimer t = MetricsHelper.recordTime("API.Config.Auth.GET")) {
      log.debug("getAuth");
      return casdoorSettings.get().appAuthConfig(app);
    }
  }
}
