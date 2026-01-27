package com.nexus.hr.model.entities;

import com.nexus.hr.model.enums.CommunicationStatus;
import com.nexus.hr.model.enums.CommunicationType;
import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Data
@Table(name = "t_hr_communications", schema = "hr")
public class HrCommunication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long hrCommunicationId;

    @Enumerated(EnumType.STRING)
    private CommunicationType communicationType; // e.g., Email, Phone, Address

    @Column(length = 500)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    @ManyToOne
    @JoinColumn(name = "hr_entity_hr_id")
    private HrEntity hrEntity;

    @Column(length = 500)
    private String senderId;

    @Column(columnDefinition = "TEXT")
    private List<String> receiverIds;

    @Enumerated(EnumType.STRING)
    private CommunicationStatus status;

    private Timestamp timestamp;

    @Column(columnDefinition = "TEXT")
    private List<String> attachments;

    @Column(columnDefinition = "TEXT")
    private List<String> ccEmails;

    @Column(columnDefinition = "TEXT")
    private List<String> bccEmails;

}
