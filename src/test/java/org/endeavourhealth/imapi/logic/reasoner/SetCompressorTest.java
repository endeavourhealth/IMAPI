package org.endeavourhealth.imapi.logic.reasoner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.endeavourhealth.imapi.model.imq.Query;
import org.endeavourhealth.imapi.transforms.ECLToIMQ;
import org.junit.jupiter.api.Test;

public class SetCompressorTest {
 @Test
  public void testSetCompressor() throws JsonProcessingException {
    String ecl= new MembersToEcl().getEclFromMembers("http://snomed.info/sct#999012891000230104");
    Query query= new ECLToIMQ().convertECL(ecl);
    System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(query));
  }
}
