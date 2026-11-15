package dev.rental.photorent.model;

public class CustomerUser extends User {

    public CustomerUser(Long id, String username, String email) {
        super(id, username, email, UserRole.CUSTOMER);
    }

    @Override
    public String getPermissionsDescription() {
        return "Browse catalog, book equipment, view own rental history";
    }
}

