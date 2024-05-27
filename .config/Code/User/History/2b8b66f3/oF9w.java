package kafka.oxxo.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class KafkaProducerExample {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "172.24.51.224:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            for (int i = 0; i < 20; i++) {
                producer.send(new ProducerRecord<String, String>("DataTransporterGeneric", Integer.toString(i), "Mensaje " + i));
                System.out.println("Mensaje enviado: Mensaje " + i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
