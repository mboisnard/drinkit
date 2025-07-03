# Scout Rule (formerly the BoyScout Rule)


> The Boy Scouts have a rule: “**Always leave the campground cleaner than you found it**”.


If you find a mess on the ground, you clean it up regardless of who might have made it. You intentionally improve the environment for the next group of campers.

The **Scout Rule**, introduced by Robert C. Martin, is a fundamental development principle that enables **continuous improvement of an application's code**.

It is **not** about rewriting everything or launching into massive refactors, but about making **small, continuous improvements every time you touch the code**.

---

## 🚩 Why adopt the Scout Rule?

- Gradually reduce technical debt.
- Keep the codebase clearer and easier to evolve.
- Foster a team mindset of continuous improvement.
- Preserve team mental ergonomics and reduce frustration with “legacy” code.

---

## 🛠️ How to apply it in practice?

**Before committing**, ask yourself:

> Have I left this file, module, or test in a better state than I found it?

---

### ✅ Small improvements you can make:
- Rename a variable or function for clarity.
- Remove dead code.
- Slightly factor out obvious duplication.
- Add a missing simple test.
- Clarify a comment or remove it by using a clearer name.

---

### ⚠️ Avoid these traps:
- It is **not** a “big bang refactor.”
- Do not drift away from your initial task in a way that compromises progress.

## 💡 Tip

Don't hesitate to start your commits by:

```
[devscout] ...
```

to indicate that you are applying the Scout Rule in your commit.