package com.agrifleet.decision.repository;

import com.agrifleet.decision.entity.HarvestDelayPredictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HarvestDelayPredictionRepository extends JpaRepository<HarvestDelayPredictionEntity, Long> {
}