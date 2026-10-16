package com.ai.generation.imagestyle;

import com.ai.generation.exception.EntityNotFoundException;
import com.ai.generation.imagestyle.dto.ImageStyleCreateUpdateDto;
import com.ai.generation.imagestyle.dto.ImageStyleDetailsDto;
import com.ai.generation.imagestyle.dto.ImageStyleDropDownItemDto;
import com.ai.generation.imagestyle.dto.ImageStyleResponse;
import com.ai.generation.imagestyle.dto.ImageStyleSearchRequest;
import com.ai.generation.imagestyle.dto.ImageStyleTableItemDto;
import com.ai.generation.imagestyle.dto.projection.ImageStyleDetailsProjection;
import com.ai.generation.imagestyle.repository.ImageStyleRepository;
import com.ai.generation.imagestyle.repository.ImageStyleSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageStyleService {

    private final ImageStyleRepository imageStyleRepository;

    public ImageStyle getById(UUID id) {
        return imageStyleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ImageStyle.class, id));
    }

    public ImageStyleDetailsDto getDetailsById(UUID id) {
        return new ImageStyleDetailsDto(imageStyleRepository.findById(id, ImageStyleDetailsProjection.class)
                .orElseThrow(() -> new EntityNotFoundException(ImageStyle.class, id)));
    }

    public List<ImageStyle> resolveImageStyles(Set<UUID> ids) {
        if (ids != null && !ids.isEmpty()) {
            return imageStyleRepository.findAllById(ids);
        }
        return imageStyleRepository.findByActive(true, ImageStyle.class);
    }

    @Transactional
    public ImageStyleResponse update(UUID id, ImageStyleCreateUpdateDto updateDto) {
        var imageStyle = getById(id);
        imageStyle.setName(updateDto.getName());
        imageStyle.setSystemPrompt(updateDto.getSystemPrompt());
        imageStyle.setActive(updateDto.isActive());
        return new ImageStyleResponse(imageStyle);
    }

    public Page<ImageStyleTableItemDto> getTablePage(ImageStyleSearchRequest searchRequest) {
        var specifications = new ArrayList<Specification<ImageStyle>>();
        specifications.add(ImageStyleSpecifications.byName(searchRequest.getName()));
        specifications.add(ImageStyleSpecifications.byActive(searchRequest.getActive()));
        var pageable = searchRequest.getPageable(ImageStyleTableItemDto.getSortingParam(searchRequest.getSortBy()).getName());

        return imageStyleRepository.findAll(Specification.allOf(specifications), pageable).map(ImageStyleTableItemDto::new);
    }

    public Page<ImageStyleDropDownItemDto> getDropDownPage(ImageStyleSearchRequest searchRequest) {
        var specifications = new ArrayList<Specification<ImageStyle>>();
        if (searchRequest.getName() != null) {
            specifications.add(ImageStyleSpecifications.byName(searchRequest.getName()));
        }

        searchRequest.setDirection(Sort.Direction.ASC);
        var pageable = searchRequest.getPageable(ImageStyleDropDownItemDto.SortField.NAME.getName());

        return imageStyleRepository.findAll(Specification.allOf(specifications), pageable).map(ImageStyleDropDownItemDto::new);
    }

    @Transactional
    public ImageStyleResponse changeActive(UUID id, boolean isActive) {
        var imageStyle = getById(id);
        imageStyle.setActive(isActive);
        return new ImageStyleResponse(imageStyle);
    }

    @Transactional
    public ImageStyleResponse create(ImageStyleCreateUpdateDto imageStyleCreateUpdateDto) {
        return new ImageStyleResponse(imageStyleRepository.save(new ImageStyle(imageStyleCreateUpdateDto)));
    }
}
