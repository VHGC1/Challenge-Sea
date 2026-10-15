package br.com.vitor.challengesea.model.user;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role;

    /** The states (UF) an ANALYST reviews; empty for every other role. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_covered_states", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "state")
    private Set<String> coveredStates = new HashSet<>();

    @CreationTimestamp
    private Instant createdAt;
}
