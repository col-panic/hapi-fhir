/*
 * #%L
 * HAPI FHIR - Core Library
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
package ca.uhn.fhir.rest.annotation;

import ca.uhn.fhir.rest.client.api.IBasicClient;
import ca.uhn.fhir.rest.client.api.IRestfulClient;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RESTful method annotation to be used for the FHIR <a href="http://hl7.org/implement/standards/fhir/http.html#read">read</a> and <a
 * href="http://hl7.org/implement/standards/fhir/http.html#vread">vread</a> method.
 *
 * <p>
 *     if you wish to support vread as well as read, you can use the {@link #version()} attribute to set it to true, indicating the method will handle both reads and vreads.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Read {

	/**
	 * The return type for this method. This generally does not need to be populated for IResourceProvider in a server implementation, but often does need to be populated in
	 * client implementations using {@link IBasicClient} or {@link IRestfulClient}, or in plain providers on a server.
	 * <p>
	 * This value also does not need to be populated if the return type for a method annotated with this annotation is sufficient to determine the type of resource provided. E.g. if the method returns
	 * <code>Patient</code> or <code>List&lt;Patient&gt;</code>, the server/client will automatically determine that the Patient resource is the return type, and this value may be left blank.
	 * </p>
	 */
	// NB: Read, Search (maybe others) share this annotation, so update the javadocs everywhere
	Class<? extends IBaseResource> type() default IBaseResource.class;

	/**
	 * This method allows the return type for this method to be specified in a
	 * non-type-specific way, using the text name of the resource, e.g. "Patient".
	 *
	 * This attribute should be populate, or {@link #type()} should be, but not both.
	 *
	 * @since 5.4.0
	 */
	String typeName() default "";

	/**
	 * If set to true (default is false), this method supports vread operation as well as read
	 */
	boolean version() default false;
}
