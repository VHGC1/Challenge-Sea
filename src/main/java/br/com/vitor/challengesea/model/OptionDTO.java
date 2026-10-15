package br.com.vitor.challengesea.model;

import java.util.Arrays;
import java.util.List;

public record OptionDTO(String option, String name, String description) {

    public static List<OptionDTO> of(OptionEnum... values) {
        return Arrays.stream(values)
                .map(value -> new OptionDTO(value.option(), value.label(), value.description()))
                .toList();
    }
}
