package com.webtic.qrprint.util

import android.os.StrictMode
import android.util.Log
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException

private const val TAG = "ConnectionManager"
private const val LOGIN_TIMEOUT_SEC = 15
private const val SOCKET_TIMEOUT_SEC = 30
private const val ISVALID_TIMEOUT_SEC = 5

class ConnectionManager {

    private var connection: Connection? = null
    private var savedServer: String = ""
    private var savedDatabase: String = ""
    private var savedUser: String = ""
    private var savedPass: String = ""

    private fun buildConnectionUrl(server: String, database: String, user: String, pass: String): String =
        "jdbc:jtds:sqlserver://$server;databaseName=$database;user=$user;password=$pass;" +
        "loginTimeout=$LOGIN_TIMEOUT_SEC;socketTimeout=$SOCKET_TIMEOUT_SEC;"

    private fun isConnectionValid(): Boolean =
        try {
            connection?.let { !it.isClosed && it.isValid(ISVALID_TIMEOUT_SEC) } ?: false
        } catch (e: Exception) {
            false
        }

    private fun reconnect(): Boolean {
        if (savedServer.isEmpty()) return false
        return try {
            Log.w(TAG, "Reconnecting to database...")
            connection?.runCatching { close() }
            DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SEC)
            connection = DriverManager.getConnection(
                buildConnectionUrl(savedServer, savedDatabase, savedUser, savedPass)
            )
            Log.i(TAG, "Reconnect successful.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Reconnect failed: ${e.message}")
            false
        }
    }

    fun tryLogin(server: String, database: String, user: String, pass: String): LoginResponse {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        return try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver")
            DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SEC)
            connection = DriverManager.getConnection(buildConnectionUrl(server, database, user, pass))
            savedServer = server
            savedDatabase = database
            savedUser = user
            savedPass = pass
            LoginSuccess
        } catch (e: SQLException) {
            if (e.errorCode == 18456) {
                AuthenticationError(e)
            } else {
                e.printStackTrace()
                NetworkError(e)
            }
        } catch (e: Exception) {
            LoginError(e)
        }
    }

    fun executeQuery(sql: String): QueryResponse {
        if (!isConnectionValid() && !reconnect()) {
            return QueryError(SQLException("Nincs adatbázis-kapcsolat"))
        }
        return try {
            QuerySuccess(connection!!.createStatement().executeQuery(sql))
        } catch (e: Exception) {
            Log.w(TAG, "Query hiba, újracsatlakozás: ${e.message}")
            if (reconnect()) {
                try {
                    QuerySuccess(connection!!.createStatement().executeQuery(sql))
                } catch (e2: Exception) {
                    Log.e(TAG, "Query hiba reconnect után: ${e2.message}")
                    QueryError(e2)
                }
            } else {
                QueryError(e)
            }
        }
    }

    fun execute(sql: String): Boolean {
        if (!isConnectionValid() && !reconnect()) return false
        return try {
            connection!!.createStatement().use { it.execute(sql) }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Execute hiba, újracsatlakozás: ${e.message}")
            if (reconnect()) {
                try {
                    connection!!.createStatement().use { it.execute(sql) }
                    true
                } catch (e2: Exception) {
                    Log.e(TAG, "Execute hiba reconnect után: ${e2.message}")
                    false
                }
            } else {
                false
            }
        }
    }

}

sealed class LoginResponse
object LoginSuccess : LoginResponse()
class AuthenticationError(val exception: SQLException) : LoginResponse()
class NetworkError(val exception: SQLException) : LoginResponse()
class LoginError(val exception: Exception) : LoginResponse()

sealed class QueryResponse
class QuerySuccess(val resultSet: ResultSet) : QueryResponse()
class QueryError(val exception: Exception) : QueryResponse()