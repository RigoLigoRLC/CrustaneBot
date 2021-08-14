package org.tech4c57.bot

import com.mongodb.*
import com.mongodb.client.model.Filters
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.reactivestreams.KMongo

data class TestData(
    val name: String,
    val id: Long,
    val u: List<Long>,
)

internal class ConnectDatabase {

    @Test
    fun tryConnect() {
        runBlocking {
            try {
                val b = Foundation.readConfig("/storage/projects/kotlin/0002_bot/botconf")
//                val jdbcClient = MongoClient(
//                    ServerAddress(b["dbhost"] ?: "localhost", 27017),
//                    MongoCredential.createCredential(b["dbuser"]!!, "admin", b["dbpwd"]!!.toCharArray()),
//                    MongoClientOptions.builder().build()
//                )

                val client = CoroutineClient(
                    KMongo.createClient(
                        ConnectionString("mongodb://${b["dbuser"]!!}:${b["dbpwd"]}@${b["dbhost"] ?: "localhost"}:27017")
                    )
                )

                val permdb = client.getDatabase("crustane_perm")
                val datadb = client.getDatabase("crustane_data")

                val dataIt = permdb.getCollection<TestData>("test_data")
                    .find(TestData::name eq "_crustane_").toList().iterator()
                assert(dataIt.hasNext())
                val data = dataIt.next()
                assertEquals(data.id, 1776526885L)
                assertEquals(data.u[0], 1397755924L)


                //
                //            val dummy = datadb.getCollection("dummy__").find(Filters.eq("__dummy__", 1)).cursor()
                //            assert(dummy.hasNext())
                //            val dummydata = dummy.next()["data"]
                //            println("Dummy data is \"${dummydata}\", expects 12345678")
                //            assertEquals(dummydata, 12345678)

            } catch (e: Throwable) {
                fail(e)
            }
        }
        assert(true)
    }
}