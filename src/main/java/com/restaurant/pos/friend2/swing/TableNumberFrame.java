package com.restaurant.pos.friend2.swing;

import com.restaurant.pos.friend2.model.Customer;
import com.restaurant.pos.friend2.service.OrderService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Requires the customer to enter a valid table number before an order can
 * be placed. Validation: must not be empty, must be numeric, must be
 * positive.
 */
public class TableNumberFrame extends JFrame {

    private final OrderService orderService;
    private final Customer customer;
    private final List<CartFrame.CartEntry> cart;
    private final JTextField tableNumberField = new JTextField(10);

    public TableNumberFrame(OrderService orderService, Customer customer, List<CartFrame.CartEntry> cart) {
        super("Restaurant POS - Table Number");
         setIconImage(new ImageIcon(getClass().getResource("/images/logo.jpeg")).getImage());
        this.orderService = orderService;
        this.customer = customer;
        this.cart = cart;
        buildUi();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(350, 160);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);

        c.gridx = 0;
        c.gridy = 0;
        add(new JLabel("Table Number:"), c);
        c.gridx = 1;
        add(tableNumberField, c);

        JButton continueButton = new JButton("Continue to Confirmation");
        continueButton.addActionListener(e -> onContinue());

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        add(continueButton, c);
    }

    private void onContinue() {
        String raw = tableNumberField.getText();
        Integer tableNo = validateTableNumber(raw);
        if (tableNo == null) {
            return;
        }

        OrderConfirmationFrame confirmationFrame =
                new OrderConfirmationFrame(orderService, customer, cart, tableNo);
        confirmationFrame.setVisible(true);
        dispose();
    }

    private Integer validateTableNumber(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Table number must not be empty.",
                    "Invalid table number", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Table number must be numeric.",
                    "Invalid table number", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (value <= 0) {
            JOptionPane.showMessageDialog(this, "Table number must be positive.",
                    "Invalid table number", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return value;
    }
}