/*
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2025 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.jpa.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.config.JpaConfig;
import ca.uhn.fhir.jpa.packages.NpmJpaValidationSupport;
import ca.uhn.fhir.jpa.term.api.ITermConceptMappingSvc;
import ca.uhn.fhir.jpa.term.api.ITermReadSvc;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.UnknownCodeSystemWarningValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.WorkerContextValidationSupportAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class JpaValidationSupportChain extends ValidationSupportChain {

	private final FhirContext myFhirContext;
	private final WorkerContextValidationSupportAdapter myWorkerContextValidationSupportAdapter;

	@Autowired
	@Qualifier(JpaConfig.JPA_VALIDATION_SUPPORT)
	public IValidationSupport myJpaValidationSupport;

	@Qualifier("myDefaultProfileValidationSupport")
	@Autowired
	private IValidationSupport myDefaultProfileValidationSupport;

	@Autowired
	private ITermReadSvc myTerminologyService;

	@Autowired
	private NpmJpaValidationSupport myNpmJpaValidationSupport;

	@Autowired
	private ITermConceptMappingSvc myConceptMappingSvc;

	@Autowired
	private UnknownCodeSystemWarningValidationSupport myUnknownCodeSystemWarningValidationSupport;

	@Autowired
	private InMemoryTerminologyServerValidationSupport myInMemoryTerminologyServerValidationSupport;

	/**
	 * Constructor
	 */
	public JpaValidationSupportChain(
			FhirContext theFhirContext,
			CacheConfiguration theCacheConfiguration,
			WorkerContextValidationSupportAdapter theWorkerContextValidationSupportAdapter) {
		super(theCacheConfiguration);

		assert theFhirContext != null;
		assert theCacheConfiguration != null;

		myFhirContext = theFhirContext;
		myWorkerContextValidationSupportAdapter = theWorkerContextValidationSupportAdapter;
	}

	@Override
	public FhirContext getFhirContext() {
		return myFhirContext;
	}

	@PreDestroy
	public void flush() {
		invalidateCaches();
	}

	@PostConstruct
	public void postConstruct() {
		myWorkerContextValidationSupportAdapter.setValidationSupport(this);

		addValidationSupport(myDefaultProfileValidationSupport);
		addValidationSupport(myJpaValidationSupport);
		addValidationSupport(myTerminologyService);
		addValidationSupport(
				new SnapshotGeneratingValidationSupport(myFhirContext, myWorkerContextValidationSupportAdapter));
		addValidationSupport(myInMemoryTerminologyServerValidationSupport);
		addValidationSupport(myNpmJpaValidationSupport);
		addValidationSupport(new CommonCodeSystemsTerminologyService(myFhirContext));
		addValidationSupport(myConceptMappingSvc);

		// This needs to be last in the chain, it was designed for that
		addValidationSupport(myUnknownCodeSystemWarningValidationSupport);
	}
}
