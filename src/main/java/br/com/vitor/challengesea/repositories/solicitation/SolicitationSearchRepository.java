package br.com.vitor.challengesea.repositories.solicitation;

import br.com.vitor.challengesea.model.solicitation.SolicitationDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SolicitationSearchRepository extends ElasticsearchRepository<SolicitationDocument, Long> {}
