/**
 * Created by csaba on 12/05/2017.
 */


/** This function updates the UI filter based on latest changes in USER CHOICE
 *
 * @param ages : A list of patients age selected by the user
 * @param genders : A list of patient genders/sex selected by the user
 * @param cancersystem : A list of cancer systems selected by the user
 * @param datasources : A list of data sources selced by the user
 * @param tumortype : A list of tumour types selected by the user
 *
 */
function updateFilters(ages, genders, cancersystem, datasources, tumortype) {

    console.log("updating filters!");
    //characters we want to see as values
    var reg = /[^A-Za-z0-9 _-]/;

    var openAgeFacet = false;
    var openGenderFacet = false;
    var openDatasourceFacet = false;
    var openCancerBySystem = false;
    var openTumorTypeFacet = false;

    //check selected age bins
    if (ages != null && ages.length > 0) {

        jQuery.each(ages, function (key, value) {

            var id = value.name;
            id = id.replace(" ", "_");
            var selected = value.selected;

            //testing id for invalid characters
            if (reg.test(id)) {
                console.log("skipping id: " + id);
                return;
            }

            if (selected) {
                jQuery("#patient_age__" + id).prop('checked', true);
                jQuery("#patient_age__" + id).siblings("label").find("span").addClass("selected");
                openAgeFacet = true;
            }

            //Add a plus to patient age 90
            if (id == '90') {
                jQuery("#patient_age__" + id).siblings("label").find("span").append("+");
            }

            var count = " (" + value.count + " of " + value.totalCount + ")";
            // jQuery("#patient_age__"+id).siblings("label").find("span").append(count);


        });

        if (openAgeFacet) {
            var ageFilterField = jQuery("li#age_filter > a.accordion-title");
            ageFilterField.click();
        }
    }


    //check selected gender options
    if (genders != null && genders.length > 0) {

        jQuery.each(genders, function (key, value) {

            var id = value.name;
            id = id.replace(" ", "_");
            var selected = value.selected;

            //testing id for invalid characters
            if (reg.test(id)) {
                console.log("skipping id: " + id);
                return;
            }

            if (selected) {
                jQuery("#patient_gender__" + id).prop('checked', true);
                jQuery("#patient_gender__" + id).siblings("label").find("span").addClass("selected");
                openGenderFacet = true;
            }

            var count = " (" + value.count + " of " + value.totalCount + ")";
            // jQuery("#patient_gender__"+id).siblings("label").find("span").append(count);

        });

        if (openGenderFacet) {
            var genderFilterField = jQuery("li#gender_filter > a.accordion-title");
            genderFilterField.click();
        }
    }


    //check selected cancer systems
    if (cancersystem != null && cancersystem.length > 0) {

        jQuery.each(cancersystem, function (key, value) {

            var id = value.name;
            id = id.replace(/ /g, "_");
            var selected = value.selected;

            console.log("system id:" + id);
            //testing id for invalid characters
            if (reg.test(id)) {
                console.log("skipping id: " + id);
                return;
            }

            if (selected) {
                jQuery("#cancer_system__" + id).prop('checked', true);
                jQuery("#cancer_system__" + id).siblings("label").find("span").addClass("selected");
                openCancerBySystem = true;
            }

            var count = " (" + value.count + " of " + value.totalCount + ")";
            // jQuery("#cancer_system__"+id).siblings("label").find("span").append(count);

        });

        if (openCancerBySystem) {
            var cancerSystemFilterField = jQuery("li#cancer_system_filter > a.accordion-title");
            cancerSystemFilterField.click();
        }
    }


    //check selected datasources
    if (datasources != null && datasources.length > 0) {

        jQuery.each(datasources, function (key, value) {

            var id = value.name;
            id = id.replace(" ", "_");
            var selected = value.selected;

            //testing id for invalid characters
            if (reg.test(id)) {
                console.log("skipping id: " + id);
                return;
            }

            if (selected) {
                jQuery("#datasource__" + id).prop('checked', true);
                jQuery("#datasource__" + id).siblings("label").find("span").addClass("selected");
                openDatasourceFacet = true;
            }

            var count = " (" + value.count + " of " + value.totalCount + ")";
            // jQuery("#datasource__" + id).siblings("label").find("span").append(count);

        });

        if (openDatasourceFacet) {
            var dsFilterField = jQuery("li#datasource_filter > a.accordion-title");
            dsFilterField.click();
        }


    }

    //check selected tumorTypes
    if (tumortype != null && tumortype.length > 0) {

        jQuery.each(tumortype, function (key, value) {

            var id = value.name;
            id = id.replace(" ", "_");
            var selected = value.selected;

            //testing id for invalid characters
            if (reg.test(id)) {
                console.log("skipping id: " + id);
                return;
            }

            if (selected) {
                jQuery("#sample_tumor_type__" + id).prop('checked', true);
                jQuery("#sample_tumor_type__" + id).siblings("label").find("span").addClass("selected");
                openTumorTypeFacet = true;
            }

            var count = " (" + value.count + " of " + value.totalCount + ")";
            // jQuery("#sample_tumor_type__" + id).siblings("label").find("span").append(count);

        });

        if (openTumorTypeFacet) {
            var ttFilterField = jQuery("li#tumor_type_filter > a.accordion-title");
            ttFilterField.click();
        }


    }

    // Check selected Molechular Characterization:
    var urlParams = new URLSearchParams(window.location.search);
    var dURLString = urlParams.toString();
    var openMarkerFacet = dURLString.search("mutation");
    if (openMarkerFacet != -1) {
        var markerFilterField = jQuery("li#marker_filter > a.accordion-title");
        markerFilterField.click();
    }

}
/* End updateFilters function */




