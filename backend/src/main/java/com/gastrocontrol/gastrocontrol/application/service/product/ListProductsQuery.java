// src/main/java/com/gastrocontrol/gastrocontrol/service/product/ListProductsQuery.java
package com.gastrocontrol.gastrocontrol.application.service.product;

public class ListProductsQuery {

    private final Boolean active;
    private final Long categoryId;
    private final String q;

    public ListProductsQuery(Boolean active, Long categoryId, String q) {
        this.active = active;
        this.categoryId = categoryId;
        this.q = q;
    }

    public Boolean getActive() { return active; }
    public Long getCategoryId() { return categoryId; }
    public String getQ() { return q; }
}
