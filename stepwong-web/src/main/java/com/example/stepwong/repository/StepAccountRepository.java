package com.example.stepwong.repository;

import com.example.stepwong.entity.StepAccount;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StepAccountRepository extends JpaRepository<StepAccount, Long> {

    List<StepAccount> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<StepAccount> findByIdAndOwnerId(Long id, Long ownerId);

    @Query("select account from StepAccount account "
            + "join fetch account.owner owner "
            + "where account.id = :id and owner.id = :ownerId")
    Optional<StepAccount> findWithOwnerByIdAndOwnerId(@Param("id") Long id, @Param("ownerId") Long ownerId);

    boolean existsByOwnerIdAndAccountNo(Long ownerId, String accountNo);

    @Query("select account from StepAccount account "
            + "join fetch account.owner owner "
            + "where account.enabled = true "
            + "and account.autoEnabled = true "
            + "and account.runHour = :hour "
            + "and account.runMinute = :minute "
            + "and (account.lastRunDate is null or account.lastRunDate < :today)")
    List<StepAccount> findDueAccounts(
            @Param("today") LocalDate today,
            @Param("hour") int hour,
            @Param("minute") int minute
    );
}
