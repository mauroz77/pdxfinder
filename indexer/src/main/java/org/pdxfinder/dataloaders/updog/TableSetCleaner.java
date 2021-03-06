package org.pdxfinder.dataloaders.updog;

import org.springframework.stereotype.Service;
import tech.tablesaw.api.Table;

import java.util.Map;

@Service
public class TableSetCleaner {

    public Map<String, Table> cleanPdxTables(Map<String, Table> pdxTableSet) {
        pdxTableSet = TableSetUtilities.removeProviderNameFromFilename(pdxTableSet);
        pdxTableSet.remove("metadata-checklist.tsv");
        TableSetUtilities.removeDescriptionColumn(pdxTableSet);
        pdxTableSet = TableSetUtilities.removeHeaderRows(pdxTableSet);
        pdxTableSet = TableSetUtilities.removeBlankRows(pdxTableSet);
        return pdxTableSet;
    }

    public Map<String, Table> cleanOmicsTables(Map<String, Table> omicsTableSet) {
        omicsTableSet = TableSetUtilities.removeProviderNameFromFilename(omicsTableSet);
        omicsTableSet = TableSetUtilities.removeHeaderRowsIfPresent(omicsTableSet);
        TableSetUtilities.removeDescriptionColumn(omicsTableSet);
        omicsTableSet = TableSetUtilities.removeBlankRows(omicsTableSet);
        return omicsTableSet;
    }

    public Table cleanOmicsTable(Table omicsTable){
        omicsTable = TableSetUtilities.removeHeaderRowsIfPresent(omicsTable);
        TableSetUtilities.removeColumnIfExists(omicsTable, "Field");
        return omicsTable;
    }

    public Map<String, Table> cleanTreatmentTables(Map<String, Table> treatmentTableSet) {
        treatmentTableSet = TableSetUtilities.removeProviderNameFromFilename(treatmentTableSet);
        treatmentTableSet = TableSetUtilities.removeHeaderRowsIfPresent(treatmentTableSet);
        TableSetUtilities.removeDescriptionColumn(treatmentTableSet);
        return treatmentTableSet;
    }
}
