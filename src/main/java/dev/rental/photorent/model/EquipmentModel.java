package dev.rental.photorent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;

@Entity
@Table(name = "equipment_model")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquipmentModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_prefix", nullable = false)
    private EquipmentCategory category;

    @Column(nullable = false, length = 50)
    private String brand;

    @Column(nullable = false, length = 100)
    private String name;

    @Setter
    private String description;

    @Setter
    @Column(name = "daily_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyRate;

    public EquipmentModel(EquipmentCategory category, String brand, String name,
                          String description, BigDecimal dailyRate) {
        this.category = category;
        this.brand = brand;
        this.name = name;
        this.description = description;
        this.dailyRate = dailyRate;
    }
}
