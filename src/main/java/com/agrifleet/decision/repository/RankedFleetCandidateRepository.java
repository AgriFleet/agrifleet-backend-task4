package com.agrifleet.decision.repository;

import com.agrifleet.decision.entity.RankedFleetCandidateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RankedFleetCandidateRepository extends JpaRepository<RankedFleetCandidateEntity, Long> {
    List<RankedFleetCandidateEntity> findByDecisionRunIdOrderByFinalRankAsc(Long decisionRunId);
}