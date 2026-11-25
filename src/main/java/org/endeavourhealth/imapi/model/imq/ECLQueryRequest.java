package org.endeavourhealth.imapi.model.imq;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.endeavourhealth.imapi.model.tripletree.TTIriRef;

import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor
public class ECLQueryRequest {

  private String ecl;
  private Query query;
  private boolean showNames;
  private ECLStatus status;
  private boolean includeLegacy;
  private int limit = 5000;
  private Set<TTIriRef> statusFilter = new HashSet<>();
  private int page;
  private int size;
  private boolean includeInactive;
  private Set<TTIriRef> schemeFilter = new HashSet<>();

  public Set<TTIriRef> getSchemeFilter() {
    return schemeFilter;
  }

  public ECLQueryRequest setSchemeFilter(Set<TTIriRef> schemeFilter) {
    this.schemeFilter = schemeFilter;
    return this;
  }

  public ECLQueryRequest addSchemeFilter(TTIriRef schemeFilter) {
    if (this.schemeFilter==null)
      this.schemeFilter = new HashSet<>();
    this.schemeFilter.add(schemeFilter);
    return this;
  }


  public ECLQueryRequest setIncludeInactive(boolean includeInactive) {
    this.includeInactive = includeInactive;
    return this;
  }

  public boolean isIncludeInactive() {
    return includeInactive;
  }

  public int getLimit() {
    return limit;
  }

  public ECLQueryRequest setLimit(int limit) {
    this.limit = limit;
    return this;
  }

  public Set<TTIriRef> getStatusFilter() {
    return statusFilter;
  }

  public ECLQueryRequest setStatusFilter(Set<TTIriRef> statusFilter) {
    this.statusFilter = statusFilter;
    return this;
  }

  public int getPage() {
    return page;
  }

  public ECLQueryRequest setPage(int page) {
    this.page = page;
    return this;
  }

  public int getSize() {
    return size;
  }

  public ECLQueryRequest setSize(int size) {
    this.size = size;
    return this;
  }

  public boolean isIncludeLegacy() {
    return includeLegacy;
  }

  public ECLQueryRequest setIncludeLegacy(boolean includeLegacy) {
    this.includeLegacy = includeLegacy;
    return this;
  }

  public ECLQueryRequest setEcl(String ecl) {
    this.ecl = ecl;
    return this;
  }

  public ECLQueryRequest setQuery(Query query) {
    this.query = query;
    return this;
  }

  public ECLQueryRequest setShowNames(boolean showNames) {
    this.showNames = showNames;
    return this;
  }

  public ECLQueryRequest setStatus(ECLStatus status) {
    this.status = status;
    return this;
  }
}
