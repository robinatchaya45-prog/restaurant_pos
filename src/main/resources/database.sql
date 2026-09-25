-- ============================================================
-- Restaurant POS Database
-- ============================================================

CREATE DATABASE IF NOT EXISTS restaurant_pos
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE restaurant_pos;

-- ============================================================
-- INGREDIENTS
-- ============================================================

CREATE TABLE IF NOT EXISTS ingredients (
    ingredient_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    available_stock DECIMAL(10,2) NOT NULL DEFAULT 0
) ENGINE=InnoDB;

-- ============================================================
-- CUSTOMERS
-- ============================================================

CREATE TABLE IF NOT EXISTS customers (
    customer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    CONSTRAINT uq_customers_phone UNIQUE (phone)
) ENGINE=InnoDB;

-- ============================================================
-- ORDERS
-- ============================================================

CREATE TABLE IF NOT EXISTS orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    table_no INT NOT NULL,
    order_time DATETIME NOT NULL,
    lock_time DATETIME NULL,
    finish_time DATETIME NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,

    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id),

    CONSTRAINT chk_orders_table_no
        CHECK (table_no > 0),

    CONSTRAINT chk_orders_status
        CHECK (status IN ('PENDING', 'LOCKED', 'CANCELLED', 'COMPLETED')),

    INDEX idx_orders_status_time (status, order_time),
    INDEX idx_orders_customer (customer_id)
) ENGINE=InnoDB;

-- ============================================================
-- ORDER ITEMS
-- ============================================================

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_order_items_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_order_items_price
        CHECK (price >= 0),

    INDEX idx_order_items_order (order_id),
    INDEX idx_order_items_item (item_id)
) ENGINE=InnoDB;

-- ============================================================
-- ORDER RESERVATIONS
-- ============================================================

CREATE TABLE IF NOT EXISTS order_reservations (
    reservation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    reserved_quantity DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT fk_order_reservations_order
        FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_order_reservations_ingredient
        FOREIGN KEY (ingredient_id)
        REFERENCES ingredients(ingredient_id),

    CONSTRAINT chk_order_reservations_qty
        CHECK (reserved_quantity >= 0),

    CONSTRAINT chk_order_reservations_status
        CHECK (status IN ('ACTIVE', 'RELEASED', 'CONFIRMED')),

    INDEX idx_order_reservations_order (order_id),
    INDEX idx_order_reservations_ingredient (ingredient_id),
    INDEX idx_order_reservations_status (status)
) ENGINE=InnoDB;

-- ============================================================
-- SAMPLE INGREDIENT DATA
-- ============================================================

INSERT INTO ingredients
    (ingredient_id, name, available_stock)
VALUES
    (1, 'Chicken', 500.00),
    (2, 'Veggie Patty', 200.00),
    (3, 'Seafood Mix', 0.00),
    (4, 'Lettuce', 300.00)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    available_stock = VALUES(available_stock);

-- ============================================================
-- SAMPLE CUSTOMER DATA
-- ============================================================

INSERT INTO customers
    (customer_id, name, phone)
VALUES
    (1, 'Alice Tan', '555-0101'),
    (2, 'Brian Lee', '555-0102'),
    (3, 'Chandra Rao', '555-0103')
ON DUPLICATE KEY UPDATE
    name = VALUES(name);

-- ============================================================
-- SAMPLE ORDER DATA
-- ============================================================

INSERT INTO orders
    (order_id, customer_id, table_no, order_time, status, total_amount)
VALUES
    (1, 1, 5, NOW() - INTERVAL 15 MINUTE, 'LOCKED', 25.98),
    (2, 2, 3, NOW() - INTERVAL 2 MINUTE, 'PENDING', 9.50)
ON DUPLICATE KEY UPDATE
    table_no = VALUES(table_no),
    status = VALUES(status);

-- ============================================================
-- SAMPLE ORDER ITEMS
-- ============================================================

INSERT INTO order_items
    (id, order_id, item_id, quantity, price)
VALUES
    (1, 1, 1, 2, 12.99),
    (2, 2, 2, 1, 9.50)
ON DUPLICATE KEY UPDATE
    quantity = VALUES(quantity),
    price = VALUES(price);

-- ============================================================
-- SAMPLE RESERVATIONS
-- ============================================================

INSERT INTO order_reservations
    (reservation_id, order_id, ingredient_id, reserved_quantity, status)
VALUES
    (1, 1, 1, 2.00, 'CONFIRMED'),
    (2, 2, 2, 1.00, 'ACTIVE')
ON DUPLICATE KEY UPDATE
    status = VALUES(status);