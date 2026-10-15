package br.com.vitor.challengesea.mappers.solicitation;

import br.com.vitor.challengesea.mappers.BaseMapperConfig;
import br.com.vitor.challengesea.model.solicitation.AddressDTO;
import br.com.vitor.challengesea.model.solicitation.BasicDataDTO;
import br.com.vitor.challengesea.model.solicitation.ConfirmationDTO;
import br.com.vitor.challengesea.model.solicitation.PendingFieldDTO;
import br.com.vitor.challengesea.model.solicitation.Solicitation;
import br.com.vitor.challengesea.model.solicitation.SolicitationDocument;
import br.com.vitor.challengesea.model.solicitation.SolicitationResponseDTO;
import br.com.vitor.challengesea.model.solicitation.SolicitationSummaryDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapperConfig.class)
public interface SolicitationMapper {

    @Mapping(target = "ownerId", source = "solicitation.owner.id")
    @Mapping(target = "analystId", source = "solicitation.analyst.id")
    SolicitationResponseDTO toResponse(Solicitation solicitation, List<PendingFieldDTO> pendingFields);

    @Mapping(target = "ownerId", source = "owner.id")
    SolicitationDocument toDocument(Solicitation solicitation);

    SolicitationSummaryDTO toSummary(SolicitationDocument document);

    void updateBasicData(BasicDataDTO dto, @MappingTarget Solicitation solicitation);

    /** The CEP is left to the service, which only keeps it once ViaCEP is asked about it. */
    @Mapping(target = "cep", ignore = true)
    void updateAddress(AddressDTO dto, @MappingTarget Solicitation solicitation);

    void updateConfirmation(ConfirmationDTO dto, @MappingTarget Solicitation solicitation);
}
