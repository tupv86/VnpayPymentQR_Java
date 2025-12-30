package com.tupv.PaymentQR.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tupv.PaymentQR.model.Order;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final Path filePath = Path.of("Data", "orders.json");
    private final ObjectMapper mapper;

    public OrderService() {
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        initFile();
    }

    private void initFile() {
        try {
            Path dir = filePath.getParent();
            if (dir != null) Files.createDirectories(dir);

            if (!Files.exists(filePath)) {
                Files.writeString(filePath, "[]", StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize orders.json", e);
        }
    }

    public List<Order> getAllOrders() {
        try {
            String json = Files.readString(filePath, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) return new ArrayList<>();

            return mapper.readValue(json, new TypeReference<List<Order>>() {});
        } catch (Exception e) {
            // Nếu file bị hỏng/không parse được, bạn có thể chọn: throw hoặc return []
            throw new RuntimeException("Cannot read orders.json", e);
        }
    }

    public void saveOrders(List<Order> orders) {
        try {
            String json = mapper.writeValueAsString(orders);
            Files.writeString(filePath, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException("Cannot save orders.json", e);
        }
    }

    public void addOrder(Order order) {
        List<Order> orders = getAllOrders();
        orders.add(order);
        saveOrders(orders);
    }

    public Optional<Order> getOrderById(String orderId) {
        return getAllOrders().stream()
                .filter(o -> orderId != null && orderId.equals(o.getOrderId()))
                .findFirst();
    }

    public void updateOrder(Order updatedOrder) {
        List<Order> orders = getAllOrders();

        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i).getOrderId().equals(updatedOrder.getOrderId())) {
                orders.set(i, updatedOrder);
                saveOrders(orders);
                return;
            }
        }
        // Không tìm thấy thì bỏ qua (giống C# index == -1)
    }
}

