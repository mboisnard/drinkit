# SOLID Principles

✨**SOLID principles** are considered classics for a reason: they help us write maintainable software by enforcing clear modular design and separation of concerns.

## 🛠️ **S — Single Responsibility Principle (SRP)**

> A class or module should have **only one reason to change**.  
> It should focus on a single responsibility, making the code easier to **read, test, and maintain**.

## 📈 **O — Open/Closed Principle (OCP)**

> Code should be **open for extension but closed for modification**.  
> You should be able to add new functionality without modifying existing code by using **inheritance, interfaces, or composition** to avoid side effects.

## 🔄 **L — Liskov Substitution Principle (LSP)**

> Derived classes should be **substitutable for their base classes without altering the correctness of the program**.  
> If a subclass replaces its superclass, the expected behavior should remain **consistent**, ensuring logical and robust hierarchies.

## ✂️ **I — Interface Segregation Principle (ISP)**

> Prefer **many specific interfaces over a single general-purpose interface**.  
> This allows classes to implement **only the methods they need**, reducing unnecessary coupling and improving flexibility.

## 🔌 **D — Dependency Inversion Principle (DIP)**

> High-level modules should not depend on low-level modules; both should depend on **abstractions**.  
> **Depending on abstractions rather than concrete implementations** improves testability and makes components easier to replace without breaking the system.
