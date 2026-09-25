package com.restaurant.pos.friend2.swing;

import com.restaurant.pos.friend2.model.Customer;
import com.restaurant.pos.friend2.service.CustomerService;
import com.restaurant.pos.friend2.service.OrderService;

import javax.swing.*;
import java.awt.*;

/**
 * First screen of the customer flow: Login -> Menu -> Cart -> Table Number
 * -> Place Order -> 10-minute edit/cancel window -> Order Locked.
 *
 * Swing talks ONLY to the service layer, never to repositories or SQL.
 */
public class CustomerLoginFrame extends JFrame {

    private final CustomerService customerService;
    private final OrderService orderService;

    private final JTextField nameField = new JTextField(20);
    private final JTextField phoneField = new JTextField(20);

    public CustomerLoginFrame(CustomerService customerService, OrderService orderService) {
        super("Restaurant POS - Customer Login");
        setIconImage(new ImageIcon(getClass().getResource("/images/logo.jpeg")).getImage());
        this.customerService = customerService;
        this.orderService = orderService;
        buildUi();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridBagLayout());
        setSize(400, 220);
        setLocationRelativeTo(null);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        add(new JLabel("Name:"), c);
        c.gridx = 1;
        add(nameField, c);

        c.gridx = 0;
        c.gridy = 1;
        add(new JLabel("Phone:"), c);
        c.gridx = 1;
        add(phoneField, c);

        JButton loginButton = new JButton("Login / Continue");
        loginButton.addActionListener(e -> onLogin());

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        add(loginButton, c);
    }

    private void onLogin() {
        String name = nameField.getText();
        String phone = phoneField.getText();
        try {
            Customer customer = customerService.loginOrRegister(name, phone);
            JOptionPane.showMessageDialog(this,
                    "Welcome, " + customer.getName() + "!",
                    "Login successful", JOptionPane.INFORMATION_MESSAGE);

            CustomerMenuFrame menuFrame = new CustomerMenuFrame(orderService, customer);
            menuFrame.setVisible(true);
            dispose();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid input", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Login failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}