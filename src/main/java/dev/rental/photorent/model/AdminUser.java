package dev.rental.photorent.model;

public class AdminUser extends User {

    public AdminUser(Long id, String username, String email) {
        super(id, username, email, UserRole.ADMIN);
    }

    @Override
    public String getPermissionsDescription() {
        return "Full access: fully-manage equipment catalog, categories, pricing, users";
    }
}

