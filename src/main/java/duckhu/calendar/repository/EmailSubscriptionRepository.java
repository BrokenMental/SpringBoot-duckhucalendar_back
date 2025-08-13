package duckhu.calendar.repository;

import duckhu.calendar.entity.EmailSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailSubscriptionRepository extends JpaRepository<EmailSubscription, Long> {
    List<EmailSubscription> findAll();

    Optional<EmailSubscription> findByEmail(String email);

    Optional<EmailSubscription> findByUnsubscribeToken(String token);

    List<EmailSubscription> findByIsActiveTrue();

    boolean existsByEmail(String email);
}
