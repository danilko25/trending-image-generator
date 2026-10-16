package com.ai.generation.imagestyle;

import com.ai.generation.imagestyle.dto.ImageStyleCreateUpdateDto;
import com.ai.generation.imagestyle.dto.ImageStyleDetailsDto;
import com.ai.generation.imagestyle.dto.ImageStyleDropDownItemDto;
import com.ai.generation.imagestyle.dto.ImageStyleResponse;
import com.ai.generation.imagestyle.dto.ImageStyleSearchRequest;
import com.ai.generation.imagestyle.dto.ImageStyleTableItemDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/image-style")
public class ImageStyleController {

    private final ImageStyleService imageStyleService;

    @GetMapping("/{id}/details")
    public ResponseEntity<ImageStyleDetailsDto> getDetailsById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(imageStyleService.getDetailsById(id));
    }

    @PostMapping("/{id}")
    public ResponseEntity<ImageStyleResponse> update(@PathVariable("id") UUID id, @RequestBody ImageStyleCreateUpdateDto updatedDto) {
        return ResponseEntity.ok(imageStyleService.update(id, updatedDto));
    }

    @PostMapping
    public ResponseEntity<ImageStyleResponse> addNewImageStylePrompt(@Valid @RequestBody ImageStyleCreateUpdateDto imageStyleCreateUpdateDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(imageStyleService.create(imageStyleCreateUpdateDto));
    }

    @PutMapping("/{id}/disable")
    public ImageStyleResponse disablePromptStyles(@PathVariable("id") UUID id) {
        return imageStyleService.changeActive(id, false);
    }

    @PutMapping("/{id}/enable")
    public ImageStyleResponse enablePromptStyles(@PathVariable("id") UUID id) {
        return imageStyleService.changeActive(id, true);
    }

    @GetMapping("/table")
    public ResponseEntity<Page<ImageStyleTableItemDto>> getTable(ImageStyleSearchRequest request) {
        return ResponseEntity.ok(imageStyleService.getTablePage(request));
    }

    @GetMapping("/dropdown")
    public ResponseEntity<Page<ImageStyleDropDownItemDto>> getDropDownPage(ImageStyleSearchRequest request) {
        return ResponseEntity.ok(imageStyleService.getDropDownPage(request));
    }
}
