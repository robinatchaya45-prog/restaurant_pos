package com.restaurant.pos.friend2.swing;

import com.restaurant.pos.friend2.exception.InsufficientStockException;
import com.restaurant.pos.friend2.exception.InvalidQuantityException;
import com.restaurant.pos.friend2.exception.InvalidTableNumberException;
import com.restaurant.pos.friend2.model.Customer;
import com.restaurant.pos.friend2.model.Order;
import com.restaurant.pos.friend2.service.OrderService;
import com.restaurant.pos.friend2.service.OrderService.CartLine;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows the order summary (items, table number, total) and places the
 * order through OrderService when the customer confirms.
 */
public class OrderConfirmationFrame extends JFrame {

    private final OrderService orderService;
    private final Customer customer;
    private final List<CartFrame.CartEntry> cart;
    private final int tableNo;

    public OrderConfirmationFrame(OrderService orderService, Customer customer,
                                   List<CartFrame.CartEntry> cart, int tableNo) {
        super("Restaurant POS - Confirm Order");
        setIconImage(new ImageIcon(getClass().getResource("/images/logo.jpeg")).getImage());
        this.orderService = orderService;
        this.customer = customer;
        this.cart = cart;
        this.tableNo = tableNo;
        buildUi();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(420, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JTextArea summary = new JTextArea();
        summary.setEditable(false);
        StringBuilder sb = new StringBuilder();
        sb.append("Table Number: ").append(tableNo).append("\n\n");
        BigDecimal total = BigDecimal.ZERO;
        for (CartFrame.CartEntry entry : cart) {
            sb.append(entry.name)
                    .append(" x").append(entry.quantity)
                    .append(" = $").append(entry.getSubtotal()).append("\n");
            total = total.add(entry.getSubtotal());
        }
        sb.append("\nTotal: $").append(total);
        summary.setText(sb.toString());
        add(new JScrollPane(summary), BorderLayout.CENTER);

        JButton placeOrderButton = new JButton("Place Order");
        placeOrderButton.addActionListener(e -> onPlaceOrder());
        add(placeOrderButton, BorderLayout.SOUTH);
    }

    private void onPlaceOrder() {
        List<CartLine> cartLines = new ArrayList<>();
        for (CartFrame.CartEntry entry : cart) {
            cartLines.add(new CartLine(entry.itemId, entry.quantity, entry.price));
        }

        try {
            Order order = orderService.placeOrder(customer.getCustomerId(), tableNo, cartLines);
            JOptionPane.showMessageDialog(this,
                    "Order #" + order.getOrderId() + " placed successfully!\n" +
                            "You have 10 minutes to edit or cancel it.",
                    "Order placed", JOptionPane.INFORMATION_MESSAGE);

            OrderStatusFrame statusFrame = new OrderStatusFrame(orderService, order.getOrderId());
            statusFrame.setVisible(true);
            dispose();
        } catch (InvalidTableNumberException | InvalidQuantityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid order", JOptionPane.ERROR_MESSAGE);
        } catch (InsufficientStockException ex) {
            JOptionPane.showMessageDialog(this,
                    "Sorry, one or more items just went out of stock:\n" + ex.getMessage(),
                    "Insufficient stock", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to place order: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}