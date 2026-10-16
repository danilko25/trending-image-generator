package com.ai.generation.design;

import com.ai.generation.common.SearchRequest;
import com.ai.generation.design.dto.DesignCardDto;
import com.ai.generation.design.dto.DesignShortDetailsDto;
import com.ai.generation.design.dto.DesignFullDetailsDto;
import com.ai.generation.image.background.RemoveBackgroundService;
import com.ai.generation.image.upscaling.ImageUpscaleService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/design")
public class DesignController {

    private final DesignService designService;
    private final RemoveBackgroundService removeBackgroundService;
    private final ImageUpscaleService imageUpscaleService;

    @GetMapping("/card")
    @Operation(summary = "Get all design cards")
    public Page<DesignCardDto> getDesignCards(SearchRequest request) {
        return designService.getDesignCards(request);
    }

    @GetMapping("/{id}/full")
    @Operation(summary = "Get design details")
    public ResponseEntity<DesignFullDetailsDto> getDesignDetails(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(designService.getFullDetailsById(id));
    }

    @GetMapping("/{id}/short")
    @Operation(summary = "Get design card for quick copying")
    public ResponseEntity<DesignShortDetailsDto> getDesignCard(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(designService.getShortDetailsById(id));
    }

    @PutMapping("/{id}/remove-background")
    public ResponseEntity<DesignShortDetailsDto> removeBackground(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(removeBackgroundService.removeBackground(id));
    }
    @PutMapping("/{id}/upscale")
    public ResponseEntity<DesignShortDetailsDto> upscale(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(imageUpscaleService.upscaleOriginalImage(id));

    }
    @PutMapping("/{id}/upscale-removed-background")
    public ResponseEntity<DesignShortDetailsDto> upscaleRemovedBackground(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(imageUpscaleService.upscaleRemovedBackgroundImage(id));
    }

}
