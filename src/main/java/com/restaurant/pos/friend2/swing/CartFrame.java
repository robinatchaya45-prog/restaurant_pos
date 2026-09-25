package com.restaurant.pos.friend2.swing;

import com.restaurant.pos.friend2.model.Customer;
import com.restaurant.pos.friend2.service.OrderService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Displays the customer's cart: item, quantity, price, subtotal, with
 * edit-quantity and remove-item actions, then proceeds to entering the
 * table number.
 */
public class CartFrame extends JFrame {

    /** Simple in-memory cart line used before the order is persisted. */
    public static class CartEntry {
        public final Long itemId;
        public final String name;
        public int quantity;
        public final BigDecimal price;

        public CartEntry(Long itemId, String name, int quantity, BigDecimal price) {
            this.itemId = itemId;
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }

        public BigDecimal getSubtotal() {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
    }

    private final OrderService orderService;
    private final Customer customer;
    private final List<CartEntry> cart;
    private final DefaultTableModel tableModel;

    public CartFrame(OrderService orderService, Customer customer, List<CartEntry> cart) {
        super("Restaurant POS - Your Cart");
        setIconImage(new ImageIcon(getClass().getResource("/images/logo.jpeg")).getImage());
        this.orderService = orderService;
        this.customer = customer;
        this.cart = cart;
        this.tableModel = new DefaultTableModel(
                new Object[]{"Item", "Quantity", "Price", "Subtotal"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // only quantity is editable
            }
        };
        buildUi();
        refreshTable();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JTable table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton removeButton = new JButton("Remove Selected");
        removeButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && row < cart.size()) {
                cart.remove(row);
                refreshTable();
            }
        });

        JButton updateQtyButton = new JButton("Apply Quantity Changes");
        updateQtyButton.addActionListener(e -> applyQuantityEdits());

        JButton checkoutButton = new JButton("Enter Table Number");
        checkoutButton.addActionListener(e -> {
            if (cart.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Your cart is empty.", "Cart empty",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            TableNumberFrame tableNumberFrame = new TableNumberFrame(orderService, customer, cart);
            tableNumberFrame.setVisible(true);
            dispose();
        });

        JPanel buttons = new JPanel();
        buttons.add(removeButton);
        buttons.add(updateQtyButton);
        buttons.add(checkoutButton);
        add(buttons, BorderLayout.SOUTH);
    }

    private void applyQuantityEdits() {
        for (int i = 0; i < cart.size(); i++) {
            Object value = tableModel.getValueAt(i, 1);
            try {
                int newQty = Integer.parseInt(String.valueOf(value));
                if (newQty <= 0) {
                    JOptionPane.showMessageDialog(this, "Quantity must be positive.",
                            "Invalid quantity", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cart.get(i).quantity = newQty;
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Quantity must be numeric.",
                        "Invalid quantity", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (CartEntry entry : cart) {
            tableModel.addRow(new Object[]{
                    entry.name, entry.quantity, entry.price, entry.getSubtotal()
            });
        }
    }
}