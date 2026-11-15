package dev.rental.photorent.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class EquipmentNote {

    private final Long id;
    private final Equipment equipment;
    private final User author;
    private final String text;
    private final LocalDateTime createdAt;

    public EquipmentNote(Long id, Equipment equipment, User author, String text, LocalDateTime createdAt) {
        this.id = id;
        this.equipment = equipment;
        this.author = author;
        this.text = text;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public User getAuthor() {
        return author;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        EquipmentNote note = (EquipmentNote) other;
        return Objects.equals(id, note.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "EquipmentNote{id=" + id + ", equipment=" + equipment.getSerialNumber()
                + ", author=" + author.getUsername() + ", createdAt=" + createdAt + "}";
    }
}

