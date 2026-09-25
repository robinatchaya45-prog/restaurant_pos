package com.restaurant.pos.friend2;

import com.restaurant.pos.friend2.controller.CustomerController;
import com.restaurant.pos.friend2.controller.OrderController;
import com.restaurant.pos.friend2.controller.ReservationController;
import com.restaurant.pos.friend2.inventory.InventoryService;
import com.restaurant.pos.friend2.inventory.InventoryServiceStub;
import com.restaurant.pos.friend2.repository.CustomerRepository;
import com.restaurant.pos.friend2.repository.OrderItemRepository;
import com.restaurant.pos.friend2.repository.OrderRepository;
import com.restaurant.pos.friend2.repository.ReservationRepository;
import com.restaurant.pos.friend2.service.CustomerService;
import com.restaurant.pos.friend2.service.OrderService;
import com.restaurant.pos.friend2.service.ReservationScheduler;
import com.restaurant.pos.friend2.service.ReservationService;
import com.restaurant.pos.friend2.swing.CustomerLoginFrame;
import com.sun.net.httpserver.HttpServer;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Application entry point for the Friend 2 module (Customer + Order +
 * Table Number + 10-minute Stock Reservation).
 *
 * Wires: Swing / REST API -> Service Layer -> Repository/DAO -> JDBC -> MySQL
 *
 * NOTE: {@link InventoryServiceStub} is a temporary placeholder for
 * Friend 1's real inventory implementation. Swap it out for the real
 * InventoryService bean here once it is merged into the shared repo -
 * no other class needs to change.
 */
public final class Main {

    private static final int HTTP_PORT = 8080;

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        // ---- Repository / DAO layer ----
        CustomerRepository customerRepository = new CustomerRepository();
        OrderRepository orderRepository = new OrderRepository();
        OrderItemRepository orderItemRepository = new OrderItemRepository();
        ReservationRepository reservationRepository = new ReservationRepository();

        // ---- Inventory contract (Friend 1 integration point) ----
        InventoryService inventoryService = new InventoryServiceStub();

        // ---- Service layer ----
        CustomerService customerService = new CustomerService(customerRepository);
        ReservationService reservationService = new ReservationService(reservationRepository, inventoryService);
        OrderService orderService = new OrderService(orderRepository, orderItemRepository,
                reservationService, inventoryService);
        ReservationScheduler reservationScheduler = new ReservationScheduler();

        // Wire the circular dependency between OrderService and ReservationScheduler.
        orderService.setReservationScheduler(reservationScheduler);
        reservationScheduler.setOrderService(orderService);

        // ---- Startup recovery: DB is the source of truth for the 10-minute window ----
        orderService.recoverPendingOrdersOnStartup();

        // ---- REST API (java.net.httpserver, no Spring MVC) ----
        HttpServer server = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        server.createContext("/api/customers", new CustomerController(customerService));
        server.createContext("/api/orders", new OrderController(orderService));
        server.createContext("/api/reservations", new ReservationController(reservationService));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("REST API listening on http://localhost:" + HTTP_PORT);

        // Graceful shutdown of the scheduler and HTTP server on JVM exit.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            server.stop(1);
            reservationScheduler.shutdown();
        }));

        // ---- Swing GUI ----
        SwingUtilities.invokeLater(() -> {
            CustomerLoginFrame loginFrame = new CustomerLoginFrame(customerService, orderService);
            loginFrame.setVisible(true);
        });
    }
}