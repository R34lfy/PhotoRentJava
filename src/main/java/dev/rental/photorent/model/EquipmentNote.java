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

import java.time.LocalDateTime;

@Entity
@Table(name = "equipment_note")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquipmentNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false, updatable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false, updatable = false)
    private User author;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "note_text", nullable = false, updatable = false)
    private String text;

    public EquipmentNote(Equipment equipment, User author, String text) {
        this.equipment = equipment;
        this.author = author;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EquipmentNote note && id != null && id.equals(note.getId());
    }

    @Override
    public int hashCode() {
        return EquipmentNote.class.hashCode();
    }

    @Override
    public String toString() {
        return "EquipmentNote[id: " + id + ", createdAt: " + createdAt + "]";
    }
}
