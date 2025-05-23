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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.jpa.dao.data;

import ca.uhn.fhir.jpa.entity.BulkImportJobEntity;
import ca.uhn.fhir.jpa.entity.BulkImportJobFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IBulkImportJobFileDao extends JpaRepository<BulkImportJobFileEntity, Long>, IHapiFhirJpaRepository {

	@Query("SELECT f FROM BulkImportJobFileEntity f WHERE f.myJob.myJobId = :jobId ORDER BY f.myFileSequence ASC")
	List<BulkImportJobFileEntity> findAllForJob(@Param("jobId") String theJobId);

	@Query("SELECT f FROM BulkImportJobFileEntity f WHERE f.myJob = :job AND f.myFileSequence = :fileIndex")
	Optional<BulkImportJobFileEntity> findForJob(
			@Param("job") BulkImportJobEntity theJob, @Param("fileIndex") int theFileIndex);

	@Query(
			"SELECT f.myFileDescription FROM BulkImportJobFileEntity f WHERE f.myJob = :job AND f.myFileSequence = :fileIndex")
	Optional<String> findFileDescriptionForJob(
			@Param("job") BulkImportJobEntity theJob, @Param("fileIndex") int theFileIndex);

	@Query("SELECT f.myId FROM BulkImportJobFileEntity f WHERE f.myJob.myJobId = :jobId ORDER BY f.myFileSequence ASC")
	List<Long> findAllIdsForJob(@Param("jobId") String theJobId);
}
