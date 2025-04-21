package ma.fin.monitor.collections.csv;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class ComplaintDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(ComplaintDataLoader.class);
    private static final Gson gson = new Gson();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    
    private final KafkaProducer<String, String> producer;
    private final String kafkaTopic;
    
    public ComplaintDataLoader(String kafkaTopic) {
        this.kafkaTopic = kafkaTopic;
        this.producer = createKafkaProducer();
    }
    
    public void loadComplaintsFromCSV(String csvFilePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(csvFilePath));
        String header = reader.readLine(); // 读取表头
        String[] headerColumns = parseCSVLine(header);
        String line;
        
        while ((line = reader.readLine()) != null) {
            try {
                Map<String, String> complaint = parseComplaint(line, headerColumns);
                String complaintId = complaint.get("complaint_id");
                producer.send(new ProducerRecord<>(kafkaTopic, 
                        complaintId, gson.toJson(complaint)));
            } catch (Exception e) {
                logger.warn("无法解析投诉记录: " + line, e);
            }
        }
        
        reader.close();
        producer.flush();
    }
    
    
    /**
     * 解析CSV行，处理引号内的逗号
     */
    private String[] parseCSVLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentToken = new StringBuilder();
        
        for (char c : line.toCharArray()) {
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(currentToken.toString().trim());
                currentToken = new StringBuilder();
            } else {
                currentToken.append(c);
            }
        }
        tokens.add(currentToken.toString().trim());
        
        return tokens.toArray(new String[0]);
    }
    
    /**
     * 将CSV行转换为投诉记录
     */
    private Map<String, String> parseComplaint(String line, String[] headers) {
        String[] values = parseCSVLine(line);
        Map<String, String> complaint = new HashMap<>();
        
        for (int i = 0; i < Math.min(headers.length, values.length); i++) {
            complaint.put(headers[i], values[i]);
        }
        
        // 标准化公司名称
        if (complaint.containsKey("company")) {
            complaint.put("company", normalizeCompanyName(complaint.get("company")));
        }
        
        return complaint;
    }
    
    /**
     * 标准化公司名称
     */
    private String normalizeCompanyName(String name) {
        if (name == null) return "";
        
        return name.trim()
                .toUpperCase()
                .replaceAll("\\s+", " ")
                .replaceAll("\\bLTD\\b", "LIMITED")
                .replaceAll("\\bCORP\\b", "CORPORATION")
                .replaceAll("\\bINC\\b", "INCORPORATED");
    }
    
    /**
     * 创建Kafka生产者
     */
    private KafkaProducer<String, String> createKafkaProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("acks", "1");
        return new KafkaProducer<>(props);
    }
    
    /**
     * 关闭资源
     */
    public void close() {
        if (producer != null) {
            producer.close();
        }
    }
}