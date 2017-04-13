package org.nzbhydra.database;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface NzbDownloadRepository extends JpaRepository<NzbDownloadEntity, Integer> {

    Collection<NzbDownloadEntity> findByIndexer(IndexerEntity indexerEntity);

    Page<NzbDownloadEntity> findByIndexerOrderByTimeDesc(IndexerEntity indexerEntity, Pageable pageable);
}
