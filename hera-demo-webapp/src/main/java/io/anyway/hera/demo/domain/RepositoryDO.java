package io.anyway.hera.demo.domain;

import java.io.Serializable;

/**
 * Created by yangzz on 16/8/17.
 */
public class RepositoryDO implements Serializable {

    private long productId;

    private String category;

    private long amount;

    private long price;

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }
}
