package dev.rental.photorent.model;

public class ManagerUser extends User {

    public ManagerUser(Long id, String username, String email) {
        super(id, username, email, UserRole.MANAGER);
    }

    @Override
    public String getPermissionsDescription() {
        return "Manage rentals: manage equipment catalog, confirm bookings, process issue/return, handle overdue cases";
    }
}

