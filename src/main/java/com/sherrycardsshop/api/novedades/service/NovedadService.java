package com.sherrycardsshop.api.novedades.service;

import java.util.List;

import com.sherrycardsshop.api.novedades.dto.NovedadDto;
import com.sherrycardsshop.api.novedades.mapper.NovedadMapper;
import com.sherrycardsshop.api.novedades.repository.NovedadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NovedadService {

    private final NovedadRepository novedadRepository;
    private final NovedadMapper novedadMapper;

    public NovedadService(NovedadRepository novedadRepository, NovedadMapper novedadMapper) {
        this.novedadRepository = novedadRepository;
        this.novedadMapper = novedadMapper;
    }

    @Transactional(readOnly = true)
    public List<NovedadDto> getActiveNovedades() {
        return novedadMapper.toDtoList(novedadRepository.findAllByActiveTrueOrderByDisplayOrderAsc());
    }
}