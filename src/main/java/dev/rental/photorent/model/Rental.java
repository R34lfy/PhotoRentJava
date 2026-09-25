package dev.rental.photorent.model;

import java.time.LocalDate;
import java.util.Objects;

public class Rental {

    private final Long id;
    private final Equipment equipment;
    private final CustomerUser customer;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private RentalStatus status;

    public Rental(Long id, Equipment equipment, CustomerUser customer,
                  LocalDate startDate, LocalDate endDate, RentalStatus status) {
        this.id = id;
        this.equipment = equipment;
        this.customer = customer;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public CustomerUser getCustomer() {
        return customer;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public RentalStatus getStatus() {
        return status;
    }

    public void setStatus(RentalStatus status) {
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
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Rental rental = (Rental) other;
        return Objects.equals(id, rental.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Rental [id: " + id + ", equipment: " + equipment.getName()
                + ", customer: " + customer.getUsername() + ", startDate: " + startDate
                + ", endDate: " + endDate + ", status: " + status + "]";
    }
}

