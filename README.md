Restaurant POS - Friend 2 Module

Customer + Order + Table Number + 10-minute Stock Reservation.


No Spring Boot / Spring Framework / Hibernate / JPA is used anywhere in
this module. Only plain Java, Java Swing, JDBC, MySQL, com.sun.net.httpserver,
Maven and Gson.


Ownership

This module owns:



model/Customer.java, model/Order.java, model/OrderItem.java, model/Reservation.java

repository/CustomerRepository.java, OrderRepository.java, OrderItemRepository.java, ReservationRepository.java

service/CustomerService.java, OrderService.java, ReservationService.java, ReservationScheduler.java

controller/CustomerController.java, OrderController.java, ReservationController.java

swing/* (Login, Menu, Cart, Table Number, Order Confirmation, Order Status)

config/DatabaseConnection.java, exception/*, inventory/InventoryService.java (contract only)


This module does not define Ingredient.java, MenuItem.java, Recipe.java,
KitchenOrder.java, Bill.java, or Payment.java - those belong to other
team members. inventory/InventoryServiceStub.java is a temporary
placeholder so this module compiles and runs standalone; swap it for
Friend 1's real implementation in Main.java once merged.


Architecture

Swing GUI  /  REST API (HttpServer)
        |
   Service Layer
        |
Repository / DAO Layer (JDBC)
        |
      MySQL

1. Install the JDK


Download and install JDK 17 or later (e.g. from https://adoptium.net).

Verify: java -version and javac -version in a terminal.


2. Install VS Code


Download from https://code.visualstudio.com and install it.


3. Install the Java Extension Pack


Open VS Code.

Go to the Extensions view (Ctrl+Shift+X).

Search for "Extension Pack for Java" (by Microsoft) and install it. This bundles Language Support for Java, Debugger for Java, Maven for Java, and the Test Runner.


4. Install Maven (if not already available)


The Java Extension Pack bundles its own Maven, but you can also install Maven separately: https://maven.apache.org/install.html

Verify: mvn -version.


5. Create / clone the project

git clone <your-shared-repo-url>
cd restaurant-pos

If starting from this delivered folder instead, simply open the
restaurant-pos folder in VS Code (File > Open Folder).


6. Configure MySQL


Install MySQL Server 8.x if you don't already have it.

Start the MySQL service.

Log in as root (or another admin user):
mysql -u root -p


7. Create the database

Run the provided script, which creates the database, tables, and sample data:


mysql -u root -p < src/main/resources/database.sql

8. Update database credentials

DatabaseConnection.java reads its configuration from environment
variables / system properties, falling back to sensible local defaults:


Setting	Env var	Default
URL	DB_URL	jdbc:mysql://localhost:3306/restaurant_pos?useSSL=false&serverTimezone=UTC
Username	DB_USER	root
Password	DB_PASSWORD	(empty)

Set your own, e.g. on macOS/Linux:


export DB_URL="jdbc:mysql://localhost:3306/restaurant_pos?useSSL=false&serverTimezone=UTC"
export DB_USER="root"
export DB_PASSWORD="your-password"

or on Windows PowerShell:


$env:DB_USER="root"
$env:DB_PASSWORD="your-password"

9. Running Maven

From the restaurant-pos folder:


mvn clean package

This compiles the module and produces a runnable shaded jar at
target/friend2-order-module.jar.


10. Running Main.java

Either:


mvn compile exec:java -Dexec.mainClass="com.restaurant.pos.friend2.Main"

or, after mvn clean package:


java -jar target/friend2-order-module.jar

or in VS Code: open Main.java and click Run above the main method.


This starts the REST API on http://localhost:8080 and opens the Swing
Customer Login window.


11. Testing the REST API

Example with curl:


# Create/find a customer
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice Tan","phone":"555-0101"}'

# Place an order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"tableNo":5,"items":[{"itemId":1,"quantity":2,"price":12.99}]}'

# Check order status
curl http://localhost:8080/api/orders/1/status

# Cancel an order (within 10 minutes)
curl -X POST http://localhost:8080/api/orders/1/cancel

12. Testing the Swing GUI


Launch the app (step 10). The Customer Login window appears.

Enter a name and phone, click Login / Continue.

Add items to the cart from the Menu screen.

Click View Cart, adjust quantities if needed, then Enter Table Number.

Enter a valid, positive table number and continue to Order Confirmation.

Click Place Order. The Order Status screen opens, showing a live countdown of the 10-minute edit/cancel window.

Within that window you can increase/decrease item quantities or cancel the order. After 10 minutes, the order is automatically locked by ReservationScheduler + OrderService.lockOrder() - no button press required.


Concurrency & Reservation Safety


Stock reservation is protected by three layered mechanisms: a synchronized critical section in ReservationService, a JDBC transaction (setAutoCommit(false) ... commit()/rollback()), and SELECT ... FOR UPDATE row locking in InventoryService.

Ingredient rows are always locked in a deterministic (sorted) order to avoid deadlocks between concurrent orders needing overlapping ingredients.

The 10-minute lock is scheduled via ScheduledExecutorService (ReservationScheduler), but the database order_time is always the source of truth: OrderService.lockOrder() re-verifies elapsed time before locking, and OrderService.recoverPendingOrdersOnStartup() re-locks/reschedules orders after an application restart.


Git workflow (shared repository)

git pull
# ... make changes only inside files this module owns ...
git add .
git commit -m "Implement customer order reservation module"
git push

Do not modify Friend 1's or Friend 3's files unless integration requires
an explicitly agreed interface change.