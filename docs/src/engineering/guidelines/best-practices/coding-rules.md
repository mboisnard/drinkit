# Coding Rules

## 🧐 Strong Typing for Clarity and Safety

Don't use primitive types like `string` or `int` for your core business concepts. Instead, create specific types that represent them directly. For example, use a `UserId` type instead of a raw `string`, or a `Money` object instead of a `BigDecimal`.

This practice provides several key benefits:

- **Prevents Mistakes**: It makes it impossible to accidentally pass an `CellarId` where a `UserId` is expected. The compiler becomes your first line of defense.
- **Embeds Business Rules**: You can build validation directly into the type. For example, an `EmailAddress` type can ensure it's always in a valid format upon creation.
- **Improves Readability**: It makes your code self-documenting. A function signature like `sendInvoice(CustomerID customerId, OrderID orderId)` is much clearer than `sendInvoice(string customerId, string orderId)`.
- **Clarifies Intent**: It clearly shows which business concepts are being manipulated, making the domain logic easier to follow.

## 🔁 Design for Idempotency

Design your APIs and message listeners (e.g., RabbitMQ consumers) to be **idempotent**. This means they can be called multiple times with the same input without creating duplicate data or causing errors. Distributed systems often have "at-least-once" delivery guarantees, so a message or request might occasionally arrive more than once.

Here’s how to achieve idempotency:

- **Check First**: Before performing an action, always check if the system is already in the desired state. For example, before creating a user, check if a user with that email already exists.
- **Succeed Silently**: If the desired state is already met, simply return a success response. Don't throw an error like "Cellar already deleted." Consider the duplicate request a success.
- **Use Idempotency Keys**: When calling external services that support it, provide a unique idempotency key for each operation. This allows the receiving service to safely handle your repeated requests.

## 🗣️ Speak the Language of the Domain

Your class, method, and variable names should reflect the business domain, not their technical implementation. The goal is for the code to be readable and understandable to anyone familiar with the business, even if they aren't a developer.

Avoid adding technical pattern suffixes like `UseCase`, `Service`, `Repository`, or `DTO` to your names.

- Prefer: `CreateUser` or `RegisterNewUser`  
  Instead of: `CreateUserUseCase` or `CreateUserService`
- Prefer: `Users` or `Customers`  
  Instead of: `UserRepository` or `CustomerRepository`

## ✅ Write Concise and Meaningful Test Names

Keep your test names focused on the specific behavior being tested. Omit filler words like `should`, `when`, `given`, or `returns` if they don't add real value. The test's structure and assertions often make these words redundant and just add noise.

- **Good**: `throws_error_if_password_is_too_short`
- **Less good**: `test_that_it_should_return_an_error_if_the_password_is_too_short`

## 🛠️ Let Testability Drive Your Design

Even if you don't strictly follow **Test-Driven Development (TDD)**, always consider how you will test your code as you write it.

This mindset is a powerful tool that naturally shapes your design for the better. Code that is easy to test is almost always loosely coupled, modular, and well-structured. It forces you to think about dependencies and separate concerns.

If you think of new edge cases or scenarios to test while in the middle of coding, don't let the idea slip away. While writing code you may come up with more cases to test. Write them down as `TODO` comments, and make sure you write tests for them when you are ready.
