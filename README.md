Project Overview
The Credit Module Project is a RESTful API that provides functionality for managing loans. It allows administrators and customers to perform operations such as creating loans, listing loans, viewing loan installments, and making payments. The project is built with Spring Boot, Spring Security, and uses a file-based H2 Database for persistent data storage.

Features
Create Loan: Create a new loan for a customer with validations on interest rate, installments, and credit limits.
List Loans: List loans for a specific customer.
View Installments: Retrieve installments for a specific loan.
Make Payments: Pay loan installments with rules for penalties and discounts based on payment timing.

Role-Based Access:
Admin Role: Access to all APIs for all customers.
Customer Role: Access restricted to APIs for their own loans.

Project Setup
Prerequisites
Java 17
Maven installed
An IDE (e.g., IntelliJ IDEA)
Postman for testing
Basic knowledge of REST APIs

Setup Instructions
Step 1: Clone the Repository
Clone the project to your local machine:

git clone https://github.com/your-username/credit-module.git
cd credit-module

Step 2: Build the Project
Run the following Maven command to build the project:
mvn clean install

Step 3: Run the Application
Run the project with the following command:
mvn spring-boot:run
The application will start on http://localhost:8080.

Step 4: Access the H2 Console
Access the H2 Database Console at:
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./data/testdb
Username: sa
Password: (leave blank)
This file-based database ensures persistent storage for testing and development purposes.

Authentication Details
The application uses Basic Authentication. The default credentials are preloaded by the DataLoader class:

Role	Username	Password
Admin	admin	adminpass
Customer	customer	customerpass

API Endpoints
1. Create Loan
Endpoint:
POST http://localhost:8080/api/loans/create
Authorization: Admin or Customer (only for their own customerId)

Request Parameters:
Parameter	Type	Required	Description
customerId	Long	Yes	ID of the customer.
amount	Decimal	Yes	Loan amount.
interestRate	Decimal	Yes	Interest rate (0.1–0.5).
installments	Integer	Yes	Installment count (6, 9, 12, 24).
Example Request in Postman:
POST http://localhost:8080/api/loans/create?customerId=1&amount=10000&interestRate=0.1&installments=12

Response:
Loan created successfully
2. List Loans
Endpoint:
GET http://localhost:8080/api/loans/list
Authorization: Admin or Customer (only for their own customerId)

Request Parameters:
Parameter	Type	Required	Description
customerId	Long	Yes	ID of the customer.
Example Request:
GET http://localhost:8080/api/loans/list?customerId=1
Response:
[
  {
    "id": 1,
    "customerId": 1,
    "loanAmount": 11000,
    "numberOfInstallments": 12,
    "createDate": "2024-12-01",
    "isPaid": false
  }
]
3. View Installments
Endpoint:
GET http://localhost:8080/api/loans/{loanId}/installments
Authorization: Admin or Customer (only for their own loanId)

Path Parameter:
Parameter	Type	Required	Description
loanId	Long	Yes	ID of the loan.
Example Request:
GET http://localhost:8080/api/loans/1/installments
Response:
[
  {
    "id": 1,
    "loanId": 1,
    "amount": 916.67,
    "paidAmount": 0,
    "dueDate": "2024-01-01",
    "paymentDate": null,
    "isPaid": false
  },
  {
    "id": 2,
    "loanId": 1,
    "amount": 916.67,
    "paidAmount": 0,
    "dueDate": "2024-02-01",
    "paymentDate": null,
    "isPaid": false
  }
]
4. Make Payment
Endpoint:
POST http://localhost:8080/api/loans/pay
Authorization: Admin or Customer (only for their own loanId)

Request Parameters:
Parameter	Type	Required	Description
loanId	Long	Yes	ID of the loan.
paymentAmount	Decimal	Yes	Payment amount.
Example Request:
POST http://localhost:8080/api/loans/pay?loanId=1&paymentAmount=2000
Response:
Paid 2 installments, total paid: 2000. Discount: 0. Penalty: 0. Loan fully paid: false

DataLoader (Default Data Initialization)
The project includes a DataLoader component to initialize the database with the following records:

Admin User:
Username: admin
Password: adminpass

Customer User:
Username: customer
Password: customerpass

Customer Details:
ID: 1
Name: John
Surname: Doe
Credit Limit: 50000
Used Credit Limit: 0
This data will be available immediately upon starting the application.

Testing the Application with Postman
1. Set Up Basic Authentication
In Postman, go to the Authorization tab.
Select Basic Auth.
Enter the credentials for either admin or customer.
2. Testing Scenarios
Admin User:
Test creating loans for any customer.
Test viewing and managing all loans.
Customer User:
Test creating loans only for their own customer ID.
Test listing loans and making payments for their own loans.
