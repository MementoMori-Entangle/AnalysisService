package com.entangle.analysis.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "service_info")
public class ServiceInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String analysisType;

    @Column(nullable = false)
    private String analysisName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String dataProcessInfoJson;

    // getter/setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAnalysisType() { return analysisType; }
    public void setAnalysisType(String analysisType) { this.analysisType = analysisType; }
    public String getAnalysisName() { return analysisName; }
    public void setAnalysisName(String analysisName) { this.analysisName = analysisName; }
    public String getDataProcessInfoJson() { return dataProcessInfoJson; }
    public void setDataProcessInfoJson(String dataProcessInfoJson) { this.dataProcessInfoJson = dataProcessInfoJson; }
}
