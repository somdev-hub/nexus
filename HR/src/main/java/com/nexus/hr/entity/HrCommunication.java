package com.nexus.hr.entity;

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

    private String subject;

    private String body;

    @ManyToOne
    @JoinColumn(name = "hr_entity_hr_id")
    private HrEntity hrEntity;

    private String senderId;

    private List<String> receiverIds;

    @Enumerated(EnumType.STRING)
    private CommunicationStatus status;

    private Timestamp timestamp;

    private List<String> attachments;

    private List<String> ccEmails;

    private List<String> bccEmails;

}
