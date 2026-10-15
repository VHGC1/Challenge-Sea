package br.com.vitor.challengesea.services.solicitation;

import br.com.vitor.challengesea.mappers.solicitation.SolicitationMapper;
import br.com.vitor.challengesea.model.solicitation.Solicitation;
import br.com.vitor.challengesea.model.solicitation.SolicitationIndexEvent;
import br.com.vitor.challengesea.repositories.solicitation.SolicitationSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Keeps the Elasticsearch index in step with PostgreSQL. A write queues the solicitation's
 * document; it is indexed only after the transaction commits, so a rollback never leaves a
 * document for data that does not exist.
 * <p>
 * An indexing failure is logged and does not fail the request, whose data is already committed:
 * the document catches up on the solicitation's next write.
 */
@Component
public class SolicitationIndexer {

    private static final Logger LOG = LoggerFactory.getLogger(SolicitationIndexer.class);

    private final ApplicationEventPublisher eventPublisher;
    private final SolicitationMapper solicitationMapper;
    private final SolicitationSearchRepository searchRepository;

    public SolicitationIndexer(
            ApplicationEventPublisher eventPublisher,
            SolicitationMapper solicitationMapper,
            SolicitationSearchRepository searchRepository) {
        this.eventPublisher = eventPublisher;
        this.solicitationMapper = solicitationMapper;
        this.searchRepository = searchRepository;
    }

    /**
     * Queues the solicitation for indexing; call it inside the writing transaction, after the save.
     *
     * @param solicitation the saved solicitation
     */
    public void indexAfterCommit(Solicitation solicitation) {
        eventPublisher.publishEvent(new SolicitationIndexEvent(solicitationMapper.toDocument(solicitation)));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIndexEvent(SolicitationIndexEvent event) {
        try {
            searchRepository.save(event.document());
        } catch (RuntimeException e) {
            // ponytail: no retry; add an outbox or a reindex job if the index must never lag
            LOG.error("Could not index solicitation {}", event.document().getId(), e);
        }
    }
}
