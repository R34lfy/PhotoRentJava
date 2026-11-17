package dev.rental.photorent.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("MANAGER")
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class ManagerUser extends User {

    public ManagerUser(String username, String email, String passwordHash) {
        super(username, email, passwordHash, UserRole.MANAGER);
    }

    @Override
    public String getPermissionsDescription() {
        return "Manage rentals: manage equipment catalog, confirm bookings, process issue/return, handle overdue cases";
    }
}
