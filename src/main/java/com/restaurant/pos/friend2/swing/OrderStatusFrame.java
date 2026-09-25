package com.restaurant.pos.friend2.swing;

import com.restaurant.pos.friend2.exception.InsufficientStockException;
import com.restaurant.pos.friend2.exception.InvalidQuantityException;
import com.restaurant.pos.friend2.exception.OrderAlreadyCancelledException;
import com.restaurant.pos.friend2.exception.OrderNotEditableException;
import com.restaurant.pos.friend2.model.Order;
import com.restaurant.pos.friend2.model.OrderItem;
import com.restaurant.pos.friend2.service.OrderService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Shows order ID, table number, status, and the remaining edit/cancel
 * time. Lets the customer increase/decrease item quantities or cancel
 * the order while it is still within the 10-minute PENDING window.
 * The customer never presses a "lock" button - locking happens
 * automatically via ReservationScheduler + OrderService.lockOrder().
 */
public class OrderStatusFrame extends JFrame {

    private final OrderService orderService;
    private final Long orderId;

    private final JLabel statusLabel = new JLabel();
    private final JLabel remainingLabel = new JLabel();
    private final DefaultListModel<String> itemsModel = new DefaultListModel<>();
    private final Timer refreshTimer;

    public OrderStatusFrame(OrderService orderService, Long orderId) {
        super("Restaurant POS - Order Status");
     setIconImage(new ImageIcon(getClass().getResource("/images/logo.jpeg")).getImage());
        this.orderService = orderService;
        this.orderId = orderId;
        buildUi();
        refreshTimer = new Timer(1000, e -> refresh());
        refreshTimer.start();
        refresh();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(420, 380);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JPanel topPanel = new JPanel(new GridLayout(3, 1));
        topPanel.add(new JLabel("Order #" + orderId));
        topPanel.add(statusLabel);
        topPanel.add(remainingLabel);
        add(topPanel, BorderLayout.NORTH);

        JList<String> itemsList = new JList<>(itemsModel);
        add(new JScrollPane(itemsList), BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton increaseButton = new JButton("Increase Qty");
        increaseButton.addActionListener(e -> promptQuantityChange(true));
        JButton decreaseButton = new JButton("Decrease Qty");
        decreaseButton.addActionListener(e -> promptQuantityChange(false));
        JButton cancelButton = new JButton("Cancel Order");
        cancelButton.addActionListener(e -> onCancel());

        buttons.add(increaseButton);
        buttons.add(decreaseButton);
        buttons.add(cancelButton);
        add(buttons, BorderLayout.SOUTH);
    }

    private void refresh() {
        orderService.findById(orderId).ifPresent(order -> {
            statusLabel.setText("Status: " + order.getStatus());
            long remaining = orderService.remainingEditSeconds(order);
            remainingLabel.setText("Remaining edit time: " + remaining + "s");

            itemsModel.clear();
            List<OrderItem> items = orderService.findItems(orderId);
            for (OrderItem item : items) {
                itemsModel.addElement("Item " + item.getItemId() +
                        " x" + item.getQuantity() + " @ $" + item.getPrice());
            }

            if (order.getStatus() != com.restaurant.pos.friend2.model.OrderStatus.PENDING) {
                refreshTimer.stop();
            }
        });
    }

    private void promptQuantityChange(boolean increase) {
        String itemIdStr = JOptionPane.showInputDialog(this, "Item ID:");
        if (itemIdStr == null || itemIdStr.trim().isEmpty()) {
            return;
        }
        String amountStr = JOptionPane.showInputDialog(this,
                (increase ? "Increase" : "Decrease") + " by how much?");
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return;
        }
        try {
            Long itemId = Long.parseLong(itemIdStr.trim());
            int amount = Integer.parseInt(amountStr.trim());
            if (increase) {
                orderService.increaseItemQuantity(orderId, itemId, amount);
            } else {
                orderService.decreaseItemQuantity(orderId, itemId, amount);
            }
            refresh();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Item ID and amount must be numeric.",
                    "Invalid input", JOptionPane.ERROR_MESSAGE);
        } catch (InsufficientStockException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Insufficient stock",
                    JOptionPane.ERROR_MESSAGE);
        } catch (InvalidQuantityException | OrderNotEditableException | OrderAlreadyCancelledException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot update order",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Update failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancel() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel this order?",
                "Confirm cancellation", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            orderService.cancelOrder(orderId);
            JOptionPane.showMessageDialog(this, "Order cancelled.", "Cancelled",
                    JOptionPane.INFORMATION_MESSAGE);
            refresh();
        } catch (OrderNotEditableException | OrderAlreadyCancelledException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot cancel",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Cancellation failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}