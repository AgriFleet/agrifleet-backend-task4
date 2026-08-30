package com.agrifleet.decision.repository;

import com.agrifleet.decision.entity.TopsisDecisionRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TopsisDecisionRunRepository extends JpaRepository<TopsisDecisionRunEntity, Long> {
}