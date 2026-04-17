package com.da.web.model;

import java.util.List;

/**
 * 模型信息
 */
public class ModelInfo {
    private String id;
    private String object = "model";
    private Long created;
    private String ownedBy;

    public ModelInfo() {}

    public ModelInfo(String id, String ownedBy) {
        this.id = id;
        this.ownedBy = ownedBy;
        this.created = System.currentTimeMillis() / 1000;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getObject() { return object; }
    public void setObject(String object) { this.object = object; }
    
    public Long getCreated() { return created; }
    public void setCreated(Long created) { this.created = created; }
    
    public String getOwnedBy() { return ownedBy; }
    public void setOwnedBy(String ownedBy) { this.ownedBy = ownedBy; }
}
