package ca.uhn.fhir.mdm.rules.json;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.mdm.api.MdmMatchResultEnum;
import ca.uhn.fhir.mdm.rules.matcher.util.MatchRuleUtil;
import ca.uhn.fhir.mdm.rules.similarity.MdmSimilarityEnum;
import ca.uhn.fhir.mdm.rules.svc.BaseMdmRulesR4Test;
import ca.uhn.fhir.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


public class MdmRulesJsonR4Test extends BaseMdmRulesR4Test {
	private static final Logger ourLog = LoggerFactory.getLogger(MdmRulesJsonR4Test.class);
	private MdmRulesJson myRules;

	@Override
	@BeforeEach
	public void before() {
		super.before();

		myRules = buildActiveBirthdateIdRules();
	}

	@Test
	public void testValidate() throws IOException {
		MdmRulesJson rules = new MdmRulesJson();
		try {
			JsonUtil.serialize(rules);
		} catch (NullPointerException e) {
			assertThat(e.getMessage()).contains("version may not be blank");
		}
	}

	@Test
	public void testSerDeser() throws IOException {
		String json = JsonUtil.serialize(myRules);
		ourLog.info(json);
		MdmRulesJson rulesDeser = JsonUtil.deserialize(json, MdmRulesJson.class);
		assertEquals(2, rulesDeser.size());
		assertEquals(MdmMatchResultEnum.MATCH, rulesDeser.getMatchResult(myBothNameFields));
		MdmFieldMatchJson second = rulesDeser.get(1);
		assertEquals("name.family", second.getResourcePath());
		assertEquals(MdmSimilarityEnum.JARO_WINKLER, second.getSimilarity().getAlgorithm());
	}

	@Test
	public void testMatchResultMap() {
		assertEquals(MdmMatchResultEnum.MATCH, myRules.getMatchResult(3L));
	}

	@Test
	public void getVector_basicTest() {
		VectorMatchResultMap vectorMatchResultMap = myRules.getVectorMatchResultMapForUnitTest();
		assertEquals(1, vectorMatchResultMap.getVector(PATIENT_GIVEN));
		assertEquals(2, vectorMatchResultMap.getVector(PATIENT_FAMILY));
		assertEquals(3, vectorMatchResultMap.getVector(String.join(",", PATIENT_GIVEN, PATIENT_FAMILY)));
		assertEquals(3, vectorMatchResultMap.getVector(String.join(", ", PATIENT_GIVEN, PATIENT_FAMILY)));
		assertEquals(3, vectorMatchResultMap.getVector(String.join(",  ", PATIENT_GIVEN, PATIENT_FAMILY)));
		assertEquals(3, vectorMatchResultMap.getVector(String.join(", \n ", PATIENT_GIVEN, PATIENT_FAMILY)));
		try {
			vectorMatchResultMap.getVector("bad");
			fail();
		} catch (ConfigurationException e) {
			assertEquals(Msg.code(1523) + "There is no matchField with name bad", e.getMessage());
		}
	}

	@Test
	public void validate_withTooManyFields_throws() {
		// setup
		MdmRulesJson rules = new MdmRulesJson();
		rules.setVersion("1");

		// we don't need real rules; just one that will hit our validate code correctly
		for (int i = 0; i < MatchRuleUtil.MAX_RULE_COUNT + 1; i++) {
			MdmFieldMatchJson fieldMatchJson = new MdmFieldMatchJson();
			fieldMatchJson.setName("field_" + i);
			rules.addMatchField(fieldMatchJson);
		}

		// test
		try {
			rules.initialize();
			fail(String.format("We currently only handle up to %s rules", MatchRuleUtil.MAX_RULE_COUNT));
		} catch (IllegalArgumentException ex) {
			assertEquals("MDM cannot guarantee accuracy with more than 64 match fields.", ex.getLocalizedMessage(), ex.getLocalizedMessage());
		}
	}

	@Test
	public void testInvalidResourceTypeDoesntDeserialize() throws IOException {
		myRules = buildOldStyleEidRules();

		String eidSystem = myRules.getEnterpriseEIDSystemForResourceType("Patient");
		assertEquals(PATIENT_EID_FOR_TEST, eidSystem);

		eidSystem = myRules.getEnterpriseEIDSystemForResourceType("Practitioner");
		assertEquals(PATIENT_EID_FOR_TEST, eidSystem);

		eidSystem = myRules.getEnterpriseEIDSystemForResourceType("Medication");
		assertEquals(PATIENT_EID_FOR_TEST, eidSystem);
	}

	@Override
	protected MdmRulesJson buildActiveBirthdateIdRules() {
		return super.buildActiveBirthdateIdRules();
	}

	private MdmRulesJson buildOldStyleEidRules() {
		MdmRulesJson mdmRulesJson = super.buildActiveBirthdateIdRules();
		mdmRulesJson.setEnterpriseEIDSystems(Collections.emptyMap());
		//This sets the new-style eid resource type to `*`
		mdmRulesJson.setEnterpriseEIDSystem(PATIENT_EID_FOR_TEST);
		return mdmRulesJson;
	}

}
