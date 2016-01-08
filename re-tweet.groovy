@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')
@Grab(group='org.twitter4j', module='twitter4j-stream', version='4.0.4')
@Grab(group='org.apache.kafka', module='kafka-clients', version='0.9.+')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.7.13')

import org.apache.kafka.clients.consumer.*

Properties props = new Properties()
props.put("bootstrap.servers", "localhost:9092")
props.put("group.id", "test")
props.put("enable.auto.commit", "true")
props.put("auto.commit.interval.ms", "1000")
props.put("session.timeout.ms", "30000")
props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props)
consumer.subscribe(Arrays.asList("influencers"))
while (true) {
	ConsumerRecords<String, String> records = consumer.poll(100)
	for (ConsumerRecord<String, String> record : records)
		System.out.printf("offset = %d, key = %s, value = %s", record.offset(), record.key(), record.value())
}

 

