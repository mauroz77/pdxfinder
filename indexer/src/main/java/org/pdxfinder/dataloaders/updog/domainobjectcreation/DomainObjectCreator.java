package org.pdxfinder.dataloaders.updog.domainobjectcreation;

import org.pdxfinder.dataloaders.updog.TSV;
import org.pdxfinder.graph.dao.*;
import org.pdxfinder.services.DataImportService;
import org.pdxfinder.services.dto.NodeSuggestionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DomainObjectCreator {

    private Map<String, Map<String, Object>> domainObjects;
    private DataImportService dataImportService;
    private GroupCreator groupCreator;
    private PatientCreator patientCreator;
    private ModelCreationCreator modelCreationCreator;
    private static final Logger log = LoggerFactory.getLogger(DomainObjectCreator.class);

    private static final String FIRST = "first";
    private static final String PATIENTS = "patient";
    private static final String PROVIDER_GROUPS = "provider_group";
    private static final String MODELS = "model";
    private static final String TUMOR_TYPES = "tumor_type";
    private static final String TISSUES = "tissue";
    private static final String HOST_STRAINS = "hoststrain";
    private static final String ENGRAFTMENT_SITES = "engraftment_site";
    private static final String ENGRAFTMENT_TYPES = "engraftment_type";
    private static final String ENGRAFTMENT_MATERIALS = "engraftment_material";
    private static final String PLATFORMS = "platform";
    private static final String NOT_SPECIFIED = "Not Specified";

    public DomainObjectCreator(
        DataImportService dataImportService,
        GroupCreator groupCreator,
        PatientCreator patientCreator,
        ModelCreationCreator modelCreationCreator
    ) {
        this.dataImportService = dataImportService;
        this.groupCreator = groupCreator;
        this.patientCreator = patientCreator;
        this.modelCreationCreator = modelCreationCreator;
        domainObjects = new HashMap<>();
    }

    public void loadDomainObjects(Map<String, Table> pdxDataTables) {
        //: Do not change the order of these unless you want to risk 1. the universe to collapse OR 2. missing nodes in the db

        domainObjects = new HashMap<>();
        createProvider(pdxDataTables);
        createPatientData(pdxDataTables);
        createModelData(pdxDataTables);
        createSampleData(pdxDataTables);
        createSharingData(pdxDataTables);
        createSamplePlatformData(pdxDataTables);
        createTreatmentData(pdxDataTables);
        createOmicData(pdxDataTables);
        persistNodes();
    }

    public void callCreators(Map<String, Table> tableSet) {
        Group provider = patientCreator.createDependencies(tableSet);
        Set<Patient> patients = patientCreator.create(tableSet, provider);

        Set<Sample> samples = modelCreationCreator.createDependencies(tableSet);
        Set<Specimen> specimens = modelCreationCreator.createDependencies(tableSet, samples);
        Set<ModelCreation> models = modelCreationCreator.create(tableSet, provider, specimens);
    }

    void createProvider(Map<String, Table> pdxDataTables) {
        log.info("Creating provider");
        Table finderRelatedTable = pdxDataTables.get("metadata-loader.tsv");
        Row row = finderRelatedTable.row(0);
        String providerName = row.getString(TSV.Metadata.name.name());
        String abbrev = row.getString(TSV.Metadata.abbreviation.name());
        String internalUrl = row.getString(TSV.Metadata.internal_url.name());
        Group providerGroup = dataImportService.getProviderGroup(
                providerName, abbrev, "", "", "", internalUrl);
        addDomainObject(PROVIDER_GROUPS, FIRST, providerGroup);
    }

    void createPatientData(Map<String, Table> pdxDataTables) {
        log.info("Creating patient data");
        Table patientTable = pdxDataTables.get("metadata-patient.tsv");
        for (Row row : patientTable) {
            try {
                Patient patient = dataImportService.createPatient(
                        row.getText(TSV.Metadata.patient_id.name()),
                        (Group) getDomainObject(TSV.Metadata.provider_group.name(), FIRST),
                        row.getText(TSV.Metadata.sex.name()),
                        "",
                        row.getText(TSV.Metadata.ethnicity.name()));

                patient.setCancerRelevantHistory(row.getText(TSV.Metadata.history.name()));
                patient.setFirstDiagnosis(row.getText(TSV.Metadata.initial_diagnosis.name()));
                patient.setAgeAtFirstDiagnosis(row.getText(TSV.Metadata.age_at_initial_diagnosis.name()));

                addDomainObject(
                    PATIENTS,
                        row.getText(TSV.Metadata.patient_id.name()),
                        dataImportService.savePatient(patient));
            } catch (Exception e) {
                log.error(
                        "Error loading patient {} at row {}",
                        row.getText(TSV.Metadata.patient_id.name()),
                        row.getRowNumber());
            }
        }
    }

    void createSampleData(Map<String, Table> pdxDataTables) {
        log.info("Creating sample data");
        Table sampleTable = pdxDataTables.get("metadata-sample.tsv");
        for (Row row : sampleTable) {
            String patientId = row.getString(TSV.Metadata.patient_id.name());
            String modelId = row.getString(TSV.Metadata.model_id.name());
            String dateOfCollection = row.getString(TSV.Metadata.collection_date.name());
            String ageAtCollection = row.getString(TSV.Metadata.age_in_years_at_collection.name());
            String collectionEvent = row.getString(TSV.Metadata.collection_event.name());
            String elapsedTime = row.getString(TSV.Metadata.months_since_collection_1.name());
            String primarySiteName = row.getString(TSV.Metadata.primary_site.name());
            String virologyStatus = row.getString(TSV.Metadata.virology_status.name());
            String treatmentNaive = row.getString(TSV.Metadata.treatment_naive_at_collection.name());

            Patient patient = (Patient) getDomainObject(PATIENTS, patientId);
            if (patient == null) {
                log.error("Patient not found {}", patientId);
                throw new NullPointerException();}

            PatientSnapshot patientSnapshot = patient.getSnapShotByCollection(
                    ageAtCollection,
                    dateOfCollection,
                    collectionEvent,
                    elapsedTime);

            if (patientSnapshot == null) {
                patientSnapshot = new PatientSnapshot(
                        patient,
                        ageAtCollection,
                        dateOfCollection,
                        collectionEvent,
                        elapsedTime);
                patientSnapshot.setVirologyStatus(virologyStatus);
                patientSnapshot.setTreatmentNaive(treatmentNaive);
                patient.addSnapshot(patientSnapshot);
            }

            Sample sample = createPatientSample(row);
            patientSnapshot.addSample(sample);
            ModelCreation modelCreation = (ModelCreation) getDomainObject(MODELS, modelId);
            if (modelCreation == null) throw new NullPointerException();
            modelCreation.setSample(sample);
            modelCreation.addRelatedSample(sample);
        }
    }

    void createModelData(Map<String, Table> pdxDataTables) {
        log.info("Creating model data");
        Table modelTable = pdxDataTables.get("metadata-model.tsv");
        Group providerGroup = (Group) domainObjects.get(PROVIDER_GROUPS).get(FIRST);
        for (Row row : modelTable) {
            String modelId = row.getString(TSV.Metadata.model_id.name());
            String hostStrainNomenclature = row.getString(TSV.Metadata.host_strain_full.name());
            String passageNum = row.getString(TSV.Metadata.passage_number.name());

            ModelCreation modelCreation = new ModelCreation();
            modelCreation.setSourcePdxId(modelId);
            modelCreation.setDataSource(providerGroup.getAbbreviation());
            addDomainObject(MODELS, modelId, modelCreation);

            Specimen specimen = modelCreation.getSpecimenByPassageAndHostStrain(passageNum, hostStrainNomenclature);
            if (specimen == null) {
                specimen = createSpecimen(row, row.getRowNumber());
                modelCreation.addSpecimen(specimen);
                modelCreation.addRelatedSample(specimen.getSample());
            }
        }
        createModelValidationData(pdxDataTables);
    }

    void createModelValidationData(Map<String, Table> pdxDataTables) {

        Table modelValidationTable = pdxDataTables.get("metadata-model_validation.tsv");
        for (Row row : modelValidationTable) {
            String modelId = row.getString(TSV.Metadata.model_id.name());
            String validationTechnique = row.getString(TSV.Metadata.validation_technique.name());
            String description = row.getString(TSV.Metadata.description.name());
            String passagesTested = row.getString(TSV.Metadata.passages_tested.name());
            String hostStrainFull = row.getString(TSV.Metadata.validation_host_strain_full.name());

            ModelCreation modelCreation = (ModelCreation) getDomainObject(MODELS, modelId);
            if (modelCreation == null) throw new NullPointerException();

            QualityAssurance qa = new QualityAssurance();
            qa.setTechnology(validationTechnique);
            qa.setDescription(description);
            qa.setPassages(passagesTested);
            qa.setValidationHostStrain(hostStrainFull);
            modelCreation.addQualityAssurance(qa);
        }
    }

    void createSharingData(Map<String, Table> pdxDataTables) {
        log.info("Creating sharing data");
        Table sharingTable = pdxDataTables.get("metadata-sharing.tsv");

        Group providerGroup = (Group) domainObjects.get(PROVIDER_GROUPS).get(FIRST);

        for (Row row : sharingTable) {
            String modelId = row.getString(TSV.Metadata.model_id.name());
            String providerType = row.getString(TSV.Metadata.provider_type.name());
            String accessibility = row.getString(TSV.Metadata.accessibility.name());
            String europdxAccessModality = row.getString(TSV.Metadata.europdx_access_modality.name());
            String email = row.getString(TSV.Metadata.email.name());
            String formUrl = row.getString(TSV.Metadata.form_url.name());
            String databaseUrl = row.getString(TSV.Metadata.database_url.name());
            String project = row.getString(TSV.Metadata.project.name());

            ModelCreation modelCreation = (ModelCreation) getDomainObject(MODELS, modelId);
            if (modelCreation == null) throw new NullPointerException();

            List<ExternalUrl> externalUrls = getExternalUrls(email, formUrl, databaseUrl);
            modelCreation.setExternalUrls(externalUrls);

            Optional.ofNullable(project).ifPresent(
                    s -> {
                        Group projectGroup = dataImportService.getProjectGroup(s);
                        modelCreation.addGroup(projectGroup);
                    });

            if (eitherIsPresent(accessibility, europdxAccessModality)) {
                Group access = dataImportService.getAccessibilityGroup(accessibility, europdxAccessModality);
                access.setName("Has access information");
                modelCreation.addGroup(access);
            }

            providerGroup.setProviderType(providerType);
            providerGroup.setContact(email);
        }
    }

    void createSamplePlatformData(Map<String, Table> pdxDataTables) {
        log.info("Creating sample platforms");
        Table samplePlatformTable = pdxDataTables.get("sampleplatform-data.tsv");
        if (samplePlatformTable == null) return;

        for (Row row : samplePlatformTable) {
            String sampleOrigin = row.getString(TSV.SamplePlatform.sample_origin.name());
            String platformName = row.getString(TSV.SamplePlatform.platform.name());
            String molCharType = row.getString(TSV.SamplePlatform.molecular_characterisation_type.name());
            Sample sample = null;

            if (sampleOrigin.equals(PATIENTS)) {
                sample = getPatientSample(row);
            } else if (sampleOrigin.equals("xenograft")) {
                sample = getOrCreateSpecimen(row).getSample();
            }
            if (sample == null) throw new NullPointerException();
            getOrCreateMolecularCharacterization(sample, platformName, molCharType);
        }
    }

    void createTreatmentData(Map<String, Table> pdxDataTables){
        log.info("Creating patient treatments");
        Table treatmentTable = pdxDataTables.get("patienttreatment-Sheet1.tsv");

        if(treatmentTable != null){
           for(Row row : treatmentTable){
                String patientId = getStringFromRowAndColumn(row, TSV.Metadata.patient_id.name());
                Patient patient = (Patient) getDomainObject(PATIENTS, patientId);
                if(patient == null) throw new NullPointerException();
                PatientSnapshot patientSnapshot = patient.getLastSnapshot();
                TreatmentProtocol treatmentProtocol = getTreatmentProtocol(row);
                patientSnapshot.addTreatmentProtocol(treatmentProtocol);
           }
        }
    }

    void createDrugDosingData(Map<String, Table> pdxDataTables){
        Table drugdosingTable = pdxDataTables.get("drugdosing-Sheet1.tsv");
        if(drugdosingTable != null){
            for(Row row : drugdosingTable){
                String modelId = getStringFromRowAndColumn(row, TSV.Metadata.model_id.name());
                ModelCreation model = (ModelCreation) getDomainObject(MODELS, modelId);
                if(model == null) throw new NullPointerException();
                TreatmentProtocol treatmentProtocol = getTreatmentProtocol(row);
                model.addTreatmentProtocol(treatmentProtocol);
            }
        }
    }

    void createOmicData(Map<String, Table> pdxDataTables) {
        log.info("Creating molecular data");
        createMutationData(pdxDataTables);
        createCnaData(pdxDataTables);
        createCytogeneticsData(pdxDataTables);
    }

    private void createMutationData(Map<String, Table> pdxDataTables){
        Table mutationTable = pdxDataTables.get("mut.tsv");
        if(mutationTable != null){
            createMolecularCharacterization(mutationTable, "mutation");
        }
        else{
            Map<String, Object> models = domainObjects.get(MODELS);
            for(Map.Entry<String, Object> entry : models.entrySet()){
                ModelCreation modelCreation = (ModelCreation) entry.getValue();
                String mutationModelId = "mut_"+modelCreation.getSourcePdxId()+".tsv";
                mutationTable = pdxDataTables.get(mutationModelId);
                if(mutationTable != null){
                    log.info(modelCreation.getSourcePdxId());
                    createMolecularCharacterization(mutationTable, "mutation");
                }
            }
        }
    }

    private void createCnaData(Map<String, Table> pdxDataTables){
        Table cnaTable = pdxDataTables.get("cna.tsv");
        if (cnaTable != null) {
            createMolecularCharacterization(cnaTable, "copynumberalteration");
        } else {
            Map<String, Object> models = domainObjects.get(MODELS);
            for(Map.Entry<String, Object> entry : models.entrySet()){
                ModelCreation modelCreation = (ModelCreation) entry.getValue();
                String cnaModelId = "cna_"+modelCreation.getSourcePdxId()+".tsv";
                cnaTable = pdxDataTables.get(cnaModelId);
                if(cnaTable != null){
                    log.info(modelCreation.getSourcePdxId());
                    createMolecularCharacterization(cnaTable, "copynumberalteration");
                }
            }
        }
    }

    private void createCytogeneticsData(Map<String, Table> pdxDataTables){
        Table cytoTable = pdxDataTables.get("cytogenetics-Sheet1.tsv");
        if (cytoTable != null)
            createMolecularCharacterization(cytoTable, "cytogenetics");
    }

    private void createMolecularCharacterization(Table table, String molcharType){
        MolecularCharacterization molecularCharacterization = getMolcharByType(table.row(0), molcharType);
        MarkerAssociation markerAssociation = new MarkerAssociation();
        for (Row row : table) {
            MolecularData molecularData = createMolecularDataObject(molecularCharacterization, row);

            if(molecularData.getMarker() != null){
                markerAssociation.addMolecularData(createMolecularDataObject(molecularCharacterization, row));
            }
        }
        molecularCharacterization.addMarkerAssociation(markerAssociation);
        molecularCharacterization.setMarkers(getMarkers(markerAssociation));
        addDomainObject("molecular_characterization", molcharType, molecularCharacterization);
    }

    private MolecularCharacterization getMolcharByType(Row row, String molCharType) {

        String sampleOrigin = row.getString("sample_origin");
        String platformName = row.getString(PLATFORMS);
        Sample sample = null;

        if (sampleOrigin.equalsIgnoreCase("patient")) {
            sample = getPatientSample(row);
        } else if (sampleOrigin.equalsIgnoreCase("xenograft")) {
            sample = getOrCreateSpecimen(row).getSample();
        }

        if (sample == null) {
            log.error(sampleOrigin);
            throw new NullPointerException();}

        return getOrCreateMolecularCharacterization(sample, platformName, molCharType);
    }

    private Set getMarkers(MarkerAssociation markerAssociation) {
        return markerAssociation
            .getMolecularDataList()
            .stream()
            .map(MolecularData::getMarker)
            .collect(Collectors.toSet());
    }

    private Sample getPatientSample(Row row) {

        String modelId = row.getString(TSV.Mutation.model_id.name());
        ModelCreation modelCreation = (ModelCreation) getDomainObject(MODELS, modelId);
        if (modelCreation == null) throw new NullPointerException();

        return modelCreation.getSample();
    }

    private Specimen getOrCreateSpecimen(Row row) {
        // For Mutation
        String modelId = row.getString(TSV.Mutation.model_id.name());
        String hostStrainSymbol = row.getString(TSV.Mutation.host_strain_nomenclature.name());
        String passage = getStringFromRowAndColumn(row, TSV.Mutation.passage.name());
        if(hostStrainSymbol.equals("")) hostStrainSymbol = NOT_SPECIFIED;
        String sampleId = row.getString(TSV.Mutation.sample_id.name());
        ModelCreation modelCreation = (ModelCreation) getDomainObject(MODELS, modelId);
        if (modelCreation == null){
            log.error("Model not found: {}", modelId);
            throw new NullPointerException();
        }

        Specimen specimen = modelCreation.getSpecimenByPassageAndHostStrain(passage, hostStrainSymbol);
        if (specimen == null) {
            specimen = new Specimen();
            specimen.setPassage(passage);

            HostStrain hostStrain = getOrCreateHostStrain(NOT_SPECIFIED, hostStrainSymbol, row.getRowNumber());
            specimen.setHostStrain(hostStrain);

            Sample sample = new Sample();
            sample.setSourceSampleId(sampleId);
            specimen.setSample(sample);
            modelCreation.addSpecimen(specimen);
            modelCreation.addRelatedSample(sample);
        }
        return specimen;
    }

    private MolecularCharacterization getOrCreateMolecularCharacterization(Sample sample, String platformName, String molCharType) {

        MolecularCharacterization molecularCharacterization = sample.getMolecularCharacterization(molCharType, platformName);
        if (molecularCharacterization == null) {
            molecularCharacterization = new MolecularCharacterization();
            molecularCharacterization.setType(molCharType);
            molecularCharacterization.setPlatform(getOrCreatePlatform(platformName, molCharType));
            sample.addMolecularCharacterization(molecularCharacterization);
        }
        return molecularCharacterization;
    }

    private Platform getOrCreatePlatform(String platformName, String molCharType) {

        Group providerGroup = (Group) getDomainObject(PROVIDER_GROUPS, FIRST);
        String platformId = molCharType + platformName;
        Platform platform = (Platform) getDomainObject(PLATFORMS, platformId);
        if (platform == null) {
            platform = new Platform();
            platform.setGroup(providerGroup);
            platform.setName(platformName);
            addDomainObject(PLATFORMS, platformId, platform);
        }
        return platform;
    }

    private MolecularData createMolecularDataObject (
        MolecularCharacterization molecularCharacterization,
        Row row
    ) {
        MolecularData molecularData = new MolecularData();
        String hgncSymbol = row.getString("symbol");
        String modelId = row.getString("model_id");
        Group provider = (Group) domainObjects.get(PROVIDER_GROUPS).get(FIRST);
        String dataSource = provider.getAbbreviation();
        NodeSuggestionDTO nodeSuggestionDTO = dataImportService.getSuggestedMarker(
            this.getClass().getSimpleName(),
            dataSource,
            modelId,
            hgncSymbol,
            molecularCharacterization.getType(),
            molecularCharacterization.getPlatform().getName());
        if (nodeSuggestionDTO.getNode() != null) {
            logMarkerSuggestions(nodeSuggestionDTO);
            molecularData = createMolecularCharacterization(
                molecularCharacterization.getType(),
                row,
                (Marker) nodeSuggestionDTO.getNode());
        }
        return molecularData;
    }

    private void logMarkerSuggestions(NodeSuggestionDTO nodeSuggestionDTO) {
        if (nodeSuggestionDTO.getNode() == null)
            log.error(nodeSuggestionDTO.getLogEntity().getMessage());
    }

    private MolecularData createMolecularCharacterization(String type, Row row, Marker marker) {
        MolecularData molecularData;
        switch (type) {
            case "mutation":
                molecularData = getMutationProperties(row, marker);
                break;
            case "cytogenetics":
                molecularData = getCytogeneticsProperties(row, marker);
                break;
            case "copynumberalteration":
                molecularData = getCNAProperties(row, marker);
                break;
            default:
                molecularData = new MolecularData();
        }
        return molecularData;
    }

    private MolecularData getMutationProperties(Row row, Marker marker) {
        MolecularData ma = new MolecularData();
        try {
            ma.setBiotype(getStringFromRowAndColumn(row,TSV.Mutation.biotype.name()));
            ma.setCodingSequenceChange(getStringFromRowAndColumn(row,TSV.Mutation.coding_sequence_change.name()));
            ma.setVariantClass(getStringFromRowAndColumn(row,TSV.Mutation.variant_class.name()));
            ma.setCodonChange(getStringFromRowAndColumn(row,TSV.Mutation.codon_change.name()));
            ma.setAminoAcidChange(getStringFromRowAndColumn(row, TSV.Mutation.amino_acid_change.name()));
            ma.setConsequence(getStringFromRowAndColumn(row, TSV.Mutation.consequence.name()));
            ma.setFunctionalPrediction(getStringFromRowAndColumn(row,TSV.Mutation.functional_prediction.name()));
            ma.setReadDepth(getStringFromRowAndColumn(row, TSV.Mutation.read_depth.name()));
            ma.setAlleleFrequency(getStringFromRowAndColumn(row, TSV.Mutation.allele_frequency.name()));
            ma.setChromosome(getStringFromRowAndColumn(row, TSV.Mutation.chromosome.name()));
            ma.setSeqStartPosition(getStringFromRowAndColumn(row, TSV.Mutation.seq_start_position.name()));
            ma.setRefAllele(getStringFromRowAndColumn(row, TSV.Mutation.ref_allele.name()));
            ma.setAltAllele(getStringFromRowAndColumn(row, TSV.Mutation.alt_allele.name()));
            ma.setUcscGeneId(getStringFromRowAndColumn(row,TSV.Mutation.ucsc_gene_id.name()));
            ma.setNcbiGeneId(getStringFromRowAndColumn(row,TSV.Mutation.ncbi_gene_id.name()));
            ma.setNcbiTranscriptId(getStringFromRowAndColumn(row,TSV.Mutation.ncbi_transcript_id.name()));
            ma.setEnsemblTranscriptId(getStringFromRowAndColumn(row, TSV.Mutation.ensembl_transcript_id.name()));
            ma.setExistingVariations(getStringFromRowAndColumn(row, TSV.Mutation.variation_id.name()));
            ma.setGenomeAssembly(getStringFromRowAndColumn(row, TSV.Mutation.genome_assembly.name()));

            ma.setNucleotideChange("");
            ma.setMarker(marker.getHgncSymbol());
        } catch (Exception e) {
            log.error(e.toString());
        }
        return ma;
    }

    private MolecularData getCNAProperties(Row row, Marker marker){

        MolecularData ma = new MolecularData();
        ma.setChromosome(getStringFromRowAndColumn(row, TSV.CopyNumberAlteration.chromosome.name()));
        ma.setSeqStartPosition(getStringFromRowAndColumn(row, TSV.CopyNumberAlteration.seq_start_position.name()));
        ma.setSeqEndPosition(getStringFromRowAndColumn(row, TSV.CopyNumberAlteration.seq_end_position.name()));
        ma.setCnaLog10RCNA(getStringFromRowAndColumn(row, TSV.CopyNumberAlteration.log10r_cna.name()));
        ma.setCnaLog2RCNA(getStringFromRowAndColumn(row, TSV.CopyNumberAlteration.log2r_cna.name()));
        ma.setCnaCopyNumberStatus(getStringFromRowAndColumn(row, TSV.CopyNumberAlteration.copy_number_status.name()));
        ma.setCnaGisticValue(getStringFromRowAndColumn(row, TSV.CopyNumberAlteration.gistic_value.name()));
        ma.setCnaPicnicValue(getStringFromRowAndColumn(row, TSV.CopyNumberAlteration.picnic_value.name()));
        ma.setGenomeAssembly(getStringFromRowAndColumn(row, TSV.CopyNumberAlteration.genome_assembly.name()));
        ma.setMarker(marker.getHgncSymbol());
        return  ma;
    }

    private MolecularData getTranscriptomicProperties(Row row, Marker marker){

        MolecularData ma = new MolecularData();
        ma.setChromosome("");
        ma.setSeqStartPosition("");
        ma.setSeqEndPosition("");
        ma.setRnaSeqCoverage("");
        ma.setRnaSeqFPKM("");
        ma.setRnaSeqTPM("");
        ma.setRnaSeqCount("");
        ma.setAffyHGEAProbeId("");
        ma.setAffyHGEAExpressionValue("");
        ma.setIlluminaHGEAProbeId("");
        ma.setIlluminaHGEAExpressionValue("");
        ma.setGenomeAssembly("");
        ma.setZscore("");
        ma.setMarker(marker.getHgncSymbol());
        return  ma;
    }

    private MolecularData getCytogeneticsProperties(Row row, Marker marker){

        MolecularData ma = new MolecularData();
        try {
            ma.setMarker(marker.getHgncSymbol());
        } catch (Exception e) {
        }
        return ma;
    }

    private boolean eitherIsPresent(String string, String anotherString) {
        return (
                Optional.ofNullable(string).isPresent() ||
                        Optional.ofNullable(anotherString).isPresent()
        );
    }

    private List<ExternalUrl> getExternalUrls(String email, String formUrl, String databaseUrl) {
        List<ExternalUrl> externalUrls = new ArrayList<>();
        Optional.ofNullable(email).ifPresent(
                s -> externalUrls.add(
                        dataImportService.getExternalUrl(ExternalUrl.Type.CONTACT, s)));
        Optional.ofNullable(formUrl).ifPresent(
                s -> externalUrls.add(
                        dataImportService.getExternalUrl(ExternalUrl.Type.CONTACT, s)));
        Optional.ofNullable(databaseUrl).ifPresent(
                s -> externalUrls.add(
                        dataImportService.getExternalUrl(ExternalUrl.Type.SOURCE, s)));
        return externalUrls;
    }

    private Specimen createSpecimen(Row row, int rowNumber) {

        String hostStrainName = row.getString(TSV.Metadata.host_strain.name());
        String hostStrainNomenclature = row.getString(TSV.Metadata.host_strain_full.name());
        String engraftmentSiteName = row.getString(TSV.Metadata.engraftment_site.name());
        String engraftmentTypeName = row.getString(TSV.Metadata.engraftment_type.name());
        String sampleType = row.getString(TSV.Metadata.sample_type.name());
        String passageNum = row.getString(TSV.Metadata.passage_number.name());

        HostStrain hostStrain = getOrCreateHostStrain(hostStrainName, hostStrainNomenclature, rowNumber);
        EngraftmentSite engraftmentSite = getOrCreateEngraftment(engraftmentSiteName);
        EngraftmentType engraftmentType = getOrCreateEngraftmentType(engraftmentTypeName);
        EngraftmentMaterial engraftmentMaterial = getOrCreateEngraftmentMaterial(sampleType);

        Sample xenoSample = new Sample();
        Specimen specimen = new Specimen();
        specimen.setPassage(passageNum);
        specimen.setHostStrain(hostStrain);
        specimen.setEngraftmentMaterial(engraftmentMaterial);
        specimen.setEngraftmentSite(engraftmentSite);
        specimen.setEngraftmentType(engraftmentType);
        specimen.setSample(xenoSample);

        return specimen;
    }

    private EngraftmentMaterial getOrCreateEngraftmentMaterial(String sampleType) {
        EngraftmentMaterial engraftmentMaterial = (EngraftmentMaterial) getDomainObject(ENGRAFTMENT_MATERIALS, sampleType);
        if (engraftmentMaterial == null) {
            engraftmentMaterial = dataImportService.getEngraftmentMaterial(sampleType);
            addDomainObject(ENGRAFTMENT_MATERIALS, sampleType, engraftmentMaterial);
        }
        return engraftmentMaterial;
    }

    private EngraftmentType getOrCreateEngraftmentType(String engraftmentTypeName) {
        EngraftmentType engraftmentType = (EngraftmentType) getDomainObject(ENGRAFTMENT_TYPES, engraftmentTypeName);
        if (engraftmentType == null) {
            engraftmentType = dataImportService.getImplantationType(engraftmentTypeName);
            addDomainObject(ENGRAFTMENT_TYPES, engraftmentTypeName, engraftmentType);
        }
        return engraftmentType;
    }

    private EngraftmentSite getOrCreateEngraftment(String engraftmentSiteName) {
        EngraftmentSite engraftmentSite = (EngraftmentSite) getDomainObject(ENGRAFTMENT_SITES, engraftmentSiteName);
        if (engraftmentSite == null) {
            engraftmentSite = dataImportService.getImplantationSite(engraftmentSiteName);
            addDomainObject(ENGRAFTMENT_SITES, engraftmentSiteName, engraftmentSite);
        }
        return engraftmentSite;
    }

    private HostStrain getOrCreateHostStrain(String hostStrainName, String hostStrainNomenclature, int rowNumber) {
        HostStrain hostStrain = (HostStrain) getDomainObject(HOST_STRAINS, hostStrainNomenclature);
        if (hostStrain == null) {
            try {
                hostStrain = dataImportService.getHostStrain(hostStrainName, hostStrainNomenclature, "", "");
                addDomainObject(HOST_STRAINS, hostStrainNomenclature, hostStrain);
            } catch (Exception e) {
                //log.error("Host strain symbol is empty in row {}", rowNumber);
            }
        }
        return hostStrain;
    }

    private Sample createPatientSample(Row row) {

        String diagnosis = row.getString(TSV.Metadata.diagnosis.name());
        String sampleId = row.getString(TSV.Metadata.sample_id.name());
        String tumorTypeName = row.getString(TSV.Metadata.tumour_type.name());
        String primarySiteName = row.getString(TSV.Metadata.primary_site.name());
        String collectionSiteName = row.getString(TSV.Metadata.collection_site.name());
        String stage = row.getString(TSV.Metadata.stage.name());
        String stagingSystem = row.getString(TSV.Metadata.staging_system.name());
        String grade = row.getString(TSV.Metadata.grade.name());
        String gradingSystem = row.getString(TSV.Metadata.grading_system.name());

        Tissue primarySite = getOrCreateTissue(primarySiteName);
        Tissue collectionSite = getOrCreateTissue(collectionSiteName);
        TumorType tumorType = getOrCreateTumorType(tumorTypeName);

        Sample sample = new Sample();
        sample.setType(tumorType);
        sample.setSampleSite(collectionSite);
        sample.setOriginTissue(primarySite);

        sample.setSourceSampleId(sampleId);
        sample.setDiagnosis(diagnosis);
        sample.setStage(stage);
        sample.setStageClassification(stagingSystem);
        sample.setGrade(grade);
        sample.setGradeClassification(gradingSystem);

        return sample;
    }

    private TumorType getOrCreateTumorType(String tumorTypeName) {
        TumorType tumorType = (TumorType) getDomainObject(TUMOR_TYPES, tumorTypeName);
        if (tumorType == null) {
            tumorType = dataImportService.getTumorType(tumorTypeName);
            addDomainObject(TUMOR_TYPES, tumorTypeName, tumorType);
        }
        return tumorType;
    }

    private Tissue getOrCreateTissue(String siteName) {
        Tissue primarySite = (Tissue) getDomainObject(TISSUES, siteName);
        if (primarySite == null) {
            primarySite = dataImportService.getTissue(siteName);
            addDomainObject(TISSUES, siteName, primarySite);
        }
        return primarySite;
    }

    private void persistNodes() {
        persistPatients();
        persistModels();
    }

    public void persistPatients(){

        log.info("Persisiting patients");
        Map<String, Object> patients = domainObjects.get(PATIENTS);
        for (Object pat : patients.values()) {
            Patient patient = (Patient) pat;
            for(PatientSnapshot ps: patient.getSnapshots()){
                for(Sample patientSample : ps.getSamples())
                    encodeMolecularDataFor(patientSample);
            }
            dataImportService.savePatient(patient);
        }
    }

    public void persistModels(){

        log.info("Persisiting models");
        Map<String, Object> models = domainObjects.get(MODELS);
        for (Object modelObject : models.values()) {
            ModelCreation model = (ModelCreation) modelObject;
            if (model.hasSpecimens())
                for (Specimen s : model.getSpecimens()) encodeMolecularDataFor(s);

            log.debug("Saving model {}", (model.getSourcePdxId()));
            dataImportService.saveModelCreation(model);
        }
    }

    public void addDomainObject(String key1, String key2, Object object) {
        if (domainObjects.containsKey(key1)) {
            domainObjects.get(key1).put(key2, object);
        } else {
            Map map = new HashMap();
            map.put(key2, object);
            domainObjects.put(key1, map);
        }
    }

    public Object getDomainObject(String key1, String key2) {
        if (domainObjects.containsKey(key1))
            return domainObjects.get(key1).get(key2);
        else {
            log.error("Tried to access nonexistant domain object key {}", key1);
            return null;
        }
    }

    private void encodeMolecularDataFor(Specimen specimen) {
        if (specimen.getSample() != null)
            encodeMolecularDataFor(specimen.getSample());
    }

    private void encodeMolecularDataFor(Sample sample) {
        if (sample.hasMolecularCharacterizations())
            for (MolecularCharacterization mc : sample.getMolecularCharacterizations()) encodeMolecularDataFor(mc);
    }

    private void encodeMolecularDataFor(MolecularCharacterization mc) {
        if (mc.hasMarkerAssociations())
            mc.getFirstMarkerAssociation().encodeMolecularData();
    }

    private TreatmentProtocol getTreatmentProtocol(Row row){

        String drugString = "";
        String doseString = "";
        String response = "";
        String responseClassification = "";
        String[] drugArray = drugString.split("\\+");
        String[] doseArray = doseString.split(";");
        TreatmentProtocol tp = new TreatmentProtocol();
        if(drugArray.length == doseArray.length){
            for(int i=0;i<drugArray.length;i++){
                Treatment treatment = new Treatment();
                treatment.setName(drugArray[i].trim());
                TreatmentComponent tc = new TreatmentComponent();
                tc.setDose(doseArray[i].trim());
                tc.setTreatment(treatment);
                tp.addTreatmentComponent(tc);
            }
            return tp;
        } else if(drugArray.length != doseArray.length && doseArray.length == 1){

            for(int i=0;i<drugArray.length;i++){
                Treatment treatment = new Treatment();
                treatment.setName(drugArray[i].trim());
                TreatmentComponent tc = new TreatmentComponent();
                tc.setDose(doseArray[0].trim());
                tc.setTreatment(treatment);
                tp.addTreatmentComponent(tc);
            }
            return tp;
        }
        return null;
    }

    private String getStringFromRowAndColumn(Row row, String columnName){

        try {
            if (row.getColumnType(columnName) == ColumnType.STRING) {
                return row.getString(columnName);
            } else if (row.getColumnType(columnName) == ColumnType.DOUBLE) {
                return Double.toString(row.getDouble(columnName));
            } else if (row.getColumnType(columnName) == ColumnType.INTEGER) {
                return Integer.toString(row.getInt(columnName));
            }
        }
        catch(Exception e){
           //column is not present, so return null
           return null;
        }
        return null;
    }

    private String getMolecularTypeKey(String s){
        return "";
    }

}
