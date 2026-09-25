package dev.rental.photorent.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Equipment {

    private final Long id;
    private final String name;
    private final String brand;
    private final String description;
    private final EquipmentCategory category;
    private final String serialNumber;
    private final BigDecimal dailyRate;
    private EquipmentStatus status;
    private EquipmentCondition condition;

    public Equipment(Long id, String name, String brand, String description,
                     EquipmentCategory category, String serialNumber, BigDecimal dailyRate,
                     EquipmentStatus status, EquipmentCondition condition) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.description = description;
        this.category = category;
        this.serialNumber = serialNumber;
        this.dailyRate = dailyRate;
        this.status = status;
        this.condition = condition;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public String getDescription() {
        return description;
    }

    public EquipmentCategory getCategory() {
        return category;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = status;
    }

    public EquipmentCondition getCondition() {
        return condition;
    }

    public void setCondition(EquipmentCondition condition) {
        this.condition = condition;
    }

    public boolean isAvailableForRental() {
        return status == EquipmentStatus.AVAILABLE && condition != EquipmentCondition.NEEDS_REPAIR;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Equipment equipment = (Equipment) other;
        return Objects.equals(id, equipment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Equipment [id: " + id + ", name: '" + name + "', brand: '" + brand
                + "', category: " + category + ", status: " + status
                + ", condition: " + condition + ", dailyRate: " + dailyRate + "]";
    }
}

