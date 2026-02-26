package com.ms.email.models;

import com.ms.email.enums.StatusEmail;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "TB_EMAIL")
public class EmailModel implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private UUID emailId;
    private String name;
    private String userEmail;
    private String emailConsultancy;
    private String companyName;
    private String telephone;
    @Column(columnDefinition = "TEXT")
    private String message;
    private LocalDateTime sendDateEmail;
    private StatusEmail statusEmail;
}
