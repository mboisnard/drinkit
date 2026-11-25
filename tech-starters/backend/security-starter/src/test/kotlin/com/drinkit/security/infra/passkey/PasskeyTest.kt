package com.drinkit.security.infra.passkey

import com.drinkit.security.Browser
import com.drinkit.security.InMemoryCurrentUser
import com.drinkit.security.InMemoryUserAccounts
import com.drinkit.security.PASSKEY_RP_ID
import com.drinkit.security.SecuredApplicationTest
import com.drinkit.security.SpySignInCodeSender
import com.drinkit.security.TestAccount
import com.drinkit.security.TestAccounts
import com.drinkit.security.UserAccount
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.UserCredentialRepository
import org.springframework.test.web.servlet.MockMvc
import tools.jackson.databind.json.JsonMapper

@SecuredApplicationTest
internal class PasskeyTest(
    private val mockMvc: MockMvc,
    private val accounts: InMemoryUserAccounts,
    private val codes: SpySignInCodeSender,
    private val userEntities: PublicKeyCredentialUserEntityRepository,
    private val userCredentials: UserCredentialRepository,
) {

    private val testAccounts = TestAccounts(accounts)
    private val authenticator = TestAuthenticator()
    private val json = JsonMapper()

    @Test
    fun `asks the authenticator for a passkey that shows the login and verifies the user`() {
        // Given
        val account = testAccounts.user()
        val browser = signedIn(account.login)

        // When
        val options = json.readTree(browser.passkeyOptions("/webauthn/register/options"))

        // Then
        options["rp"]["id"].asString() shouldBe PASSKEY_RP_ID
        options["user"]["name"].asString() shouldBe account.login
        options["user"]["displayName"].asString() shouldBe account.login
        options["user"]["id"].asString() shouldNotBe account.id
        options["authenticatorSelection"]["residentKey"].asString() shouldBe "required"
        options["authenticatorSelection"]["userVerification"].asString() shouldBe "required"
    }

    @Test
    fun `signs in with a passkey without any email, under a new session id`(output: CapturedOutput) {
        // Given
        val account = testAccounts.user()
        signedIn(account.login).registerPasskey(authenticator).andExpect { status { isOk() } }
        val browser = Browser(mockMvc, codes)
        browser.passkeyOptions("/webauthn/authenticate/options")
        val anonymousSessionId = browser.session.id

        // When
        browser.signInWithPasskey(authenticator).andExpect { status { isOk() } }

        // Then
        browser.session.id shouldNotBe anonymousSessionId
        browser.get("/api/cellars/me").andExpect { jsonPath("$.id") { value(account.id) } }
        accounts.signInsOf(account.id) shouldBe 2
        output.out shouldContain "authn_login_success:${account.id},method=passkey"
    }

    @Test
    fun `keeps the roles of the account, and reloads them at refresh`() {
        // Given
        val account = testAccounts.user()
        val browser = signedIn(account.login)
        browser.registerPasskey(authenticator)
        browser.signInWithPasskey(authenticator).andExpect { status { isOk() } }
        accounts.save(UserAccount(account.id, account.login, setOf("ROLE_USER", "ROLE_ADMIN")))

        // When
        browser.post("/api/cellars/refresh").andExpect { status { isOk() } }

        // Then
        browser.get("/api/admin/stats").andExpect {
            status { isOk() }
            content { string(account.id) }
        }
    }

    @Test
    fun `refuses a passkey signed in a page of another origin, and logs it`(output: CapturedOutput) {
        // Given
        signedIn(testAccounts.user().login).registerPasskey(authenticator)
        authenticator.movesTo("https://drinkit.evil")

        // When
        val signIn = Browser(mockMvc, codes).signInWithPasskey(authenticator)

        // Then
        signIn.andExpect { status { isUnauthorized() } }
        output.out shouldContain "authn_login_fail"
    }

    @Test
    fun `refuses a passkey that signs for another site`() {
        // Given
        signedIn(testAccounts.user().login).registerPasskey(authenticator)
        authenticator.signsFor("drinkit.evil")

        // When
        val signIn = Browser(mockMvc, codes).signInWithPasskey(authenticator)

        // Then
        signIn.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `refuses to create a passkey in a page of another origin`() {
        // Given
        val account = testAccounts.user()
        val browser = signedIn(account.login)
        authenticator.movesTo("https://drinkit.evil")

        // When
        val creation = browser.registerPasskey(authenticator)

        // Then
        creation.andExpect { status { isBadRequest() } }
        passkeysOf(account).shouldBeEmpty()
    }

    @Test
    fun `refuses to create a passkey that already exists`() {
        // Given
        val account = testAccounts.user()
        val browser = signedIn(account.login)
        val creation = authenticator.create(browser.passkeyOptions("/webauthn/register/options"), "Ada's phone")
        browser.savePasskey(creation)
        browser.passkeyOptions("/webauthn/register/options")

        // When
        val again = browser.savePasskey(creation)

        // Then
        again.andExpect { status { isBadRequest() } }
        passkeysOf(account) shouldHaveSize 1
    }

    @Test
    fun `refuses a passkey that did not verify its user`() {
        // Given
        signedIn(testAccounts.user().login).registerPasskey(authenticator)

        // When
        val signIn = Browser(mockMvc, codes).signInWithPasskey(authenticator, verifiesUser = false)

        // Then
        signIn.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `lists the passkeys of the signed-in user only, with their label`() {
        // Given
        val account = testAccounts.user()
        signedIn(account.login).registerPasskey(authenticator, label = "Ada's phone")
        signedIn(testAccounts.user().login).registerPasskey(TestAuthenticator(), label = "Grace's laptop")

        // When
        val passkeys = passkeysOf(account)

        // Then
        passkeys.map { it.label } shouldBe listOf("Ada's phone")
    }

    @Test
    fun `removes a passkey, which signs in no more`() {
        // Given
        val account = testAccounts.user()
        val browser = signedIn(account.login)
        browser.registerPasskey(authenticator)

        // When
        browser.delete("/webauthn/register/${passkeysOf(account).single().id}").andExpect { status { isNoContent() } }

        // Then
        passkeysOf(account).shouldBeEmpty()
        Browser(mockMvc, codes).signInWithPasskey(authenticator).andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `refuses to remove the passkey of another user`() {
        // Given
        val owner = testAccounts.user()
        signedIn(owner.login).registerPasskey(authenticator)
        val other = signedIn(testAccounts.user().login)

        // When
        val removal = other.delete("/webauthn/register/${passkeysOf(owner).single().id}")

        // Then
        removal.andExpect { status { isForbidden() } }
        passkeysOf(owner) shouldHaveSize 1
    }

    @Test
    fun `lets no anonymous visitor create or remove a passkey`() {
        // Given
        val browser = Browser(mockMvc, codes)

        // When & Then
        browser.post("/webauthn/register/options").andExpect { status { isBadRequest() } }
        browser.post("/webauthn/register").andExpect { status { isUnauthorized() } }
        browser.delete("/webauthn/register/any").andExpect { status { isUnauthorized() } }
    }

    private fun passkeysOf(account: TestAccount) =
        SpringPasskeys(userEntities, userCredentials, InMemoryCurrentUser(accounts).also { it.signIn(account.id) })
            .ofCurrentUser()

    private fun signedIn(login: String) = Browser(mockMvc, codes).also {
        it.signIn(login).andExpect { status { isNoContent() } }
    }
}
