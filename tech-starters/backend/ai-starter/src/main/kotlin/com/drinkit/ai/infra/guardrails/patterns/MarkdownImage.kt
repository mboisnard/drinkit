package com.drinkit.ai.infra.guardrails.patterns

/** An image the client fetches as soon as it shows the text: its URL can carry data out. */
internal val MARKDOWN_IMAGE = Regex("""!\[[^\]]*]\(https?://[^)]*\)""")
