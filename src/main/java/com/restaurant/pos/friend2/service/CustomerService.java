package com.restaurant.pos.friend2.service;

import com.restaurant.pos.friend2.model.Customer;
import com.restaurant.pos.friend2.repository.CustomerRepository;

import java.util.Optional;

/**
 * Business logic for customers. Swing / HTTP controllers must call this
 * layer instead of talking to CustomerRepository directly.
 */
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Logs a customer in by phone number, registering them as a new
     * customer if the phone number is not yet known.
     */
    public Customer loginOrRegister(String name, String phone) {
        Optional<Customer> existing = customerRepository.findByPhone(phone);
        if (existing.isPresent()) {
            return existing.get();
        }
        Customer customer = new Customer(name, phone);
        return customerRepository.save(customer);
    }

    public Customer register(String name, String phone) {
        Customer customer = new Customer(name, phone);
        return customerRepository.save(customer);
    }

    public Optional<Customer> findById(Long customerId) {
        return customerRepository.findById(customerId);
    }
}