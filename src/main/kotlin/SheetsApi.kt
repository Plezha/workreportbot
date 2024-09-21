import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.auth.oauth2.TokenResponseException
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver.Builder
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.AppendValuesResponse
import com.google.api.services.sheets.v4.model.ValueRange
import java.io.*
import java.security.GeneralSecurityException


object SheetsApi {
    private const val SPREADSHEET_ID = "1Kag03Cwun5rNlPlG6yVkSGXA2KZyJOprPhIHIb953no"
    private const val APPLICATION_NAME = "Google Sheets API For Work Report Bot"
    private val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()
    private const val TOKENS_DIRECTORY_PATH = "tokens"
    private val SCOPES = listOf<String>(SheetsScopes.SPREADSHEETS,  SheetsScopes.SPREADSHEETS_READONLY)
    private const val CREDENTIALS_FILE_PATH = "/credentials.json"
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private var service = buildService(httpTransport)

    @Throws(IOException::class, GeneralSecurityException::class)
    @JvmStatic
    fun appendRectangularDataToSheet(values: List<List<String?>>) {
        service.appendValues(values)
    }

    fun appendRowDataToSheet(values: List<String?>) = appendRectangularDataToSheet(listOf(values))

    fun areTokensValid(): Boolean {
        return try {
            service.spreadsheets().get(SPREADSHEET_ID).execute()
            true // Success: Tokens are valid
        } catch (e: TokenResponseException) {
            println("Token validation failed: ${e.message}")
            false // Failure: Tokens are likely invalid
        }
    }

    fun refreshCredentials() {
        File(TOKENS_DIRECTORY_PATH).deleteRecursively()
        service = buildService(httpTransport)
    }

    /**
     * Creates an authorized Credential object.
     *
     * @param httpTransport The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    @Throws(IOException::class)
    private fun getCredentials(httpTransport: NetHttpTransport): Credential {
        // Load client secrets.
        val inStream = SheetsApi::class.java.getResourceAsStream(CREDENTIALS_FILE_PATH)
            ?: throw FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH)
        val clientSecrets: GoogleClientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(inStream))

        // Build flow and trigger user authorization request.
        val flow: GoogleAuthorizationCodeFlow = GoogleAuthorizationCodeFlow.Builder(
            httpTransport, JSON_FACTORY, clientSecrets, SCOPES
        )
            .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()
        val receiver: LocalServerReceiver = Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    private fun buildService(httpTransport: NetHttpTransport): Sheets =
        Sheets.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport))
            .setApplicationName(APPLICATION_NAME)
            .build()

    private fun Sheets.appendValues(
        values: List<List<String?>>
    ): AppendValuesResponse? = spreadsheets().values()
        .append(SPREADSHEET_ID, "A1", ValueRange().setValues(values))
        .setValueInputOption("RAW")
        .execute()

}