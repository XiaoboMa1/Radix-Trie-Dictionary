package ma.fin.monitor.collections.csv;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
// 从UK制裁名单CSV加载数据
public class SanctionsDataLoader {
  private static final Logger log = LoggerFactory.getLogger(SanctionsDataLoader.class);
  private static final Gson gson = new Gson();
  private final KafkaProducer<String, String> producer;
  
  public SanctionsDataLoader() {
    this.producer = getKafkaProducer();
  }
  
  public void loadSanctionsData(String filePath, String outputTopic) {
    try {
      // 读取CSV文件，处理不同格式
      BufferedReader reader = new BufferedReader(new FileReader(filePath));
      String line;
      String header = reader.readLine(); // 读取表头
      
      while ((line = reader.readLine()) != null) {
        Map<String, String> entityData = parseCSVLine(line, header);
        SanctionedEntity entity = convertToEntity(entityData);
        
        // 发送到Kafka
        producer.send(new ProducerRecord<>(outputTopic, 
                  entity.getId(), gson.toJson(entity)));
      }
      
      reader.close();
      producer.flush();
    } catch (Exception e) {
      log.error("Error loading sanctions data", e);
    }
  }
  
  private KafkaProducer<String, String> getKafkaProducer() {
    Properties props = new Properties();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    props.put("acks", "1");
    return new KafkaProducer<>(props);
  }
  
  private Map<String, String> parseCSVLine(String line, String header) {
    // Similar implementation to ComplaintDataLoader
    String[] headers = header.split(",");
    String[] values = line.split(",");
    Map<String, String> data = new HashMap<>();
    
    for (int i = 0; i < Math.min(headers.length, values.length); i++) {
      data.put(headers[i], values[i]);
    }
    
    return data;
  }
  
  private SanctionedEntity convertToEntity(Map<String, String> data) {
    return new SanctionedEntity(data);
  }
  
  // Inner class to represent a sanctioned entity
  public static class SanctionedEntity {
    private String id;
    private Map<String, String> data;
    
    public SanctionedEntity(Map<String, String> data) {
      this.data = data;
      this.id = data.getOrDefault("id", UUID.randomUUID().toString());
    }
    
    public String getId() {
      return id;
    }
  }
  
  public void close() {
    if (producer != null) {
      producer.close();
    }
  }
}