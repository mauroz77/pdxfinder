package org.pdxfinder.dao;

import org.neo4j.ogm.annotation.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by csaba on 07/06/2017.
 */
@NodeEntity
public class OntologyTerm {

    @GraphId
    private Long id;

    private String url;
    private String label;
    private int directMappedSamplesNumber;
    private int indirectMappedSamplesNumber;

    @Relationship(type = "SUBCLASS_OF" ,direction = Relationship.OUTGOING)
    private Set<OntologyTerm> subclassOf;

    @Relationship(type = "MAPPED_TO", direction = Relationship.INCOMING)
    private SampleToDiseaseOntologyRelationship mappedTo;




    public OntologyTerm() {
    }

    public OntologyTerm(String url, String label) {
        this.url = url;
        this.label = label;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Set<OntologyTerm> getSubclassOf() {
        return subclassOf;
    }

    public void setSubclassOf(Set<OntologyTerm> subclassOf) {
        this.subclassOf = subclassOf;
    }

    public SampleToDiseaseOntologyRelationship getMappedTo() {
        return mappedTo;
    }

    public void setMappedTo(SampleToDiseaseOntologyRelationship mappedTo) {
        this.mappedTo = mappedTo;
    }

    public void addSubclass(OntologyTerm ot){
        if(this.subclassOf == null){
            this.subclassOf = new HashSet<OntologyTerm>();
        }
        this.subclassOf.add(ot);
    }

    public int getDirectMappedSamplesNumber() {
        return directMappedSamplesNumber;
    }

    public void setDirectMappedSamplesNumber(int directMappedSamplesNumber) {

        this.directMappedSamplesNumber = directMappedSamplesNumber;
    }

    public int getIndirectMappedSamplesNumber() {
        return indirectMappedSamplesNumber;
    }

    public void setIndirectMappedSamplesNumber(int indirectMappedSamplesNumber) {
        this.indirectMappedSamplesNumber = indirectMappedSamplesNumber;
    }
}
