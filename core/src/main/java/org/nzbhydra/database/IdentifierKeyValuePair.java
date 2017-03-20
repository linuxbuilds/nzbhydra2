package org.nzbhydra.database;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;


@Data
@Entity
public class IdentifierKeyValuePair {

    @Id
    @GeneratedValue
    private Integer id;

    public IdentifierKeyValuePair(String identifierKey, String identifierValue) {
        this.identifierKey = identifierKey;
        this.identifierValue = identifierValue;
    }

    private String identifierKey;
    private String identifierValue;


}
