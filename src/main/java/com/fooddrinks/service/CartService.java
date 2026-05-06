package com.fooddrinks.service;

import com.fooddrinks.dto.request.AddCartItemRequest;
import com.fooddrinks.dto.request.UpdateCartItemRequest;
import com.fooddrinks.dto.response.CartResponse;

public interface CartService {

    /** Get (or auto-create) the current user's cart. */
    CartResponse getCart(String email);

    /**
     * Add a product to the cart. If the product already exists,
     * its quantity is incremented by the requested amount.
     */
    CartResponse addItem(String email, AddCartItemRequest request);

    /** Set the quantity of a specific cart item. Quantity must be >= 1. */
    CartResponse updateItem(String email, Long productId, UpdateCartItemRequest request);

    /** Remove a specific product from the cart. */
    CartResponse removeItem(String email, Long productId);

    /** Remove all items from the cart. */
    CartResponse clearCart(String email);
}
