package at.tobi.cardreadertest

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import io.vertx.mysqlclient.MySQLConnectOptions
import io.vertx.mysqlclient.MySQLPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.Tuple

val vertx = Vertx.vertx()
val connectOptions = MySQLConnectOptions()
    .setPort(3307)
    .setHost("10.0.0.123")
    .setDatabase("Zeitmanagement")
    .setUser("timemanagement")
    .setPassword("")

val poolOptions = PoolOptions().setMaxSize(5)
val client = MySQLPool.pool(vertx, connectOptions, poolOptions)

suspend fun getTokenFromUid(uid: String): String {
    try {
        val result = client.preparedQuery("SELECT * FROM cards WHERE uid=?").execute(Tuple.of(uid)).await()
        for (row in result) {
            return row.getString("fcm_token") ?: ""
        }
    } catch (e: Exception) {
        println("Fehler: ${e.message}")
    }
    return ""
}

// Dies wird am Ende des Programms aufgerufen, um den Pool zu schließen.
fun shutdown() {
    client.close()
    vertx.close()
}
