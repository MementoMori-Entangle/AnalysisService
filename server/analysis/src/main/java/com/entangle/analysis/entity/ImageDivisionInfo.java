package com.entangle.analysis.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "image_division_info")
public class ImageDivisionInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String uid;

    @Column(nullable = false)
    private int divisionNum;

    @Column(name = "embed_meta_text", columnDefinition = "LONGTEXT")
    private String embedMetaText;

    @Column(name = "embed_meta_base64", columnDefinition = "LONGTEXT")
    private String embedMetaBase64;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // getter/setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public int getDivisionNum() { return divisionNum; }
    public void setDivisionNum(int divisionNum) { this.divisionNum = divisionNum; }
    public String getEmbedMetaText() { return embedMetaText; }
    public void setEmbedMetaText(String embedMetaText) { this.embedMetaText = embedMetaText; }
    public String getEmbedMetaBase64() { return embedMetaBase64; }
    public void setEmbedMetaBase64(String embedMetaBase64) { this.embedMetaBase64 = embedMetaBase64; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
