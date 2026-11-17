package dev.rental.photorent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "equipment_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquipmentCategory {

    @Id
    @Column(name = "serial_prefix", length = 3, updatable = false)
    private String serialPrefix;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "last_serial_number", nullable = false)
    private int lastSerialNumber;

    public EquipmentCategory(String serialPrefix, String name) {
        this.serialPrefix = serialPrefix;
        this.name = name;
    }

    // вызывать только внутри транзакции (строка категории при этом блокируется)
    public String nextSerialNumber() {
        lastSerialNumber++;
        return String.format("%s-%05d", serialPrefix, lastSerialNumber);
    }
}
