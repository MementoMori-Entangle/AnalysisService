package com.entangle.analysis.service;

import com.entangle.analysis.entity.ServiceInfo;
import com.entangle.analysis.repository.ServiceInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ServiceInfoService {
    @Autowired
    private ServiceInfoRepository repository;

    public List<ServiceInfo> findAll() {
        return repository.findAll();
    }
    public Optional<ServiceInfo> findById(Long id) {
        return repository.findById(id);
    }
    public ServiceInfo save(ServiceInfo info) {
        return repository.save(info);
    }
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
