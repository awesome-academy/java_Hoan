package com.fooddrinks.service.impl;

import com.fooddrinks.dto.request.AddCartItemRequest;
import com.fooddrinks.dto.request.UpdateCartItemRequest;
import com.fooddrinks.dto.response.CartItemResponse;
import com.fooddrinks.dto.response.CartResponse;
import com.fooddrinks.entity.Cart;
import com.fooddrinks.entity.CartItem;
import com.fooddrinks.entity.Product;
import com.fooddrinks.entity.User;
import com.fooddrinks.exception.BadRequestException;
import com.fooddrinks.exception.ResourceNotFoundException;
import com.fooddrinks.repository.CartItemRepository;
import com.fooddrinks.repository.CartRepository;
import com.fooddrinks.repository.ProductRepository;
import com.fooddrinks.repository.UserRepository;
import com.fooddrinks.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CartResponse getCart(String email) {
        User user = findUserOrThrow(email);
        Cart cart = getOrCreateCart(user);
        return toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(String email, AddCartItemRequest request) {
        User user = findUserOrThrow(email);
        Cart cart = getOrCreateCart(user);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));
        if (!product.getIsActive()) {
            throw new BadRequestException("Product is not available: " + product.getName());
        }

        // If item already exists in cart, increment quantity
        Optional<CartItem> existing = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId());

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(request.getQuantity());
            cart.getItems().add(item);
        }

        return toCartResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse updateItem(String email, Long productId, UpdateCartItemRequest request) {
        User user = findUserOrThrow(email);
        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product " + productId + " is not in your cart"));

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        // cart.getItems() already holds the updated item (same entity in PC)
        return toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(String email, Long productId) {
        User user = findUserOrThrow(email);
        Cart cart = getOrCreateCart(user);

        // Verifies ownership: CartItem must belong to this user's cart
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product " + productId + " is not in your cart"));

        cart.getItems().remove(item);
        return toCartResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse clearCart(String email) {
        User user = findUserOrThrow(email);
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        return toCartResponse(cartRepository.save(cart));
    }

    // --- helpers ---

    /**
     * Returns the existing cart for the user, or creates a new one.
     * Uses findWithItemsByUserId to eagerly load items and their products
     * (prevents N+1 in toCartResponse).
     */
    private Cart getOrCreateCart(User user) {
        return cartRepository.findWithItemsByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }

    private User findUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    /**
     * Converts a Cart entity to a CartResponse.
     * Assumes cart.items and items.product are already loaded (via EntityGraph).
     * product.images are lazy but batched via @BatchSize(20) on Product.
     */
    private CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::toCartItemResponse)
                .toList();

        BigDecimal totalAmount = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                .build();
    }

    private CartItemResponse toCartItemResponse(CartItem item) {
        Product product = item.getProduct();

        // Primary image URL — loaded lazily, batched by @BatchSize(20) on Product.images
        String imageUrl = product.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(img -> img.getImageUrl())
                .findFirst()
                .orElse(null);

        BigDecimal subtotal = product.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productPrice(product.getPrice())
                .productImageUrl(imageUrl)
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .addedAt(item.getAddedAt())
                .build();
    }
}
