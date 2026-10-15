package br.com.vitor.challengesea.model.solicitation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

/** The searchable copy of a {@link Solicitation}; PostgreSQL stays the source of truth. */
@Getter
@Setter
@NoArgsConstructor
@Document(indexName = "solicitations")
@Setting(shards = 1, replicas = 0) // single node: a replica could never be assigned
public class SolicitationDocument {
    @Id
    private Long id;

    @Field(type = FieldType.Long)
    private Long ownerId;

    @Field(type = FieldType.Keyword)
    private SolicitationStatus status;

    @Field(type = FieldType.Keyword)
    private ServiceType serviceType;

    @Field(type = FieldType.Keyword)
    private Priority priority;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Keyword)
    private String state;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate preferredDate;

    @Field(type = FieldType.Double)
    private BigDecimal estimatedValue;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant submittedAt;
}
