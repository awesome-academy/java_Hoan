package com.fooddrinks.util;

/**
 * Centralized URL path constants for the REST API.
 *
 * All constants are compile-time String literals, so they are safe to use
 * inside annotation attributes (e.g. @RequestMapping, @PostMapping).
 *
 * Grouping by domain keeps related paths together and makes renaming
 * the /api prefix a one-line change.
 */
public final class ApiPaths {

    private ApiPaths() {
    }

    /** Root prefix shared by all API routes. */
    public static final String ROOT = "/api";

    private static final String API = ROOT;

    public static final class Auth {
        private Auth() {
        }

        public static final String URL = API + "/auth";
    }

    public static final class Users {
        private Users() {
        }

        public static final String URL = API + "/users";
    }

    public static final class Categories {
        private Categories() {
        }

        public static final String URL = API + "/categories";
    }

    public static final class Products {
        private Products() {
        }

        public static final String URL = API + "/products";
    }

    public static final class Orders {
        private Orders() {
        }

        public static final String URL = API + "/orders";
    }

    public static final class Cart {
        private Cart() {
        }

        public static final String URL = API + "/cart";
    }

    public static final class Suggestions {
        private Suggestions() {
        }

        public static final String URL = API + "/suggestions";
    }

    public static final class Summary {
        private Summary() {
        }

        public static final String URL = API + "/summary";
    }

    public static final class Ratings {
        private Ratings() {
        }

        public static final String URL = API + "/ratings";
    }
}
