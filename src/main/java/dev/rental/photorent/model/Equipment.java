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

@Entity
@Table(name = "equipment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id", nullable = false, updatable = false)
    private EquipmentModel model;

    @Column(name = "serial_number", nullable = false, length = 15, updatable = false)
    private String serialNumber;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EquipmentStatus status;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EquipmentCondition condition;

    public Equipment(EquipmentModel model, String serialNumber,
                     EquipmentStatus status, EquipmentCondition condition) {
        this.model = model;
        this.serialNumber = serialNumber;
        this.status = status;
        this.condition = condition;
    }

    public boolean isAvailableForRental() {
        return status == EquipmentStatus.AVAILABLE
                && condition != EquipmentCondition.NEEDS_REPAIR
                && condition != EquipmentCondition.DECOMMISSIONED;
    }

    public boolean isUsable() {
        return condition != EquipmentCondition.NEEDS_REPAIR
                && condition != EquipmentCondition.DECOMMISSIONED;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Equipment equipment && id != null && id.equals(equipment.getId());
    }

    @Override
    public int hashCode() {
        return Equipment.class.hashCode();
    }

    @Override
    public String toString() {
        return "Equipment[id: " + id + ", serialNumber: '" + serialNumber
                + "', status: " + status + ", condition: " + condition + "]";
    }
}