function redirectPage(){

    var no_parameters = true;
    var url = "?"

    var searchField = jQuery("#query");

    if (searchField.val() != null && searchField.val() != "") {
        url+="query="+searchField.val();
        no_parameters = false;
    }


    for (var i=1; i<20; i++){

        var geneFilter = jQuery("#geneFilter"+i);
        var variantFilter = jQuery("#variantFilter"+i);

        if (geneFilter.val() != null && geneFilter.val() != "")
        {
            var allVariants = getVariantSize(geneFilter.val());
            for (var j=0; j<variantFilter.val().length; j++){
                if (!no_parameters) {
                    url = url + "&";
                }
                if(allVariants.length == variantFilter.val().length){
                    url += "mutation=" + geneFilter.val() + "___MUT" + "___ALL";
                    no_parameters = false;
                    break;
                }else{
                    url += "mutation=" + geneFilter.val() + "___MUT" + "___"+variantFilter.val()[j];
                    no_parameters = false;

                }
            }
        }

    }


    // Add all diagnosis filters to the URL
    jQuery(".diagnosis").each(function () {
        var id = jQuery(this).attr("id");

        //characters we want to see as values
        var reg = /[^A-Za-z0-9 _-]/;

        var res = id.split("__");

        if (!no_parameters) {
            url = url + "&";
        }

        if (!reg.test(res[1])) {
            url = url + res[0] + "=" + encodeURIComponent(res[1].replace(/_/g, ' '));
            no_parameters = false;
        }
    });


    //get all filters with values
    jQuery(".filter").each(function(){
        var id = jQuery(this).attr("id");

        //characters we want to see as values
        var reg = /[^A-Za-z0-9 _-]/;

        if (jQuery(this).is(':checked')){

            var res = id.split("__");

            if(!no_parameters){
                url = url+"&";
            }

            if( ! reg.test(res[1])){
                url = url+res[0]+"="+encodeURIComponent(res[1].replace(/_/g, ' '));
                no_parameters = false;

            }
        }
        else if(jQuery(this).is("input:text")){
            return;
        }
    });
    window.location.replace(url);
}







var geneticVar = 1;
var counter = 1;

function addMarkerAndVariants(param, startIndex) {
    if (startIndex != 2 && counter == 1) {
        geneticVar = startIndex;
    }
    geneticVar++;
    counter++;
    for (var i = startIndex; i <= 20; i++) {
        if ((param == 'AND' || param == 'OR') && geneticVar == i) {
            //$("#geneticVar"+i).show();
            document.getElementsByClassName("geneticVar" + i)[0].style.display = "block";
        }
    }
}



