# Mono Repo

## 🚀 Why we choose a **Monorepo**?

We opted for a **monorepo** to centralize and simplify the management of:

- 🛠️ **Application code**: all apps (frontend & backend) live in the same repository, making it easier to navigate and understand module dependencies.
- ⚙️ **Gradle conventions**: shared build configurations ensure consistency and simplify maintenance.
- 🗂️ **Documentation**: functional and technical documentation remain close to the code, reducing divergence risks.
- 📦 **Shared libraries (tech-starters)**: internal tools, starters, and reusable components are transparently shared without the overhead of external versioning and publishing.

---

## 🌟 Benefits

✅ **Immediate visibility of change impact**
- Changes in a shared library are instantly reflected in consuming projects.
- The compiler signals immediately if a consuming project is impacted.
- Enables confident refactoring with immediate feedback via compilation and tests.

✅ **Reduced version management overhead**
- No need to publish internal artifacts for each update.
- Continuous updates without complex internal dependency deployments.

✅ **Easier onboarding**
- New contributor access code, shared libraries, and conventions in one place, accelerating ecosystem understanding.

✅ **Technical alignment and consistency**
- Technical improvements are applied consistently and immediately across projects.

---

## ⚖️ Known Limitations

⚠️ **Change coordination required**:
In a monorepo, developers can make changes affecting other teams directly. This speeds up alignment but requires informing impacted teams to avoid surprises.
Not a big issue with this little Github project but let's keep it in mind.

⚠️ **Potentially longer build times**:  
Building the entire repository can take longer than isolated builds, but is currently negligible for our project size.

⚠️ **Not suited for very large scale**:  
Monorepos can become harder to manage as they grow in size or with many independent teams, but for our current project scale, the trade-off is acceptable.

---

## 🗒️ Best Practices

✅ Keep modules **decoupled** where possible to minimize unnecessary rebuilds.  
✅ Use **clear module naming** to ease navigation.  
✅ Automate **pre-commit checks and CI pipelines** to detect issues early.  
✅ Keep **documentation near code** and up to date with changes.  
✅ Use **build caching** technics to reduce build times.  
✅ Regularly review **dependencies between modules** to avoid tight coupling.  
✅ Communicate with other contributors when making **breaking changes** in shared libraries.  
✅ Regularly **clean up unused modules** to keep the repository lean.

---

::: tip
More details in some Medium articles [like here](https://medium.com/streamdal/mostly-terrible-the-monorepo-5db704f76bdb)
:::