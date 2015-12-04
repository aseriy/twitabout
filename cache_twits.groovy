@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')
@Grab(group='org.twitter4j', module='twitter4j-stream', version='4.0.4')
@Grab(group='org.apache.kafka', module='kafka_2.11', version='0.8.2.2')

import twitter4j.*
import twitter4j.json.*
import static common_db.*
import kafka.javaapi.producer.Producer
import kafka.producer.KeyedMessage
import kafka.producer.ProducerConfig
import kafka.producer.Partitioner
import kafka.utils.VerifiableProperties

// Kafka setup
Properties props = new Properties()
props.put("metadata.broker.list", "localhost:9092")
props.put("serializer.class", "kafka.serializer.StringEncoder")
props.put("request.required.acks", "1")

ProducerConfig config = new ProducerConfig(props)
Producer<String, String> producer = new Producer<String, String>(config)



StatusListener statusListener = new StatusListener() {
	void onStatus(Status status) {
		println status.getUser().getName() + " : " + status.getText()
		//Status To JSON String
		String statusJson = DataObjectFactory.getRawJSON(status)
		KeyedMessage<String, String> data = new KeyedMessage<String, String>("influencers", statusJson)
		producer.send(data)
	}

	void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {}

	void onTrackLimitationNotice(int numberOfLimitedStatuses) {
		println "onTrackLimitationNotice: " + ex
	}

	void onException(Exception ex) {
		println "onException: " + ex
	}

	void onScrubGeo(long userId, long upToStatusId) {}
	void onStallWarning(StallWarning warning) {}
}

// Twitter Stream
TwitterStream twitterStream = new TwitterStreamFactory().getInstance()
twitterStream.addListener(statusListener)

FilterQuery fq = new FilterQuery()
fq.follow(dbGetInfluencers() as Long[])

println twitterStream
println fq

twitterStream.filter(fq)
//twitterStream.cleanUp()
//twitterStream.shutdown()


