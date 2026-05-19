package com.bank.core.repository;

import com.bank.core.entity.BankAccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccountEntity, Long> {

    List<BankAccountEntity> findAllByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM BankAccountEntity a WHERE a.id = :id")
    Optional<BankAccountEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT a FROM BankAccountEntity a
            WHERE a.type = 'SAVINGS'
              AND a.status = 'ACTIVE'
              AND (a.lastInterestAccruedDate IS NULL OR a.lastInterestAccruedDate < :date)
            """)
    Page<BankAccountEntity> findSavingsForAccrual(@Param("date") LocalDate date, Pageable pageable);
}
