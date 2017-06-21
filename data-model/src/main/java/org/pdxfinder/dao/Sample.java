package org.pdxfinder.dao;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;

/**
 * Sample represents a piece of tissue taken from a specimen (human or mouse)
 * <p>
 * A sample could be cancerous or not (tissue used to compare to cancer sampled from a health tissue)
 */
@NodeEntity
public class Sample {

    @GraphId
    private Long id;

    private String sourceSampleId;
    private String diagnosis;
    private Tissue originTissue;
    private Tissue sampleSite;
    private String classification;
    private String dataSource;
    private ExternalDataSource externalDataSource;
    public Boolean normalTissue;

    @Relationship(type="MAPPED_TO")
    private SampleToDiseaseOntologyRelationship sampleToDiseaseOntologyRelationship;

    @Relationship(type = "OF_TYPE", direction = Relationship.OUTGOING)
    private TumorType type;

    @Relationship(type = "CHARACTERIZED_BY", direction = Relationship.INCOMING)
    private Set<MolecularCharacterization> molecularCharacterizations;

    public Sample() {
        // Empty constructor required as of Neo4j API 2.0.5
    }

    public Sample(String sourceSampleId, TumorType type, String diagnosis, Tissue originTissue, Tissue sampleSite, String classification, Boolean normalTissue, ExternalDataSource externalDataSource) {
        this.sourceSampleId = sourceSampleId;
        this.type = type;
        this.diagnosis = diagnosis;
        this.originTissue = originTissue;
        this.sampleSite = sampleSite;
        this.classification = classification;
        this.dataSource = externalDataSource.getAbbreviation();
        this.externalDataSource = externalDataSource;
        this.normalTissue = normalTissue;

    }

    public String getSourceSampleId() {
        return sourceSampleId;
    }

    public void setSourceSampleId(String sourceSampleId) {
        this.sourceSampleId = sourceSampleId;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public Tissue getOriginTissue() {
        return originTissue;
    }

    public void setOriginTissue(Tissue originTissue) {
        this.originTissue = originTissue;
    }

    public Tissue getSampleSite() {
        return sampleSite;
    }

    public void setSampleSite(Tissue sampleSite) {
        this.sampleSite = sampleSite;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public ExternalDataSource getExternalDataSource() {
        return externalDataSource;
    }

    public void setExternalDataSource(ExternalDataSource externalDataSource) {
        this.externalDataSource = externalDataSource;
    }

    public Boolean getNormalTissue() {
        return normalTissue;
    }

    public void setNormalTissue(Boolean normalTissue) {
        this.normalTissue = normalTissue;
    }

    public TumorType getType() {
        return type;
    }

    public void setType(TumorType type) {
        this.type = type;
    }

    public Set<MolecularCharacterization> getMolecularCharacterizations() {
        return molecularCharacterizations;
    }

    public void setMolecularCharacterizations(Set<MolecularCharacterization> molecularCharacterizations) {
        this.molecularCharacterizations = molecularCharacterizations;
    }

    public SampleToDiseaseOntologyRelationship getSampleToDiseaseOntologyRelationship() {
        return sampleToDiseaseOntologyRelationship;
    }

    public void setSampleToDiseaseOntologyRelationship(SampleToDiseaseOntologyRelationship sampleToDiseaseOntologyRelationship) {
        this.sampleToDiseaseOntologyRelationship = sampleToDiseaseOntologyRelationship;
    }
}
