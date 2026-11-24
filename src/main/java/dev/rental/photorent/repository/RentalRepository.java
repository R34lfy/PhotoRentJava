package dev.rental.photorent.repository;

import dev.rental.photorent.model.Rental;
import dev.rental.photorent.model.RentalStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface RentalRepository extends JpaRepository<Rental, Long>, JpaSpecificationExecutor<Rental> {

    // техника и клиент подгружаются одним запросом, иначе DTO дочитывал бы их по одной
    @Override
    @EntityGraph(attributePaths = {"equipment", "customer"})
    List<Rental> findAll(Specification<Rental> spec);

    List<Rental> findByStatus(RentalStatus status);

    boolean existsByEquipmentIdAndStatusIn(Long equipmentId, Collection<RentalStatus> statuses);

    // пересечение периодов, границы включительно (как Rental.overlapsWith)
    @Query("""
            select r from Rental r
            where r.equipment.id = :equipmentId
              and r.status in :statuses
              and r.startDate <= :endDate
              and r.endDate >= :startDate
            """)
    List<Rental> findOverlapping(@Param("equipmentId") Long equipmentId,
                                 @Param("statuses") Collection<RentalStatus> statuses,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

    // то же, но без самой аренды (нужно при продлении)
    @Query("""
            select r from Rental r
            where r.equipment.id = :equipmentId
              and r.id <> :excludedRentalId
              and r.status in :statuses
              and r.startDate <= :endDate
              and r.endDate >= :startDate
            """)
    List<Rental> findOverlappingExcluding(@Param("equipmentId") Long equipmentId,
                                          @Param("excludedRentalId") Long excludedRentalId,
                                          @Param("statuses") Collection<RentalStatus> statuses,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
}

