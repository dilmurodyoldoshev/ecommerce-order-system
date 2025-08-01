package uz.app.strong_junior_test_task.service;

import uz.app.strong_junior_test_task.dto.*;
import uz.app.strong_junior_test_task.entity.*;
import uz.app.strong_junior_test_task.enums.OrderStatus;
import uz.app.strong_junior_test_task.exception.*;
import uz.app.strong_junior_test_task.repository.OrderRepository;
import uz.app.strong_junior_test_task.repository.ProductRepository;
import uz.app.strong_junior_test_task.util.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Email validatsiya
        if (!request.getCustomerEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Email format noto‘g‘ri!");
        }

        // Duplicate mahsulotlar borligini tekshirish
        Set<Long> uniqueProductIds = new HashSet<>();
        for (OrderItemRequest item : request.getItems()) {
            if (!uniqueProductIds.add(item.getProductId())) {
                throw new IllegalArgumentException("Bir buyurtmada bir xil mahsulot ikki marta bo‘lmasligi kerak.");
            }
        }

        List<OrderItem> orderItems = new ArrayList<>();
        double totalAmount = 0.0;

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException("Mahsulot topilmadi: " + itemRequest.getProductId()));

            if (!Boolean.TRUE.equals(product.getIsActive()) || product.getStock() <= 0) {
                throw new IllegalStateException("Mahsulot mavjud emas yoki aktiv emas: " + product.getName());
            }

            if (product.getStock() < itemRequest.getQuantity()) {
                throw new InsufficientStockException("Yetarli mahsulot yo‘q: " + product.getName());
            }

            // Stock kamaytirish
            product.setStock(product.getStock() - itemRequest.getQuantity());

            // Agar stock 0 bo‘lsa, isActive ni false qilish
            if (product.getStock() <= 0) {
                product.setIsActive(false);
            }

            productRepository.save(product); // saqlash

            double itemTotal = product.getPrice() * itemRequest.getQuantity();

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .totalPrice(itemTotal)
                    .build();

            orderItems.add(orderItem);
            totalAmount += itemTotal;
        }

        // Order yaratish
        Order order = Order.builder()
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .orderItems(new ArrayList<>()) // vaqtinchalik
                .build();

        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        order.setOrderItems(orderItems);

        Order saved = orderRepository.save(order);
        return orderMapper.toResponse(saved);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        return orderMapper.toResponse(order);
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public List<OrderResponse> getOrdersByCustomerEmail(String email) {
        return orderRepository.findByCustomerEmail(email).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public OrderResponse changeOrderStatus(Long id, String statusStr) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus;

        try {
            newStatus = OrderStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOrderStatusException("Noto‘g‘ri status nomi: " + statusStr);
        }

        if (!currentStatus.allowedNextStatuses().contains(newStatus)) {
            throw new InvalidOrderStatusException("Statusni " + currentStatus + " dan " + newStatus + " ga o‘tkazib bo‘lmaydi");
        }

        order.setStatus(newStatus);

        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            int currentStock = product.getStock();
            int quantity = item.getQuantity();

            // ✅ CONFIRMED → stock kamayadi
            if (newStatus == OrderStatus.CONFIRMED) {
                int updatedStock = currentStock - quantity;
                product.setStock(updatedStock);
                if (updatedStock <= 0) {
                    product.setIsActive(false);
                }
            }

            // ✅ CANCELLED → stock ortadi va isActive true bo‘ladi
            if (newStatus == OrderStatus.CANCELLED && currentStatus != OrderStatus.CANCELLED) {
                int updatedStock = currentStock + quantity;
                product.setStock(updatedStock);
                product.setIsActive(true);
            }

            productRepository.save(product);
        }

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    public void cancelOrder(Long id) {
        changeOrderStatus(id, "CANCELLED");
    }
}
