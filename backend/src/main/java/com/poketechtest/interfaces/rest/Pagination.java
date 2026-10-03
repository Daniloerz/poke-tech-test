package com.poketechtest.interfaces.rest;

/** Pagination limits shared by the paginated endpoints. */
final class Pagination {

    static final String DEFAULT_PAGE = "0";
    static final String DEFAULT_SIZE = "20";
    static final int MAX_SIZE = 50;

    private Pagination() {
    }
}
