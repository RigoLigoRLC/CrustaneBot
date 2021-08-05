package org.tech4c57.bot

import com.mongodb.*
import com.mongodb.client.model.Filters
import org.bson.conversions.Bson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

internal class ConnectDatabase {

    @Test
    fun tryConnect() {
        try {
            val b = Foundation.readConfig("/storage/projects/kotlin/0002_bot/botconf")
            val client = MongoClient(
                ServerAddress(b["dbhost"] ?: "localhost", 27017),
                MongoCredential.createCredential(b["dbuser"]!!, "admin", b["dbpwd"]!!.toCharArray()),
                MongoClientOptions.builder().build()
            )

            val permdb = client.getDatabase("crustane_perm")
            val datadb = client.getDatabase("crustane_data")

            val ping = permdb.getCollection("global_ctrl").find(Filters.eq("name", "ping")).cursor()
            assert(ping.hasNext())
            assertEquals(ping.next()["allow"], true)

            val dummy = datadb.getCollection("dummy__").find(Filters.eq("__dummy__", 1)).cursor()
            assert(dummy.hasNext())
            val dummydata = dummy.next()["data"]
            println("Dummy data is \"${dummydata}\", expects 12345678")
            assertEquals(dummydata, 12345678)

        } catch (e: Throwable) {
            fail(e)
        }
        assert(true)
    }
}