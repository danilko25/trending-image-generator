package com.ai.generation.niche;

import com.ai.generation.exception.EntityNotFoundException;
import com.ai.generation.niche.dto.NicheDetailsDto;
import com.ai.generation.niche.dto.SearchNicheRequest;
import com.ai.generation.niche.dto.SearchNicheResponse;
import com.ai.generation.niche.dto.projection.NicheDetailsProjection;
import com.ai.generation.niche.repository.NicheRepository;
import com.ai.generation.niche.repository.NicheSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NicheService {

    private final NicheRepository nicheRepository;

    public Niche getById(UUID id) {
        return nicheRepository.findById(id)
                .orElseThrow(()-> new EntityNotFoundException(Niche.class, id));
    }

    public NicheDetailsDto getDetailsById(UUID id) {
        return new NicheDetailsDto(nicheRepository.findById(id, NicheDetailsProjection.class)
                .orElseThrow(()-> new EntityNotFoundException(Niche.class, id)));
    }

    public Page<SearchNicheResponse> searchNiches(SearchNicheRequest request) {

        Pageable pageable = request.getPageable(SearchNicheResponse.getSortingParam(request.getSortBy()).getName());

        var specifications = new ArrayList<Specification<Niche>>();
        if (request.getKeyword() != null) {
            specifications.add(NicheSpecifications.byKeyword(request.getKeyword()));
        }
        if (request.getCreatedAfter() != null) {
            specifications.add(NicheSpecifications.createdAfter(request.getCreatedAfter()));
        }
        if (request.getCreatedBefore() != null) {
            specifications.add(NicheSpecifications.createdBefore(request.getCreatedBefore()));
        }
        if (!specifications.isEmpty()) {
            return nicheRepository.findAll(Specification.allOf(specifications), pageable).map(SearchNicheResponse::new);
        } else {
            return nicheRepository.findAll(pageable).map(SearchNicheResponse::new);
        }
    }
}
