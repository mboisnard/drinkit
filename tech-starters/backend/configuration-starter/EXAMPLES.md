``` kotlin
object BusinessConfiguration {

    /**
     * Maximum number of cellars a user can create.
     */
    object MaxCellarsPerUser : ConfigurationKey<Int>

    /**
     * Enable premium features for all users.
     */
    object PremiumFeaturesEnabled : ConfigurationKey<Boolean>

    /**
     * Default cellar capacity in bottles.
     */
    object DefaultCellarCapacity : ConfigurationKey<Int>

    /**
     * Enable wine recommendation engine.
     */
    object RecommendationEngineEnabled : ConfigurationKey<Boolean>
}
```

``` kotlin
object MailConfiguration {

    /**
     * Complete SMTP server configuration.
     *
     * This is a complex configuration object that includes all SMTP settings:
     * - host
     * - port
     * - username
     * - password
     * - useTls
     *
     * Stored as JSON, retrieved as a typed SmtpConfig object.
     */
    object Smtp : ConfigurationKey<SmtpConfig>
}

data class SmtpConfig(
    val host: String,
    val port: Int,
    val username: String? = null,
    val password: String? = null,
    val useTls: Boolean = false
) {
    companion object {
        /**
         * Default SMTP configuration for development.
         */
        val DEFAULT = SmtpConfig(
            host = "localhost",
            port = 25,
            useTls = false
        )
    }
}
```
``` kotlin
object OcrConfiguration {

    /**
     * OCR provider priority order as comma-separated list (e.g., "vertex-ai,tesseract")
     *
     * The first available provider in the list will be used.
     * If a provider fails, the next one in the list is tried.
     */
    object ProviderPriority : ConfigurationKey<String>

    /**
     * Enable Tesseract provider
     */
    object TesseractEnabled : ConfigurationKey<Boolean>

    /**
     * Enable Vertex AI provider
     */
    object VertexAiEnabled : ConfigurationKey<Boolean>

    /**
     * Vertex AI project ID (GCP project ID)
     */
    object VertexAiProjectId : ConfigurationKey<String>
}
```