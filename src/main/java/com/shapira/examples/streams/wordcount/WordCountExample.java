package com.shapira.examples.streams.wordcount;

import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

public class WordCountExample {

    public static void main(String[] args) throws Exception{

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // setting offset reset to earliest so that we can re-run the demo code with the same pre-loaded data
        // Note: To re-run the demo, you need to use the offset reset tool:
        // https://cwiki.apache.org/confluence/display/KAFKA/Kafka+Streams+Application+Reset+Tool
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // work-around for an issue around timing of creating internal topics
        // Fixed in Kafka 0.10.2.0
        // don't use in large production apps - this increases network load
        // props.put(CommonClientConfigs.METADATA_MAX_AGE_CONFIG, 500);

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> source = builder.stream("wordcount-input");

        final Pattern pattern = Pattern.compile("\\W+");
        KStream<Object, String> counts = source.flatMapValues(value -> Arrays.asList(pattern.split(value.toLowerCase())))
                                               .map((key, value) -> new KeyValue<Object, Object>(value, value))
                                               .filter((key, value) -> (!value.equals("the")))
                                               .groupByKey()
                                               .count()
                                               .mapValues(value -> Long.toString(value)).toStream();
        counts.to("wordcount-output");

        try (KafkaStreams streams = new KafkaStreams(builder.build(), props)) {
            // This is for reset to work. Don't use in production - it causes the app to re-load the state from Kafka on every start
            streams.cleanUp();

            streams.start();

            // usually the stream application would be running forever,
            // in this example we just let it run for some time and stop since the input data is finite.
            Thread.sleep(5000L);
        }
    }
}
