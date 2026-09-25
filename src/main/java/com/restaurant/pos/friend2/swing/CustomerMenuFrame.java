package com.restaurant.pos.friend2.swing;

import com.restaurant.pos.friend2.model.Customer;
import com.restaurant.pos.friend2.service.OrderService;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays available menu items. Unavailable items are shown disabled
 * (greyed out). In the fully integrated system, {@link #loadDemoMenu()}
 * would be replaced by a call through the agreed API contract into
 * Friend 1's menu/inventory service.
 */
public class CustomerMenuFrame extends JFrame {

    private final OrderService orderService;
    private final Customer customer;
    private final List<CartFrame.CartEntry> cart = new ArrayList<>();

    public CustomerMenuFrame(OrderService orderService, Customer customer) {
        super("Restaurant POS - Menu");
        setIconImage(new ImageIcon(getClass().getResource("/images/logo.jpeg")).getImage());
        this.orderService = orderService;
        this.customer = customer;
        buildUi();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(480, 420);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JPanel itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));

        for (MenuItemView item : loadDemoMenu()) {
            itemsPanel.add(buildItemRow(item));
        }

        JScrollPane scrollPane = new JScrollPane(itemsPanel);
        add(scrollPane, BorderLayout.CENTER);

        JButton viewCartButton = new JButton("View Cart");
        viewCartButton.addActionListener(e -> {
            CartFrame cartFrame = new CartFrame(orderService, customer, cart);
            cartFrame.setVisible(true);
        });
        add(viewCartButton, BorderLayout.SOUTH);
    }

    private JPanel buildItemRow(MenuItemView item) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JLabel label = new JLabel(item.toString());
        row.add(label, BorderLayout.CENTER);

        JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
        quantitySpinner.setEnabled(item.isAvailable());
        quantitySpinner.setMaximumSize(new Dimension(60, 24));

        JButton addButton = new JButton("Add to Cart");
        addButton.setEnabled(item.isAvailable());
        addButton.addActionListener(e -> {
            int qty = (Integer) quantitySpinner.getValue();
            cart.add(new CartFrame.CartEntry(item.getItemId(), item.getName(), qty, item.getPrice()));
            JOptionPane.showMessageDialog(this, item.getName() + " x" + qty + " added to cart.");
        });

        JPanel controls = new JPanel();
        controls.add(quantitySpinner);
        controls.add(addButton);
        row.add(controls, BorderLayout.EAST);

        return row;
    }

    /**
     * Placeholder menu data. Replace with a real call into Friend 1's
     * menu/inventory service via the agreed API contract.
     */
    private List<MenuItemView> loadDemoMenu() {
        List<MenuItemView> items = new ArrayList<>();
        items.add(new MenuItemView(1L, "Grilled Chicken", new BigDecimal("12.99"), true));
        items.add(new MenuItemView(2L, "Veggie Burger", new BigDecimal("9.50"), true));
        items.add(new MenuItemView(3L, "Seafood Pasta", new BigDecimal("15.00"), false));
        items.add(new MenuItemView(4L, "Caesar Salad", new BigDecimal("7.25"), true));
        return items;
    }
}