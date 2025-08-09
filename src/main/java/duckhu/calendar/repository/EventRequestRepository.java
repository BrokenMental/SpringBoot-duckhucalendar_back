package duckhu.calendar.repository;

import duckhu.calendar.entity.EventRequest;
import duckhu.calendar.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRequestRepository extends JpaRepository<EventRequest, Long> {

    List<EventRequest> findAllByOrderByCreatedAtDesc();

    List<EventRequest> findByStatus(RequestStatus status);

    List<EventRequest> findByRequesterEmailOrderByCreatedAtDesc(String email);
}
