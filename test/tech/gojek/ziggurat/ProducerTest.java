package tech.gojek.ziggurat;

import clojure.lang.Keyword;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.integration.utils.IntegrationTestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.gojek.ziggurat.test.Fixtures;

import java.util.List;
import java.util.Properties;

public class ProducerTest {

    @Before
    public void setup() {
        Fixtures.mountConfig();
        Fixtures.mountProducer();
    }

    @After
    public void teardown() {
        Fixtures.unmountAll();
    }

    @Test
    public void shouldSendStringData() throws InterruptedException {
        Producer.send(Keyword.intern("default"), "ziggurat-java-test", "String - Key", "String - Sent from Java");

        List<KeyValue<String,String>> result = IntegrationTestUtils.waitUntilMinKeyValueRecordsReceived(getStringConsumerConfig(), "ziggurat-java-test", 1, 2000);

        Assert.assertEquals(result.get(0).key, "String - Key");
        Assert.assertEquals(result.get(0).value, "String - Sent from Java");
    }

    private Properties getStringConsumerConfig() {
        Properties consumerProperties = new Properties();
        consumerProperties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProperties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProperties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "ziggurat-consumer");
        consumerProperties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProperties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        return consumerProperties;
    }
}
