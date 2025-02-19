package at.tobi.cardreadertest

import com.google.auth.oauth2.GoogleCredentials
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.FileInputStream
import java.nio.file.Paths

class MainVerticle : CoroutineVerticle() {

    private var serverStarted = false
    private lateinit var webClient: WebClient

    override suspend fun start() {
        val server = vertx.createHttpServer()
        val router = Router.router(vertx)
        webClient = WebClient.create(vertx)

        router.post("/api/uid").handler { context: RoutingContext ->
            context.request().bodyHandler { body ->
                launch {
                    val jsonBody = body.toJsonObject()
                    val uid = jsonBody.getString("uid")

                    try {
                        val token = getTokenFromUid(uid)
                        println("Token: $token")

                        if (token.isNotEmpty()) {

                            val url = javaClass.classLoader.getResource("service_account.json")
                            val serviceAccountJsonPath = Paths.get(url.toURI()).toString()
                            sendPushNotification(token, "Die Karte wurde gescannt", uid, serviceAccountJsonPath)
                            context.response().setStatusCode(200).end(JsonObject().put("message", "Push gesendet").encodePrettily())
                        } else {
                            context.response().setStatusCode(400).end(JsonObject().put("error", "Token nicht gefunden").encodePrettily())
                        }
                    } catch (e: Exception) {
                        context.response().setStatusCode(500).end(JsonObject().put("error", e.message).encodePrettily())
                    }
                }
            }
        }

        server.requestHandler(router)

        try {
            server.listen(8888).coAwait()
            serverStarted = true
            println("HTTP-Server gestartet auf Port 8888")
        } catch (e: Exception) {
            println("Fehler beim Starten des HTTP-Servers")
            e.printStackTrace()
        }
    }

    private fun getAccessToken(serviceAccountJsonPath: String): String {
        val credentials = GoogleCredentials.fromStream(FileInputStream(serviceAccountJsonPath))
            .createScoped("https://www.googleapis.com/auth/firebase.messaging")
        credentials.refreshIfExpired()
        return credentials.accessToken.tokenValue
    }

    private suspend fun sendPushNotification(token: String, title: String, body: String, serviceAccountJsonPath: String) {
        val fcmUrl = "https://fcm.googleapis.com/v1/projects/cardreadertest-2c710/messages:send"

        val accessToken = getAccessToken(serviceAccountJsonPath)

        val messageJson = JsonObject()
            .put("message", JsonObject()
                .put("token", token)
                .put("notification", JsonObject()
                    .put("title", title)
                    .put("body", body)
                )
                .put("data", JsonObject()
                    .put("extra_key", "extra_value")
                )
            )

        val response = webClient.postAbs(fcmUrl)
            .putHeader("Authorization", "Bearer $accessToken")
            .putHeader("Content-Type", "application/json")
            .sendJsonObject(messageJson)
            .await()

        if (response.statusCode() == 200) {
            println("Push-Benachrichtigung erfolgreich gesendet: ${response.bodyAsString()}")
        } else {
            println("Fehler beim Senden der Push-Benachrichtigung: ${response.statusCode()} - ${response.bodyAsString()}")
        }
    }

    override suspend fun stop() {
        if (serverStarted) {
            try {
                println("Server läuft weiterhin... Manuelles Stoppen erforderlich!")
            } catch (e: Exception) {
                println("Fehler beim Stoppen des HTTP-Servers")
                e.printStackTrace()
            }
        } else {
            println("HTTP-Server war nicht gestartet")
        }
    }
}

fun main() {
    val vertx = Vertx.vertx()

    runBlocking {
        try {
            val deploymentId = vertx.deployVerticle(MainVerticle()).coAwait()
            println("Verticle erfolgreich deployt: $deploymentId")

            Runtime.getRuntime().addShutdownHook(Thread {
                println("Das Programm wurde beendet.")
            })
        } catch (e: Exception) {
            println("Fehler beim Deployen des Verticles")
            e.printStackTrace()
        }
    }
}
