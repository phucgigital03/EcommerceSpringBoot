
# E-Commerce Project Masterclass

## Description

This project is a professional-grade e-commerce platform built using Java
and Spring Boot. It is designed to provide a robust and scalable solution 
for online businesses, featuring functionalities such as product management,
user authentication, order processing, and payment integration.

## Setup Instructions

1. Clone the repository:
   ```bash
   git clone https://github.com/phucgigital03/EcommerceSpringBoot.git
   ```
2. Navigate to the project directory:
   ```bash
   cd eCommerceUdemy
   ```
3. Install dependencies:
   - Ensure you have Java 17+ and Maven installed.
   - Run the following command to install dependencies:
     ```bash
     mvn clean install
     ```
4. Configure the database:
   - Update the `application.properties` file with your database credentials.
   - Run the database migration scripts located in the `src/main/resources/db` directory.

5. Start the application:
   ```bash
   mvn spring-boot:run
   ```

6. Access the application:
   - Open your browser and navigate to `http://localhost:8080`.

## Usage

- **Admin Panel**: Manage products, categories, and orders.
- **User Features**: Browse products, add items to the cart, and complete purchases.
- **API Endpoints**: Use the RESTful APIs for integration with external systems.

## Contributor Guidelines

We welcome contributions to this project! To contribute:

1. Fork the repository.
2. Create a new branch for your feature or bug fix:
   ```bash
   git checkout -b feature-name
   ```
3. Commit your changes:
   ```bash
   git commit -m "Description of changes"
   ```
4. Push your branch:
   ```bash
   git push origin feature-name
   ```
5. Open a pull request and describe your changes in detail.

## License

This project is licensed under the MIT License. See the `LICENSE` file for more details.
