package one.ampadu.dsv.repository;

import one.ampadu.dsv.entity.ProtocolProcessRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProtocolProcessRunRepository extends JpaRepository<ProtocolProcessRun, Long> {
    Optional<ProtocolProcessRun> findFirstByOrderByIdDesc();
}
