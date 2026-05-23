package com.dailyCode.paypal_user.transaction.repository;

import com.dailyCode.paypal_user.transaction.entity.Transaction;
import com.dailyCode.paypal_user.transaction.enums.TransactionStatus;
import com.dailyCode.paypal_user.transaction.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReferenceId(String referenceId);

    // All transactions where user is sender or receiver
    @Query("SELECT t FROM Transaction t WHERE t.sender.id = :userId OR t.receiver.id = :userId ORDER BY t.createdAt DESC")
    Page<Transaction> findAllByUserId(Long userId, Pageable pageable);

    // Sent transactions
    Page<Transaction> findBySenderIdOrderByCreatedAtDesc(Long senderId, Pageable pageable);

    // Received transactions
    Page<Transaction> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    // Filter by type
    @Query("SELECT t FROM Transaction t WHERE (t.sender.id = :userId OR t.receiver.id = :userId) AND t.type = :type ORDER BY t.createdAt DESC")
    Page<Transaction> findAllByUserIdAndType(Long userId, TransactionType type, Pageable pageable);

    // Filter by status
    @Query("SELECT t FROM Transaction t WHERE (t.sender.id = :userId OR t.receiver.id = :userId) AND t.status = :status ORDER BY t.createdAt DESC")
    Page<Transaction> findAllByUserIdAndStatus(Long userId, TransactionStatus status, Pageable pageable);

    boolean existsByReferenceId(String referenceId);
}
