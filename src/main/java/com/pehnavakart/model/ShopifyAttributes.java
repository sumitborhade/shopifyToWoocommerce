package com.pehnavakart.model;

public class ShopifyAttributes {
    private String title;
    private String bodyHTML;
    private String Type;
    private String tags;
    private String imageLocation;
    private String sku;
    private String size;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBodyHTML() {
        return bodyHTML;
    }

    public void setBodyHTML(String bodyHTML) {
        this.bodyHTML = bodyHTML;
    }

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getImageLocation() {
        return imageLocation;
    }

    public void setImageLocation(String imageLocation) {
        this.imageLocation = imageLocation;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "ShopifyAttributes{" +
                "title='" + title + '\'' +
                ", bodyHTML='" + bodyHTML + '\'' +
                ", Type='" + Type + '\'' +
                ", tags='" + tags + '\'' +
                ", imageLocation='" + imageLocation + '\'' +
                ", sku='" + sku + '\'' +
                ", size='" + size + '\'' +
                '}';
    }
}