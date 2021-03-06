package org.pdxfinder.dataloaders.updog.domainobjectcreation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pdxfinder.BaseTest;
import org.pdxfinder.graph.dao.*;
import org.pdxfinder.services.DataImportService;
import org.pdxfinder.services.dto.NodeSuggestionDTO;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class DomainObjectCreatorTest extends BaseTest {

    @Mock private DataImportService dataImportService;
    @Mock private GroupCreator groupCreator;
    @Mock private PatientCreator patientCreator;
    @Mock private ModelCreationCreator modelCreationCreator;
    @InjectMocks private DomainObjectCreator domainObjectCreator;

    private Map<String, Table> pdxDataTables;
    private Group providerGroup;
    private Group accessibilityGroup;
    private Patient testPatient;
    private ModelCreation testModel;
    private static final String key1 = "model";
    private static final String key2 = "MOCK_MODEL";
    private static final String passage = "0";
    private static final String hostStrainSymbol = "Not Specified";
    private static final String enaAccession = "ERP120397";
    private static final String platformName = "Next Generation Sequencing";
    private static final String mcType = "mutation";

    private static final String FIRST = "first";

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        pdxDataTables = getTestPdxDataTables();

        providerGroup = Group.createProviderGroup("TestProvider", "TP", "description", "", "", "");
        providerGroup.setType("Provider");

        accessibilityGroup = Group.createAccessibilityGroup("academia", "");

        testPatient = new Patient("patient 1", "female", "-", "ethnicity",providerGroup );
        testModel = new ModelCreation("model1");
    }


    @Test
    public void Given_ProviderTable_When_CreateProviderIsCalled_Then_ProviderNodeIsInDomainMap(){

        when(dataImportService.getProviderGroup(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(providerGroup);
        domainObjectCreator.createProvider(getTestPdxDataTables());

        Group provider = (Group) domainObjectCreator.getDomainObject("provider_group", FIRST);
        Assert.assertEquals("TP", provider.getAbbreviation());
    }


    @Test
    public void Given_PatientTable_When_CreatePatient_Then_PatientNodeIsInMap(){

        domainObjectCreator.addDomainObject("provider_group", FIRST, providerGroup);
        when(dataImportService.createPatient("patient 1", providerGroup, "female", "", "ethnicity"))
            .thenReturn(testPatient);
        when(dataImportService.savePatient(testPatient))
            .thenReturn(testPatient);

        domainObjectCreator.createPatientData(getTestPdxDataTables());

        Patient patient = (Patient) domainObjectCreator.getDomainObject("patient", "patient 1");

        Assert.assertEquals("patient 1", patient.getExternalId());
        Assert.assertEquals("female", patient.getSex());
    }

    @Test
    public void Given_ModelTable_When_CreateModel_Then_ModelNodeIsInMap(){

        domainObjectCreator.addDomainObject("provider_group", FIRST, providerGroup);
        domainObjectCreator.createModelData(getTestPdxDataTables());
        ModelCreation model = (ModelCreation) domainObjectCreator.getDomainObject("model", "model 1");
        Assert.assertEquals("model 1", model.getSourcePdxId());

    }

    @Test
    public void Given_SampleTable_When_CreateSample_Then_SampleIsInMap(){

        domainObjectCreator.addDomainObject("provider_group", FIRST, providerGroup);
        domainObjectCreator.addDomainObject("patient", "patient 1", testPatient);
        domainObjectCreator.addDomainObject("model", "model 1", testModel);

        when(dataImportService.getTumorType("Primary")).thenReturn(new TumorType("Primary"));

        domainObjectCreator.createSampleData(getTestPdxDataTables());

        ModelCreation model = (ModelCreation) domainObjectCreator.getDomainObject("model", "model 1");
        Patient patient = (Patient) domainObjectCreator.getDomainObject("patient", "patient 1");

        Sample sample = model.getSample();

        Assert.assertEquals("sample 1", sample.getSourceSampleId());
        Assert.assertEquals("70", patient.getSnapshotByDate("01/01/1900").getAgeAtCollection());
        Assert.assertEquals("Primary", sample.getType().getName());
    }

    @Test
    public void Given_SharingTable_When_CreateSharing_Then_SharingInfoAdded(){

        domainObjectCreator.addDomainObject("provider_group", FIRST, providerGroup);
        domainObjectCreator.addDomainObject("model", "model 1", testModel);

        when(dataImportService.getProjectGroup("project 1")).thenReturn(getProjectGroup("project 1"));
        when(dataImportService.getAccessibilityGroup("academia", "collaboration only"))
            .thenReturn(Group.createAccessibilityGroup("academia", "collaboration only"));

        domainObjectCreator.createSharingData(getTestPdxDataTables());

        ModelCreation model = (ModelCreation) domainObjectCreator.getDomainObject("model", "model 1");
        Set<Group> groups = model.getGroups();

        Group projectGroup = new Group();
        Group accessGroup = new Group();

        for(Group group : groups){
            if(group != null && group.getType().equals("Project")) projectGroup = group;
            if(group != null && group.getType().equals("Accessibility")) accessGroup = group;
        }

        Assert.assertEquals("project 1", projectGroup.getName());
        Assert.assertEquals("academia", accessGroup.getAccessibility());
        Assert.assertEquals("collaboration only", accessGroup.getAccessModalities());
    }



    public static Map<String, Table> getTestPdxDataTables(){

        String loader = "metadata-loader.tsv";
        String patient = "metadata-patient.tsv";
        String model = "metadata-model.tsv";
        String modelValidation = "metadata-model_validation.tsv";
        String sample = "metadata-sample.tsv";
        String sharing = "metadata-sharing.tsv";
        String samplePlatform = "sampleplatform-data.tsv";
        String mutation = "mutation.tsv";
        String cytogenetics = "cytogenetics-Sheet1.tsv";
        String cna = "cna.tsv";

        Map<String, Table> pdxDataTables = new HashMap<>();
        pdxDataTables.put(loader, Table.create(loader).addColumns(
            StringColumn.create("name", Collections.singletonList("Test Provider")),
            StringColumn.create("abbreviation", Collections.singletonList("TP")),
            StringColumn.create("internal_url", Collections.singletonList("/source/test")),
            StringColumn.create("internal_dosing_url", Collections.singletonList(""))
        ));
        pdxDataTables.put(patient, Table.create(patient).addColumns(
            StringColumn.create("patient_id", Collections.singletonList("patient 1")),
            StringColumn.create("sex", Collections.singletonList("female")),
            StringColumn.create("history", Collections.singletonList("history")),
            StringColumn.create("ethnicity", Collections.singletonList("ethnicity")),
            StringColumn.create("initial_diagnosis", Collections.singletonList("initial diagnosis")),
            StringColumn.create("age_at_initial_diagnosis", Collections.singletonList("age at initial diagnosis"))
        ));
        pdxDataTables.put(model, Table.create(model).addColumns(
            StringColumn.create("model_id", Collections.singletonList("model 1")),
            StringColumn.create("host_strain",Collections.singletonList("host strain name 1")),
            StringColumn.create("host_strain_full",Collections.singletonList("host strain nomenclature 1")),
            StringColumn.create("engraftment_site",Collections.singletonList("engraftment site 1")),
            StringColumn.create("engraftment_type",Collections.singletonList("engraftment type 1")),
            StringColumn.create("sample_type",Collections.singletonList("sample type 1")),
            StringColumn.create("sample_state",Collections.singletonList("sample state 1")),
            StringColumn.create("passage_number",Collections.singletonList("1")),
            StringColumn.create("publications",Collections.singletonList("publications 1"))
        ));
        pdxDataTables.put(modelValidation, Table.create(modelValidation).addColumns(
            StringColumn.create("model_id", Collections.singletonList("model 1")),
            StringColumn.create("validation_technique", Collections.singletonList("technique")),
            StringColumn.create("description", Collections.singletonList("description 1")),
            StringColumn.create("passages_tested", Collections.singletonList("passage 1")),
            StringColumn.create("validation_host_strain_full", Collections.singletonList("host strain 1"))
        ));
        pdxDataTables.put(sample, Table.create(sample).addColumns(
            StringColumn.create("patient_id", Collections.singletonList("patient 1")),
            StringColumn.create("sample_id", Collections.singletonList("sample 1")),
            StringColumn.create("collection_date", Collections.singletonList("01/01/1900")),
            StringColumn.create("age_in_years_at_collection", Collections.singletonList("70")),
            StringColumn.create("diagnosis", Collections.singletonList("Cancer")),
            StringColumn.create("tumour_type", Collections.singletonList("Primary")),
            StringColumn.create("primary_site", Collections.singletonList("Breast")),
            StringColumn.create("collection_site", Collections.singletonList("Breast")),
            StringColumn.create("model_id", Collections.singletonList("model 1")),
            StringColumn.create("collection_event", Collections.singletonList("")),
            StringColumn.create("months_since_collection_1", Collections.singletonList("")),
            StringColumn.create("virology_status", Collections.singletonList("")),
            StringColumn.create("stage", Collections.singletonList("")),
            StringColumn.create("staging_system", Collections.singletonList("")),
            StringColumn.create("grade", Collections.singletonList("")),
            StringColumn.create("grading_system", Collections.singletonList("")),
            StringColumn.create("treatment_naive_at_collection", Collections.singletonList(""))
        ));
        pdxDataTables.put(sharing, Table.create(sharing).addColumns(
            StringColumn.create("model_id", Collections.singletonList("model 1")),
            StringColumn.create("provider_type", Collections.singletonList("academia")),
            StringColumn.create("accessibility", Collections.singletonList("academia")),
            StringColumn.create("europdx_access_modality", Collections.singletonList("collaboration only")),
            StringColumn.create("email", Collections.singletonList("test@test.com")),
            StringColumn.create("form_url", Collections.singletonList("www.test.com")),
            StringColumn.create("database_url", Collections.singletonList("www.test.com")),
            StringColumn.create("project", Collections.singletonList("project 1"))));

        pdxDataTables.put(samplePlatform, Table.create(samplePlatform).addColumns(
                StringColumn.create("sample_id", Collections.singletonList("sample 1")),
                StringColumn.create("sample_origin", Collections.singletonList("xenograft")),
                StringColumn.create("passage", Collections.singletonList(passage)),
                StringColumn.create("model_id", Collections.singletonList(key2)),
                StringColumn.create("host_strain_name", Collections.singletonList("")),
                StringColumn.create("host_strain_nomenclature", Collections.singletonList(hostStrainSymbol)),
                StringColumn.create("molecular_characterisation_type", Collections.singletonList(mcType)),
                StringColumn.create("raw_data_file", Collections.singletonList(enaAccession)),
                StringColumn.create("platform", Collections.singletonList(platformName)),
                StringColumn.create("internal_protocol_url", Collections.singletonList("www.pdxFinder.org"))
        ));

        pdxDataTables.put(mutation, Table.create(mutation).addColumns(
                StringColumn.create("model_id", Collections.singletonList("model 1")),
                StringColumn.create("sample_id", Collections.singletonList("sample 1")),
                StringColumn.create("sample_origin", Collections.singletonList("xenograft")),
                StringColumn.create("host_strain_nomenclature", Collections.singletonList("host strain nomenclature 1")),
                StringColumn.create("passage", Collections.singletonList("0")),
                StringColumn.create("hgnc_symbol", Collections.singletonList("KRAS")),
                StringColumn.create("amino_acid_change", Collections.singletonList("L22F1")),
                StringColumn.create("consequence", Collections.singletonList("")),
                StringColumn.create("allele_frequency", Collections.singletonList("")),
                StringColumn.create("chromosome", Collections.singletonList("")),
                StringColumn.create("read_depth", Collections.singletonList("")),
                StringColumn.create("ref_allele", Collections.singletonList("")),
                StringColumn.create("alt_allele", Collections.singletonList("")),
                StringColumn.create("genome_assembly", Collections.singletonList("")),
                StringColumn.create("variation_id", Collections.singletonList("")),
                StringColumn.create("seq_start_position", Collections.singletonList("")),
                StringColumn.create("ensembl_transcript_id", Collections.singletonList("")),
                StringColumn.create("platform", Collections.singletonList("Next Generation Sequencing"))
        ));

        pdxDataTables.put(cytogenetics, Table.create(cytogenetics).addColumns(
                StringColumn.create("sample_id", Collections.singletonList("sample 1")),
                StringColumn.create("sample_origin", Collections.singletonList("xenograft")),
                StringColumn.create("passage", Collections.singletonList("0")),
                StringColumn.create("host_strain_nomenclature", Collections.singletonList("host strain nomenclature 1")),
                StringColumn.create("model_id", Collections.singletonList("model 1")),
                StringColumn.create("symbol", Collections.singletonList("ERBB2")),
                StringColumn.create("marker_status", Collections.singletonList("KRAS")),
                StringColumn.create("platform", Collections.singletonList("ImmunoHistoChemistry"))
        ));

        pdxDataTables.put(cna, Table.create(cna).addColumns(
                StringColumn.create("sample_id", Collections.singletonList("sample 1")),
                StringColumn.create("sample_origin", Collections.singletonList("xenograft")),
                StringColumn.create("passage", Collections.singletonList("0")),
                StringColumn.create("host_strain_nomenclature", Collections.singletonList("host strain nomenclature 1")),
                StringColumn.create("chromosome", Collections.singletonList("")),
                StringColumn.create("seq_start_position", Collections.singletonList("")),
                StringColumn.create("seq_end_position", Collections.singletonList("")),
                StringColumn.create("log10r_cna", Collections.singletonList("")),
                StringColumn.create("log2r_cna", Collections.singletonList("")),
                StringColumn.create("copy_number_status", Collections.singletonList("")),
                StringColumn.create("gistic_value", Collections.singletonList("")),
                StringColumn.create("picnic_value", Collections.singletonList("")),
                StringColumn.create("genome_assembly", Collections.singletonList("")),
                StringColumn.create("symbol", Collections.singletonList("PTEN")),
                StringColumn.create("platform", Collections.singletonList("Targeted Next Generation Sequencing")),
                StringColumn.create("model_id", Collections.singletonList("model 1"))
        ));

        return pdxDataTables;
    }

    private Group getProjectGroup(String name){

        Group group = new Group();
        group.setType("Project");
        group.setName(name);
        return group;
    }

    private NodeSuggestionDTO getSuggestedMarker(){

        Marker marker = new Marker();
        marker.setHgncSymbol("KRAS");

        NodeSuggestionDTO nsdto = new NodeSuggestionDTO();
        nsdto.setNode(marker);

        return nsdto;
    }

    @Test public void callCreators_givenTableSet_callsObjectCreationInCorrectOrder() {
        // just checking the universe has not collapsed ;)
        InOrder inOrder = inOrder(patientCreator, modelCreationCreator);
        domainObjectCreator.callCreators(pdxDataTables);

        inOrder.verify(patientCreator).createDependencies(any());
        inOrder.verify(patientCreator).create(any(), any());
        inOrder.verify(modelCreationCreator).createDependencies(any());
        inOrder.verify(modelCreationCreator).create(any(), any(), any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test public void callCreators_givenTableSet_callsCreatePatient() {
        domainObjectCreator.callCreators(pdxDataTables);
        verify(patientCreator).create(any(), any());
    }

    @Test public void callCreators_givenTableSet_callsCreateModelCreation() {
        domainObjectCreator.callCreators(pdxDataTables);
        verify(modelCreationCreator).create(any(), any(), any());
    }

    @Test public void createSamplePlatformData_givenBlankSampleAlreadyExists_overwriteSampleId(){
        ModelCreation mockModel = new ModelCreation();
        Sample mockSample = new Sample();
        mockSample.setSourceSampleId("");
        HostStrain mockHostStrain = new HostStrain(hostStrainSymbol);
        Specimen mockSpecimen = new Specimen();

        mockSpecimen.setPassage(passage);
        mockSpecimen.setSample(mockSample);
        mockSpecimen.setHostStrain(mockHostStrain);
        mockModel.setSpecimens(Collections.singleton(mockSpecimen));
        mockModel.addRelatedSample((mockSpecimen.getSample()));

        domainObjectCreator.addDomainObject(key1, key2, mockModel);
        domainObjectCreator.createSamplePlatformData(pdxDataTables);

        Assert.assertNotNull(mockModel
                .getSpecimenByPassageAndHostStrain(passage, hostStrainSymbol)
                .getSample()
                .getSourceSampleId());

        Assert.assertNotEquals(mockModel
                .getSpecimenByPassageAndHostStrain(passage, hostStrainSymbol)
                .getSample()
                .getSourceSampleId(), "");
    }

    @Test public void createSamplePlatformdata_givenEnaEgaAndGeoAccessions_buildLinktoResource(){
        String egaAccession = "EGAN00001285956";
        String geoAccession = "GSM1986620";
        String rubbishAccession ="rubbish";

        String expectedEna = String.format("https://www.ebi.ac.uk/ena/data/view/%s", enaAccession);
        String expectedEga = String.format("https://www.ebi.ac.uk/ega/search/site/%s", egaAccession);
        String expectedGeo = String.format("https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=%s", geoAccession);
        String expectedRubbish = "";

        String actualEna = domainObjectCreator.formatAccessionToURL(enaAccession);
        String actualEga = domainObjectCreator.formatAccessionToURL(egaAccession);
        String actualGeo = domainObjectCreator.formatAccessionToURL(geoAccession);
        String actualRubbish = domainObjectCreator.formatAccessionToURL(rubbishAccession);

        Assert.assertEquals(actualEna.split(",")[0], enaAccession);
        Assert.assertEquals(actualEga.split(",")[0], egaAccession);
        Assert.assertEquals(actualGeo.split(",")[0], geoAccession);

        Assert.assertEquals(actualEna.split(",")[1], expectedEna);
        Assert.assertEquals(actualEga.split(",")[1], expectedEga);
        Assert.assertEquals(actualGeo.split(",")[1], expectedGeo);
        Assert.assertEquals(actualRubbish, "");
    }


    @Test public void getCellAsText_whenGivenDifferentColumnTypes_returnsString() {
        Table table = Table.create(
            "table",
            StringColumn.create("StringColumn", "x"),
            IntColumn.create("IntColumn", new int[]{1}),
            DoubleColumn.create("DoubleColumn", 1.0));

        Assert.assertEquals(
            "x",
            domainObjectCreator.getCellAsText(table.row(0), "StringColumn"));
        Assert.assertEquals(
            "1",
            domainObjectCreator.getCellAsText(table.row(0), "IntColumn"));
        Assert.assertEquals(
            "1.0",
            domainObjectCreator.getCellAsText(table.row(0), "DoubleColumn"));
    }
}
