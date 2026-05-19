package com.bank.core.repository;

import com.bank.core.entity.InterestAccrualLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InterestAccrualLogRepository extends JpaRepository<InterestAccrualLogEntity, UUID> {
}
