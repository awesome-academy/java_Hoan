package com.fooddrinks.util;

/**
 * Centralized URL path and Thymeleaf view name constants for the Admin UI.
 *
 * URL constants — used for redirect strings and formAction model attributes.
 * View constants — Thymeleaf template paths returned from controller methods.
 *
 * Grouping by domain (nested static classes) keeps related constants together
 * and makes refactoring the /admin prefix a one-line change.
 */
public final class AdminPaths {

    private AdminPaths() {
    }

    /**
     * Root prefix shared by all admin routes. Change this to relocate the entire
     * admin area.
     */
    public static final String ROOT = "/admin";

    private static final String ADMIN = ROOT;

    public static final class Dashboard {
        private Dashboard() {
        }

        public static final String URL = ADMIN + "/dashboard";
        public static final String VIEW = "admin/dashboard";
    }

    public static final class Login {
        private Login() {
        }

        public static final String URL = ADMIN + "/login";
        public static final String LOGOUT_URL = ADMIN + "/logout";
        public static final String VIEW = "admin/login";
    }

    public static final class Users {
        private Users() {
        }

        public static final String URL = ADMIN + "/users";
        public static final String VIEW_LIST = "admin/users/list";
        public static final String VIEW_DETAIL = "admin/users/detail";
    }

    public static final class Categories {
        private Categories() {
        }

        public static final String URL = ADMIN + "/categories";
        public static final String VIEW_LIST = "admin/categories/list";
        public static final String VIEW_FORM = "admin/categories/form";
    }

    public static final class Products {
        private Products() {
        }

        public static final String URL = ADMIN + "/products";
        public static final String VIEW_LIST = "admin/products/list";
        public static final String VIEW_FORM = "admin/products/form";
    }

    public static final class Orders {
        private Orders() {
        }

        public static final String URL = ADMIN + "/orders";
        public static final String VIEW_LIST = "admin/orders/list";
        public static final String VIEW_DETAIL = "admin/orders/detail";
    }

    public static final class Suggestions {
        private Suggestions() {
        }

        public static final String URL = ADMIN + "/suggestions";
        public static final String VIEW_LIST = "admin/suggestions/list";
        public static final String VIEW_DETAIL = "admin/suggestions/detail";
    }
}
