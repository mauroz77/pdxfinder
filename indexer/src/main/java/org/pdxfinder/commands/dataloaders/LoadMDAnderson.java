package org.pdxfinder.commands.dataloaders;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.neo4j.ogm.session.Session;
import org.pdxfinder.graph.dao.*;
import org.pdxfinder.services.DataImportService;
import org.pdxfinder.services.UtilityService;
import org.pdxfinder.services.ds.Standardizer;
import org.pdxfinder.services.dto.LoaderDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Load data from University of Texas MD Anderson PDXNet.
 */
@Component
@Order(value = -17)
public class LoadMDAnderson extends LoaderBase implements CommandLineRunner {

    private final static Logger log = LoggerFactory.getLogger(LoadMDAnderson.class);

    private final static String DATASOURCE_ABBREVIATION = "PDXNet-MDAnderson";
    private final static String DATASOURCE_NAME = "MD Anderson Cancer Center";
    private final static String DATASOURCE_DESCRIPTION = "University Texas MD Anderson PDX mouse models for PDXNet.";
    private final static String DATASOURCE_CONTACT = "bfang@mdanderson.org";
    private final static String SOURCE_URL = null;

    private final static String PROVIDER_TYPE = "";
    private final static String ACCESSIBILITY = "";

    private final static String NOT_SPECIFIED = Standardizer.NOT_SPECIFIED;

    // for now all samples are of tumor tissue
    private final static Boolean NORMAL_TISSUE_FALSE = false;

    //   private HostStrain nsgBS;
    private Group mdaDS;
    private Group projectGroup;

    private Options options;
    private CommandLineParser parser;
    private CommandLine cmd;
    private HelpFormatter formatter;

    private DataImportService dataImportService;
    private Session session;

    @Autowired
    private UtilityService utilityService;

    @Value("${pdxfinder.data.root.dir}")
    private String dataRootDir;

    //   @Value("${mdapdx.url}")
    //   private String urlStr;
    @PostConstruct
    public void init() {
        formatter = new HelpFormatter();
    }

    public LoadMDAnderson(DataImportService dataImportService) {
        this.dataImportService = dataImportService;
    }

    @Override
    public void run(String... args) throws Exception {

        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        parser.accepts("loadMDA", "Load MDAnderson PDX data");

        parser.accepts("loadALL", "Load all, including MDA PDX data");
        OptionSet options = parser.parse(args);

        if (options.has("loadMDA") || options.has("loadALL")) {

            initMethod();

            mdAndersonAlgorithm();
        }

    }



    public void mdAndersonAlgorithm() throws Exception {

        step00StartReportManager();

        step01GetMetaDataFolder();

        for (int i = 0; i < listOfFiles.length; i++) {

            if (listOfFiles[i].isFile()) {

                this.jsonFile = rootDataDirectory + dataSourceAbbreviation + "/pdx/" + listOfFiles[i].getName();
                loaderTemplate();
            }
        }

        log.info("Finished loading " + dataSourceAbbreviation + " PDX data.");
    }



    @Override
    protected void initMethod() {

        log.info("Loading MDAnderson PDX data.");

        dto = new LoaderDTO();
        rootDataDirectory = dataRootDir;
        dataSource = DATASOURCE_ABBREVIATION;
        filesDirectory = dataRootDir + DATASOURCE_ABBREVIATION + "/pdx/";
        dataSourceAbbreviation = DATASOURCE_ABBREVIATION;
        dataSourceContact = DATASOURCE_CONTACT;
    }

    // MD ANDERSON uses default implementation Steps step01GetMetaDataFolder, step02GetMetaDataJSON

    @Override
    protected void step03CreateProviderGroup() {

        loadProviderGroup(DATASOURCE_NAME, DATASOURCE_ABBREVIATION, DATASOURCE_DESCRIPTION, PROVIDER_TYPE, DATASOURCE_CONTACT, SOURCE_URL);
    }

    @Override
    protected void step04CreateNSGammaHostStrain() {

    }

    @Override
    protected void step05CreateNSHostStrain() {

    }

    @Override
    protected void step06CreateProjectGroup() {

        loadProjectGroup("PDXNet");
    }


    @Override
    protected void step07GetPDXModels() {

        loadPDXModels(metaDataJSON,"MDA");
    }


    // MD ANDERSON uses default implementation Steps step08GetMetaData, step09LoadPatientData


    @Override
    protected void step10LoadExternalURLs() {

        loadExternalURLs(DATASOURCE_CONTACT,Standardizer.NOT_SPECIFIED);

    }


    @Override
    protected void step11LoadBreastMarkers() {

    }


    // IRCC uses default implementation Steps Step11CreateModels default


    @Override
    protected void step13LoadSpecimens()throws Exception {

        loadSpecimens("mdAnderson");

    }


    @Override
    protected void step14LoadCurrentTreatment() {

    }


    @Override
    protected void step15LoadImmunoHistoChemistry() {

    }


    @Override
    protected void step16LoadVariationData() {

    }



}
