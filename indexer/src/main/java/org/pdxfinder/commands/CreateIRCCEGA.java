package org.pdxfinder.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.neo4j.ogm.session.Session;
import org.pdxfinder.dao.*;
import org.pdxfinder.irccdatamodel.IRCCEGARow;
import org.pdxfinder.irccdatamodel.IRCCMarkerMutation;
import org.pdxfinder.irccdatamodel.IRCCPatient;
import org.pdxfinder.irccdatamodel.IRCCSample;
import org.pdxfinder.utilities.LoaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by csaba on 08/08/2017.
 */
@Component
public class CreateIRCCEGA implements CommandLineRunner {

    private final static Logger log = LoggerFactory.getLogger(CreateIRCCEGA.class);

    private Options options;
    private CommandLineParser parser;
    private CommandLine cmd;
    private HelpFormatter formatter;

    private LoaderUtils loaderUtils;
    private Session session;


    List<IRCCEGARow> newFile;

    private final String SEPARATOR = "\t";
    private final String END_OF_LINE = "\n";
    private static final String FILE_HEADER = "id,firstName,lastName,gender,age";


    @Override
    @Transactional
    public void run(String... args) throws Exception {

        log.info(args[0]);

        if ("createIRCCEGA".equals(args[0]) || "-createIRCCEGA".equals(args[0])) {

            log.info("Creating IRCC EGA file");

            loadDataFromFile();
            insertDataFromDB();
            createAndSave();


        }
    }

    public CreateIRCCEGA(LoaderUtils loaderUtils) {
        this.loaderUtils = loaderUtils;
        this.newFile = new ArrayList<>();
    }

    private void loadDataFromFile() {


        String currentLine;
        long currentLineCounter = 1;
        String[] rowData;

        try {
            BufferedReader buf = new BufferedReader(new FileReader("/Users/csaba/PDX/LoaderData/IRCC/EGA_IRCC.txt"));

            while (true) {
                currentLine = buf.readLine();

                if (currentLine == null) {
                    break;
                } else if (currentLineCounter < 2) {
                    currentLineCounter++;
                    continue;

                } else {
                    rowData = currentLine.split("\t");

                    IRCCEGARow row = new IRCCEGARow(rowData[0], rowData[1], rowData[2], rowData[3], rowData[4], rowData[5], rowData[6]);
                    // t_PGDX670X_Ex
                    String id = rowData[2];
                    String[] split = id.split("_");
                    row.setSampleType(split[0]);
                    row.setSequencingPlatform(split[2]);

                    String id2 = split[1];
                    row.setSampleOrigin(id2.substring(id2.length() - 1));
                    row.setSampleId(split[0]+"_"+id2);
                    row.setSpecimenId(id2.substring(0, id2.length() - 1));

                    this.newFile.add(row);
                }
                currentLineCounter++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void insertDataFromDB(){

        for(IRCCEGARow row :this.newFile){

            String modelId = row.getTorinoId();

            PatientSnapshot ps = loaderUtils.getPatientSnapshotByModelId(modelId);

            if(ps != null){

                row.setAge(ps.getAge());
                if(ps.getPatient()!= null){
                    row.setSex(ps.getPatient().getSex());
                    row.setEthnicity(ps.getPatient().getEthnicity());
                    row.setRace(ps.getPatient().getRace());
                    row.setDataSource(ps.getPatient().getDataSource());
                    row.setExternalId(ps.getPatient().getExternalId());
                }



            }


        }

    }

    private void createAndSave(){

        System.out.println();    FileWriter fileWriter = null;


        try {

            fileWriter = new FileWriter("/Users/csaba/PDX/LoaderData/IRCC/EGA_IRCC_updated.txt");

            fileWriter.append("EGA ID");
            fileWriter.append(SEPARATOR);
            fileWriter.append("SAME ID");
            fileWriter.append(SEPARATOR);
            fileWriter.append("Internal ID");
            fileWriter.append(SEPARATOR);
            fileWriter.append("Torino ID");
            fileWriter.append(SEPARATOR);


            fileWriter.append("Patient external ID");
            fileWriter.append(SEPARATOR);
            fileWriter.append("Sex");
            fileWriter.append(SEPARATOR);
            fileWriter.append("Age");
            fileWriter.append(SEPARATOR);
            fileWriter.append("Source sample id");
            fileWriter.append(SEPARATOR);





            fileWriter.append("Bam file name");
            fileWriter.append(SEPARATOR);
            fileWriter.append("MD5 Sum");
            fileWriter.append(SEPARATOR);
            fileWriter.append("PGP Encrypted MD5");
            fileWriter.append(SEPARATOR);
            fileWriter.append(END_OF_LINE);



            for (IRCCEGARow row : newFile) {

                fileWriter.append(row.getEgaId());
                fileWriter.append(SEPARATOR);
                fileWriter.append(row.getSameId());
                fileWriter.append(SEPARATOR);
                fileWriter.append(row.getInternalId());
                fileWriter.append(SEPARATOR);
                fileWriter.append(row.getTorinoId());
                fileWriter.append(SEPARATOR);


                fileWriter.append(row.getExternalId());
                fileWriter.append(SEPARATOR);
                fileWriter.append(row.getSex());
                fileWriter.append(SEPARATOR);
                fileWriter.append(row.getAge());
                fileWriter.append(SEPARATOR);
                fileWriter.append(row.getSourceSampleId());
                fileWriter.append(SEPARATOR);



                fileWriter.append(row.getEgaId());
                fileWriter.append(SEPARATOR);
                fileWriter.append(row.getBamFileName());
                fileWriter.append(SEPARATOR);
                fileWriter.append(row.getMd5Sum());
                fileWriter.append(SEPARATOR);
                fileWriter.append(row.getEncrMd5());







                fileWriter.append(END_OF_LINE);

            }

            System.out.println("CSV file was created successfully.");

        } catch(Exception e) {

            System.out.println("Error in CsvFileWriter !!!");

            e.printStackTrace();

        } finally{

            try {

                fileWriter.flush();

                fileWriter.close();

            } catch (IOException e) {

                System.out.println("Error while flushing/closing fileWriter !!!");

                e.printStackTrace();

            }
        }
    }

}
