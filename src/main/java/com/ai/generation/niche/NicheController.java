package com.ai.generation.niche;

import com.ai.generation.niche.dto.NicheDetailsDto;
import com.ai.generation.niche.dto.SearchNicheRequest;
import com.ai.generation.niche.dto.SearchNicheResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/niche")
@RequiredArgsConstructor
public class NicheController {

    private final NicheService nicheService;

    @GetMapping("/{id}/details")
    public ResponseEntity<NicheDetailsDto> getDetailsById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(nicheService.getDetailsById(id));
    }

    @GetMapping("/search")
    public Page<SearchNicheResponse> searchNiche(SearchNicheRequest request) {
        return nicheService.searchNiches(request);
    }

}
