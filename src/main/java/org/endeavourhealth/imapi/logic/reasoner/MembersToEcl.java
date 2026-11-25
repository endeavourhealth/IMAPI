package org.endeavourhealth.imapi.logic.reasoner;

import org.endeavourhealth.imapi.dataaccess.SetRepository;
import org.endeavourhealth.imapi.logic.service.EntityService;
import org.endeavourhealth.imapi.model.Pageable;
import org.endeavourhealth.imapi.model.imq.ECLQueryRequest;
import org.endeavourhealth.imapi.model.imq.Entail;
import org.endeavourhealth.imapi.model.imq.Node;
import org.endeavourhealth.imapi.model.imq.Query;
import org.endeavourhealth.imapi.model.tripletree.TTEntity;
import org.endeavourhealth.imapi.model.tripletree.TTIriRef;
import org.endeavourhealth.imapi.transforms.IMQToECL;

import java.util.*;
import java.util.stream.Collectors;

public class MembersToEcl {
  private SetRepository setRepository= new SetRepository();

public String getEclFromMembers(String iri) {
  Pageable<Node> memberNodes= setRepository.getMembers(iri,false,1,10000);
  if (memberNodes.getResult()==null){
    return getEclFromSubsets(iri);
  }
  Set<String> members= memberNodes.getResult().stream().map(Node::getIri).collect(Collectors.toSet());
    Set<String> inclusionAncestors = new HashSet<>();
    Set<String> exclusions = new HashSet<>();
    Map<String, Map<String, Set<String>>> level0Parents = setRepository.getLevel0ParentInclusions(iri);
    setRepository.addExclusions(iri, level0Parents);
    for (String parentIri : level0Parents.keySet()) {
      members.remove(parentIri);
      inclusionAncestors.add(parentIri);
      Map<String, Set<String>> InOrOut = level0Parents.get(parentIri);
      if (InOrOut.get("inclusion") != null) {
        for (String include : InOrOut.get("inclusion")) {
          members.remove(include);
        }
      }
      if (InOrOut.get("exclusion") != null) {
        for (String exclude : InOrOut.get("exclusion")) {
          exclusions.add(exclude);
          members.remove(exclude);
        }
      }
    }
    int beforeSize = members.size() + 1;
    while (members.size() != beforeSize) {
      levelN(members,inclusionAncestors,exclusions);
      beforeSize = members.size();
    }
    setRepository.removeInactives(iri,members);
   StringBuilder ecl= new StringBuilder();
    if (!exclusions.isEmpty())
      ecl.append("(");
    int count=-1;
    if (!inclusionAncestors.isEmpty()) {
      for (String ancestor : inclusionAncestors) {
        count++;
        ancestor = ancestor.substring(ancestor.lastIndexOf("#") + 1);
        if (count > 0) ecl.append("OR ");
        ecl.append("<< ").append(ancestor).append("\n");
      }
    }
    count=0;
    for (String member:members){
      member= member.substring(member.lastIndexOf("#")+1);
      count++;
      if (count>1||!inclusionAncestors.isEmpty())
        ecl.append("OR ");
      ecl.append(member).append("\n");
    }
    if (!exclusions.isEmpty()) {
      ecl.append(")\n");
      ecl.append("MINUS (");
      count = -1;
      for (String exclude : exclusions) {
        exclude = exclude.substring(exclude.lastIndexOf("#") + 1);
        count++;
        if (count > 0)
          ecl.append("OR ");
        ecl.append(exclude).append("\n");
      }
      ecl.append(")");
    }
    return ecl.toString();
  }

  private String getEclFromSubsets(String iri) {
  Set<TTIriRef> subsets= setRepository.getSubsetIrisWithNames(iri);
  if (subsets.isEmpty())
    return "";
  Query query= new Query();
  for (TTIriRef subset:subsets){
    query.addOr(new Query().setIs(new Node().setIri(subset.getIri()).setName(subset.getName()).setEntails(Entail.memberOf)));
  }
    ECLQueryRequest request= new ECLQueryRequest().setQuery(query);
    new IMQToECL().getECLFromQuery(request);
    return request.getEcl();
  }

  private void levelN(Set<String> members,Set<String> inclusionAncestors,Set<String> exclusions) {
  Map<String,Map<String,Set<String>>> levelNParents= setRepository.getLevelNParentInclusions(members);
   setRepository.addNExclusions(members,levelNParents);
    for (String parentIri:levelNParents.keySet()){
      Map<String,Set<String>> InOrOut= levelNParents.get(parentIri);
      if (InOrOut.get("exclusion")==null){
        members.remove(parentIri);
        inclusionAncestors.add(parentIri);
      }
      else if (InOrOut.get("inclusion")!=null){
        int inSize= InOrOut.get("inclusion").size();
        int outSize= InOrOut.get("exclusion").size();
        double ratio = (double) outSize / inSize;
        if (ratio <0.5){
          members.remove(parentIri);
          inclusionAncestors.add(parentIri);
          exclusions.addAll(InOrOut.get("exclusion"));
        }
      }
    }
  }



}
