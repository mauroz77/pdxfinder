package org.pdxfinder.repositories;

import org.neo4j.ogm.cypher.query.PagingAndSorting;
import org.pdxfinder.dao.Marker;
import org.pdxfinder.dao.MarkerAssociation;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

/**
 * Created by csaba on 25/04/2017.
 */
public interface MarkerAssociationRepository extends PagingAndSortingRepository<MarkerAssociation, Long>{

    //TODO: Change cypher
    @Query("MATCH (t:MarkerAssociation) WHERE t.symbol = {symbol} RETURN t")
    MarkerAssociation findByTypeAndMarker(@Param("type") String type, @Param("markerName") String markerName);

}