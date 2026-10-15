package br.com.vitor.challengesea.model;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponseDTO<T>(List<T> items, int page, int size, long total) {

    public static <T> PageResponseDTO<T> of(Page<T> page) {
        return new PageResponseDTO<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
