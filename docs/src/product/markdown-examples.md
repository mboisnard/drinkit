# Markdown Extension Examples

This page demonstrates some of the built-in markdown extensions provided by VitePress.

## Syntax Highlighting

VitePress provides Syntax Highlighting powered by [Shiki](https://github.com/shikijs/shiki), with additional features like line-highlighting:

**Input**

````md
```js{4}
export default {
  data () {
    return {
      msg: 'Highlighted!'
    }
  }
}
```
````

**Output**

```js{4}
export default {
  data () {
    return {
      msg: 'Highlighted!'
    }
  }
}
```

## Custom Containers

**Input**

```md
> [!NOTE]
> This is an info box.

> [!TIP]
> This is a tip.

> [!WARNING]
> This is a warning.

> [!CAUTION]
> This is a dangerous warning.

<details>
<summary>Details</summary>

This is a details block.

</details>
```

**Output**

> [!NOTE]
> This is an info box.

> [!TIP]
> This is a tip.

> [!WARNING]
> This is a warning.

> [!CAUTION]
> This is a dangerous warning.

<details>
<summary>Details</summary>

This is a details block.

</details>

## More

Check out the documentation for the [full list of markdown extensions](https://vitepress.dev/guide/markdown).
