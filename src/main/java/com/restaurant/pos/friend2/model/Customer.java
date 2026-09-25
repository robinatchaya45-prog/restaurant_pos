package com.restaurant.pos.friend2.model;

/**
 * Plain domain model representing a restaurant customer.
 * No SQL / persistence logic belongs here (see CustomerRepository).
 */
public class Customer {

    private Long customerId;
    private String name;
    private String phone;

    public Customer() {
    }

    public Customer(Long customerId, String name, String phone) {
        setCustomerId(customerId);
        setName(name);
        setPhone(phone);
    }

    public Customer(String name, String phone) {
        this(null, name, phone);
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name must not be empty");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException("Customer name is too long (max 100 characters)");
        }
        this.name = name.trim();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer phone must not be empty");
        }
        String trimmed = phone.trim();
        if (!trimmed.matches("^[+]?[0-9\\-\\s]{7,15}$")) {
            throw new IllegalArgumentException("Customer phone number is invalid");
        }
        this.phone = trimmed;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "customerId=" + customerId +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}