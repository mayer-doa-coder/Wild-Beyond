package com.wildbeyond.service;

import com.wildbeyond.dto.OrderDTO;
import com.wildbeyond.dto.OrderItemDTO;
import com.wildbeyond.exception.ResourceNotFoundException;
import com.wildbeyond.model.*;
import com.wildbeyond.repository.OrderRepository;
import com.wildbeyond.repository.ProductRepository;
import com.wildbeyond.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Business logic for Order CRUD operations.
 *
 * Security design:
 *   - The buyer is always resolved from the authenticated principal —
 *     never accepted from client-provided input.
 *   - GET /api/orders returns all orders for ADMIN;
 *     buyers see only their own via /api/orders/my (future scope).
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Resolve the currently authenticated user from the security context.
     * Spring Security stores the email (used as username) in the principal.
     *
     * @throws ResourceNotFoundException if the authenticated email has no DB record
     */
    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();   // email is used as the Spring Security "username"
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user not found: " + email));
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(role -> "ADMIN".equals(role.getName()));
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> roleName.equals(role.getName()));
    }

    private void assertBuyerOrSeller(User user) {
        if (!(hasRole(user, "BUYER") || hasRole(user, "SELLER"))) {
            throw new AccessDeniedException("Only buyers and sellers can place orders");
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /**
     * Place a new order for the currently authenticated buyer.
     *
     * For each item in the DTO:
     *   1. Looks up the Product — throws 404 if missing.
     *   2. Creates an OrderItem with unitPrice snapshotted from the product's current price.
     *   3. Calculates totalPrice as the sum of (quantity × unitPrice) across all items.
     *
     * Order is persisted with status PENDING.
     *
     * @param dto list of items (productId + quantity); buyerId is NOT accepted from client
     * @throws ResourceNotFoundException if any productId in the order does not exist
     */
    @Transactional
    public Order create(OrderDTO dto) {
        User buyer = currentUser();
        assertBuyerOrSeller(buyer);

        Map<Long, Integer> requestedItems = new LinkedHashMap<>();
        for (OrderItemDTO itemDto : dto.getItems()) {
            Integer existing = requestedItems.getOrDefault(itemDto.getProductId(), 0);
            requestedItems.put(itemDto.getProductId(), existing + itemDto.getQuantity());
        }

        return createFromRequestedItems(buyer, requestedItems);
    }

    @Transactional
    public Order createFromCart(Map<Long, Integer> productQuantities) {
        User buyer = currentUser();
        assertBuyerOrSeller(buyer);
        return createFromRequestedItems(buyer, productQuantities);
    }

    private Order createFromRequestedItems(User buyer, Map<Long, Integer> productQuantities) {
        if (productQuantities == null || productQuantities.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        Order order = new Order();
        order.setBuyer(buyer);
        order.setStatus(OrderStatus.PENDING);

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be at least 1");
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + productId));

            if (hasRole(buyer, "SELLER") && product.getSeller().getId().equals(buyer.getId())) {
                throw new AccessDeniedException("Sellers cannot buy their own products");
            }

            if (product.getStock() == null || product.getStock() < quantity) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
            }

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(quantity);
            // Snapshot the price at the time of ordering — preserves order history
            // even if the product's price changes later.
            item.setUnitPrice(product.getPrice());

            items.add(item);
            total = total.add(product.getPrice()
                    .multiply(BigDecimal.valueOf(quantity)));

                product.setStock(product.getStock() - quantity);
        }

        order.setItems(items);
        order.setTotalPrice(total);

        return orderRepository.save(order);
    }

    /**
     * Return all orders (ADMIN use).
     */
    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    /**
     * Return orders placed by the currently authenticated buyer.
     */
    @Transactional(readOnly = true)
    public List<Order> findMyOrders() {
        return orderRepository.findByBuyerId(currentUser().getId());
    }

    @Transactional(readOnly = true)
    public List<Order> findOrdersForCurrentSellerProducts() {
        User seller = currentUser();
        if (!hasRole(seller, "SELLER")) {
            throw new AccessDeniedException("Only sellers can access product order history");
        }
        return orderRepository.findOrdersContainingSellerProducts(seller.getId());
    }

    @Transactional(readOnly = true)
    public long countOrders() {
        return orderRepository.count();
    }

    @Transactional(readOnly = true)
    public long countOrdersByCurrentUser() {
        User user = currentUser();
        return orderRepository.countByBuyerId(user.getId());
    }

    @Transactional(readOnly = true)
    public long countBuyingOrdersForCurrentSeller() {
        User seller = currentUser();
        if (!hasRole(seller, "SELLER")) {
            throw new AccessDeniedException("Only sellers can access buying order count");
        }
        return orderRepository.countByBuyerId(seller.getId());
    }

    @Transactional(readOnly = true)
    public long countOrdersForSeller() {
        User seller = currentUser();
        return orderRepository.countOrdersContainingSellerProducts(seller.getId());
    }

    /**
     * Return an ownership-aware order detail DTO for MVC view rendering.
     * Buyers can only view their own orders; ADMIN can view any order.
     */
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        User user = currentUser();
        Order order = findById(id);

        if (!isAdmin(user) && !order.getBuyer().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not allowed to view this order");
        }

        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderDate(order.getOrderDate());
        dto.setStatus(order.getStatus().name());
        dto.setTotalPrice(order.getTotalPrice());

        List<OrderItemDTO> itemDtos = order.getItems().stream().map(item -> {
            OrderItemDTO itemDto = new OrderItemDTO();
            itemDto.setProductId(item.getProduct().getId());
            itemDto.setProductName(item.getProduct().getName());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitPrice(item.getUnitPrice());
            return itemDto;
        }).toList();

        dto.setItems(itemDtos);
        return dto;
    }

    /**
     * Return a single order by id.
     *
     * @throws ResourceNotFoundException if no Order exists with the given id
     */
    @Transactional(readOnly = true)
    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + id));
    }

    /**
     * Update order status with role-aware transition rules.
     *
     * ADMIN: can set any status.
     * BUYER (owner only): can cancel when the order is still active.
     */
    @Transactional
    public Order updateStatus(Long id, OrderStatus nextStatus) {
        User user = currentUser();
        Order order = findById(id);

        List<OrderStatus> allowedStatuses = computeAllowedStatuses(user, order);
        if (!allowedStatuses.contains(nextStatus)) {
            throw new AccessDeniedException("You are not allowed to set this order status");
        }

        order.setStatus(nextStatus);
        return orderRepository.save(order);
    }

    @Transactional
    public Order updateStatus(Long id, String statusValue) {
        OrderStatus nextStatus;
        try {
            nextStatus = OrderStatus.valueOf(statusValue.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid order status: " + statusValue);
        }
        return updateStatus(id, nextStatus);
    }

    @Transactional(readOnly = true)
    public List<OrderStatus> getAllowedStatusUpdates(Long id) {
        User user = currentUser();
        Order order = findById(id);
        return computeAllowedStatuses(user, order);
    }

    private List<OrderStatus> computeAllowedStatuses(User user, Order order) {
        if (isAdmin(user)) {
            return Arrays.asList(OrderStatus.values());
        }

        boolean isOwner = order.getBuyer().getId().equals(user.getId());
        if (!isOwner) {
            return List.of();
        }

        EnumSet<OrderStatus> cancellable = EnumSet.of(
                OrderStatus.PENDING,
                OrderStatus.CONFIRMED,
                OrderStatus.PROCESSING
        );

        if (cancellable.contains(order.getStatus())) {
            return List.of(OrderStatus.CANCELLED);
        }

        return List.of();
    }

    /**
     * Cancel (delete) an order by id.
     *
     * @throws ResourceNotFoundException if no Order exists with the given id,
     *         so the controller returns 404 instead of a silent 204.
     */
    @Transactional
    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order not found with id: " + id);
        }
        orderRepository.deleteById(id);
    }
}
