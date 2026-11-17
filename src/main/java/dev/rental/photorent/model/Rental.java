package dev.rental.photorent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "rental")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false, updatable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private CustomerUser customer;

    @Column(name = "start_date", nullable = false, updatable = false)
    private LocalDate startDate;

    // не final (аренду можно продлить)
    @Setter
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RentalStatus status;

    public Rental(Equipment equipment, CustomerUser customer,
                  LocalDate startDate, LocalDate endDate, RentalStatus status) {
        this.equipment = equipment;
        this.customer = customer;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public boolean overlapsWith(LocalDate otherStart, LocalDate otherEnd) {
        return !startDate.isAfter(otherEnd) && !otherStart.isAfter(endDate);
    }

    public boolean isOverdue(LocalDate today) {
        return status == RentalStatus.ACTIVE && today.isAfter(endDate);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Rental rental && id != null && id.equals(rental.getId());
    }

    @Override
    public int hashCode() {
        return Rental.class.hashCode();
    }

    @Override
    public String toString() {
        return "Rental[id: " + id + ", startDate: " + startDate
                + ", endDate: " + endDate + ", status: " + status + "]";
    }
}
