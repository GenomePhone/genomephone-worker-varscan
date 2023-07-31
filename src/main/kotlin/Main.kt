import com.edgedb.driver.EdgeDBClient
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.io.File
import java.time.Duration
import java.util.*

// minimal wrapper around varscan that pulls from the edgeDB database
fun main() {
    // Get the reference genome from EdgeDB
    val client = EdgeDBClient()

    val props = Properties()
    props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = "kafka:9092"
    props[ConsumerConfig.GROUP_ID_CONFIG] = "test"
    props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"

    val consumer = KafkaConsumer<String, String>(props)

    consumer.subscribe(listOf(System.getenv("PROJECT_ID")))

    while (true) {
        val records = consumer.poll(Duration.ofMillis(100))
        for (record in records) {
            println("Received message: ${record.value()} at offset ${record.offset()}")

            // Get the target chunk from EdgeDB
            val targetChunkId = UUID.fromString(record.value())
            val targetChunkData = getChunkById(client, targetChunkId)
            val targetChunkFile = File("$targetChunkId.pileup")
            targetChunkFile.writeText(targetChunkData!!)


            // Run varscan
            // val outputDir = File.createTempFile("varscan", "").parentFile
            val command = "java -jar VarScan.jar mpileup2snp ${targetChunkFile.absolutePath}"// > ${outputDir.absolutePath}/output.vcf"
            val process = Runtime.getRuntime().exec(command)
            process.waitFor()

            // Write the result back to EdgeDB
            val output = process.inputStream.bufferedReader().readText()
            writeResultToChunk(client, targetChunkId, output)
            consumer.close()
            return  // Only need to process one message
        }
    }
}

fun getChunkById(client: EdgeDBClient, id: UUID): String? {
    return client.querySingle(
        String::class.java,
        "SELECT Chunk { data } FILTER .id = <uuid>$id"
    ).toCompletableFuture().get()
}

fun writeResultToChunk(client: EdgeDBClient, id: UUID, result: String) {
    client.querySingle(
        UUID::class.java,
        "UPDATE Chunk { result := <str>$result } FILTER .id = <uuid>$id"
    ).toCompletableFuture().get()
}