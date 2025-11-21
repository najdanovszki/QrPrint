package com.webtic.qrprint.util

import android.os.StrictMode
import android.util.Log
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException

class ConnectionManager {

    private lateinit var connection: Connection

    fun tryLogin(server: String, database: String, user: String, pass: String): LoginResponse {
        val policy = StrictMode.ThreadPolicy.Builder()
            .permitAll().build()
        StrictMode.setThreadPolicy(policy)
        return try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver")
            val connectionUrl =
                "jdbc:jtds:sqlserver://$server;databaseName=$database;user=$user;password=$pass;"
            connection = DriverManager.getConnection(connectionUrl)
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
        return try {
            QuerySuccess(connection.createStatement().executeQuery(sql))
        } catch (e: Exception) {
            QueryError(e)
        }
    }
    fun execute(sql: String): Boolean{
        return try{
            connection.createStatement().execute(sql)
            return true
        }
        catch (e: Exception){
            Log.e("Error", e.message.toString())
            false
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