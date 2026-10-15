package br.com.vitor.challengesea.mappers;

import org.mapstruct.MapperConfig;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy =
                NullValuePropertyMappingStrategy.IGNORE // Null fields in a DTO never overwrite entity values
        )
public interface BaseMapperConfig {}
