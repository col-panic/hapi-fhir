/*-
 * #%L
 * HAPI FHIR Storage api
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
package ca.uhn.fhir.jpa.binary.api;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import jakarta.annotation.Nonnull;
import org.hl7.fhir.instance.model.api.IBaseBinary;
import org.hl7.fhir.instance.model.api.IIdType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IBinaryStorageSvc {

	/**
	 * Gets the maximum number of bytes that can be stored in a single binary
	 * file by this service. The default is {@link Long#MAX_VALUE}
	 */
	long getMaximumBinarySize();

	/**
	 * Given a blob ID, return true if it is valid for the underlying storage mechanism, false otherwise.
	 *
	 * @param theNewBlobId the blob ID to validate
	 * @return true if the blob ID is valid, false otherwise.
	 */
	default boolean isValidBinaryContentId(String theNewBlobId) {
		return true; // default method here as we don't want to break existing implementations
	}

	/**
	 * Sets the maximum number of bytes that can be stored in a single binary
	 * file by this service. The default is {@link Long#MAX_VALUE}
	 *
	 * @param theMaximumBinarySize The maximum size
	 */
	void setMaximumBinarySize(long theMaximumBinarySize);

	/**
	 * Gets the minimum number of bytes that will be stored. Binary content smaller
	 * * than this threshold may be inlined even if a binary storage service
	 * * is active.
	 */
	int getMinimumBinarySize();

	/**
	 * Sets the minimum number of bytes that will be stored. Binary content smaller
	 * than this threshold may be inlined even if a binary storage service
	 * is active.
	 */
	void setMinimumBinarySize(int theMinimumBinarySize);

	/**
	 * Give the storage service the ability to veto items from storage
	 *
	 * @param theSize        How large is the item
	 * @param theResourceId  What is the resource ID it will be associated with
	 * @param theContentType What is the content type
	 * @return <code>true</code> if the storage service should store the item
	 */
	boolean shouldStoreBinaryContent(long theSize, IIdType theResourceId, String theContentType);

	/**
	 * Generate a new binaryContent ID that will be passed to {@link #storeBinaryContent(IIdType, String, String, InputStream)} later
	 */
	String newBinaryContentId();

	/**
	 * Store a new binary blob
	 *
	 * @param theResourceId   The resource ID that owns this blob. Note that it should not be possible to retrieve a blob without both the resource ID and the blob ID being correct.
	 * @param theBlobIdOrNull If set, forces
	 * @param theContentType  The content type to associate with this blob
	 * @param theInputStream  An InputStream to read from. This method should close the stream when it has been fully consumed.
	 * @return Returns details about the stored data
	 * @deprecated Use {@link #storeBinaryContent(IIdType theResourceId, String theBlobIdOrNull, String theContentType,
	 * 	InputStream theInputStream, RequestDetails theRequestDetails)} instead. This method
	 * 	will be removed because it doesn't receive the 'theRequestDetails' parameter it needs to forward to the pointcut)
	 */
	@Deprecated(since = "6.6.0", forRemoval = true)
	@Nonnull
	default StoredDetails storeBinaryContent(
			IIdType theResourceId, String theBlobIdOrNull, String theContentType, InputStream theInputStream)
			throws IOException {
		return storeBinaryContent(
				theResourceId, theBlobIdOrNull, theContentType, theInputStream, new ServletRequestDetails());
	}

	/**
	 * Store a new binary blob
	 *
	 * @param theResourceId   The resource ID that owns this blob. Note that it should not be possible to retrieve a blob without both the resource ID and the blob ID being correct.
	 * @param theBlobIdOrNull If set, forces
	 * @param theContentType  The content type to associate with this blob
	 * @param theInputStream  An InputStream to read from. This method should close the stream when it has been fully consumed.
	 * @param theRequestDetails The operation request details.
	 * @return Returns details about the stored data
	 */
	@Nonnull
	StoredDetails storeBinaryContent(
			IIdType theResourceId,
			String theBlobIdOrNull,
			String theContentType,
			InputStream theInputStream,
			RequestDetails theRequestDetails)
			throws IOException;

	StoredDetails fetchBinaryContentDetails(IIdType theResourceId, String theBlobId) throws IOException;

	/**
	 * @return Returns <code>true</code> if the blob was found and written, of <code>false</code> if the blob was not found (i.e. it was expunged or the ID was invalid)
	 */
	boolean writeBinaryContent(IIdType theResourceId, String theBlobId, OutputStream theOutputStream)
			throws IOException;

	void expungeBinaryContent(IIdType theResourceId, String theBlobId);

	/**
	 * Fetch the contents of the given blob
	 *
	 * @param theResourceId The resource ID
	 * @param theBlobId     The blob ID
	 * @return The payload as a byte array
	 */
	byte[] fetchBinaryContent(IIdType theResourceId, String theBlobId) throws IOException;

	/**
	 * Fetch the byte[] contents of a given Binary resource's `data` element. If the data is a standard base64encoded string that is embedded, return it.
	 * Otherwise, attempt to load the externalized binary blob via the the externalized binary storage service.
	 *
	 * @param theResource The  Binary resource you want to extract data bytes from
	 * @return The binary data blob as a byte array
	 */
	byte[] fetchDataByteArrayFromBinary(IBaseBinary theResource) throws IOException;
}
