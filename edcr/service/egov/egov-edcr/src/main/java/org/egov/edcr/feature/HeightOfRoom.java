/*
 * eGov  SmartCity eGovernance suite aims to improve the internal efficiency,transparency,
 * accountability and the service delivery of the government  organizations.
 *
 *  Copyright (C) <2019>  eGovernments Foundation
 *
 *  The updated version of eGov suite of products as by eGovernments Foundation
 *  is available at http://www.egovernments.org
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see http://www.gnu.org/licenses/ or
 *  http://www.gnu.org/licenses/gpl.html .
 *
 *  In addition to the terms of the GPL license to be adhered to in using this
 *  program, the following additional terms are to be complied with:
 *
 *      1) All versions of this program, verbatim or modified must carry this
 *         Legal Notice.
 *      Further, all user interfaces, including but not limited to citizen facing interfaces,
 *         Urban Local Bodies interfaces, dashboards, mobile applications, of the program and any
 *         derived works should carry eGovernments Foundation logo on the top right corner.
 *
 *      For the logo, please refer http://egovernments.org/html/logo/egov_logo.png.
 *      For any further queries on attribution, including queries on brand guidelines,
 *         please contact contact@egovernments.org
 *
 *      2) Any misrepresentation of the origin of the material is prohibited. It
 *         is required that all modified versions of this material be marked in
 *         reasonable ways as different from the original version.
 *
 *      3) This license does not grant any rights to any user of the program
 *         with regards to rights under trademark law for use of the trade names
 *         or trademarks of eGovernments Foundation.
 *
 *  In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */

package org.egov.edcr.feature;

import static org.egov.edcr.constants.DxfFileConstants.A;
import static org.egov.edcr.constants.DxfFileConstants.F;
import static org.egov.edcr.constants.DxfFileConstants.G;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.Room;
import org.egov.common.entity.edcr.RoomHeight;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.service.ProcessHelper;
import org.egov.edcr.utility.DcrConstants;
import org.jfree.util.Log;
import org.springframework.stereotype.Service;

@Service
public class HeightOfRoom extends FeatureProcess {

    private static final String RULE = "4.4.4";
    private static final String SUBRULE_41_II_B = "41-ii-b";

    private static final String RULE_AC_DESC = "Minimum height of ac room";
    private static final String RULE_REGULAR_DESC = "Minimum height of regular room";
    private static final String SUBRULE_41_II_B_AREA_DESC = "Total area of rooms";
    private static final String SUBRULE_41_II_B_TOTAL_WIDTH = "Minimum Width of room";

    public static final BigDecimal MINIMUM_HEIGHT_3_6 = BigDecimal.valueOf(3.6);
    public static final BigDecimal MINIMUM_HEIGHT_3 = BigDecimal.valueOf(3);
    public static final BigDecimal MINIMUM_HEIGHT_2_75 = BigDecimal.valueOf(2.75);
    public static final BigDecimal MINIMUM_HEIGHT_2_4 = BigDecimal.valueOf(2.4);
    public static final BigDecimal MINIMUM_AREA_9_5 = BigDecimal.valueOf(9.5);
    public static final BigDecimal MINIMUM_WIDTH_2_4 = BigDecimal.valueOf(2.4);
    public static final BigDecimal MINIMUM_WIDTH_2_1 = BigDecimal.valueOf(2.1);
    public static final BigDecimal MINIMUM_AREA_7_5 = BigDecimal.valueOf(7.5);
    public static final BigDecimal MAXIMUM_AREA_46_45 = BigDecimal.valueOf(46.45);
    private static final String FLOOR = "Floor";
    private static final String ROOM_HEIGHT_NOTDEFINED = "Room height is not defined in layer ";
    private static final String LAYER_ROOM_HEIGHT = "BLK_%s_FLR_%s_%s";
    private static final Logger LOG = LogManager.getLogger(HeightOfRoom.class);

    @Override
    public Plan validate(Plan pl) {
        return pl;
    }

    @Override
    public Plan process(Plan pl) {
        Map<String, Integer> heightOfRoomFeaturesColor = pl.getSubFeatureColorCodesMaster().get("HeightOfRoom");
        validate(pl);
        HashMap<String, String> errors = new HashMap<>();
        if (pl != null && pl.getBlocks() != null) {
            OccupancyTypeHelper mostRestrictiveOccupancy = pl.getVirtualBuilding() != null
                    ? pl.getVirtualBuilding().getMostRestrictiveFarHelper()
                    : null;
            if (mostRestrictiveOccupancy != null && mostRestrictiveOccupancy.getType() != null
                    && (A.equalsIgnoreCase(mostRestrictiveOccupancy.getType().getCode()) ||
                            (G.equalsIgnoreCase(mostRestrictiveOccupancy.getType().getCode())
                                    || F.equalsIgnoreCase(mostRestrictiveOccupancy.getType().getCode())))) {
                for (Block block : pl.getBlocks()) {
                    if (block.getBuilding() != null && !block.getBuilding().getFloors().isEmpty()) {
                        scrutinyDetail = new ScrutinyDetail();
                        scrutinyDetail.addColumnHeading(1, RULE_NO);
                        scrutinyDetail.addColumnHeading(2, DESCRIPTION);
                        scrutinyDetail.addColumnHeading(3, FLOOR);
                        scrutinyDetail.addColumnHeading(4, REQUIRED);
                        scrutinyDetail.addColumnHeading(5, PROVIDED);
                        scrutinyDetail.addColumnHeading(6, STATUS);

                        scrutinyDetail.setKey("Block_" + block.getNumber() + "_" + "Room");
                        
                        ScrutinyDetail scrutinyDetail1 = new ScrutinyDetail();
                        scrutinyDetail.addColumnHeading(1, RULE_NO);
                        scrutinyDetail.addColumnHeading(2, DESCRIPTION);
                        scrutinyDetail.addColumnHeading(3, FLOOR);
                        scrutinyDetail.addColumnHeading(4, REQUIRED);
                        scrutinyDetail.addColumnHeading(5, PROVIDED);
                        scrutinyDetail.addColumnHeading(6, STATUS);

                        scrutinyDetail.setKey("Block_" + block.getNumber() + "_" + " Room Area");


                        for (Floor floor : block.getBuilding().getFloors()) {
                            List<BigDecimal> roomAreas = new ArrayList<>();
                            List<BigDecimal> roomWidths = new ArrayList<>();
                            BigDecimal minimumHeight = BigDecimal.ZERO;
                            BigDecimal totalArea = BigDecimal.ZERO;
                            BigDecimal minWidth = BigDecimal.ZERO;
                            BigDecimal maxArea = BigDecimal.ZERO;
                            String subRule = null;
                            String subRuleDesc = null;
                            String color = "";

                            if (A.equalsIgnoreCase(mostRestrictiveOccupancy.getType().getCode()))
                                color = DxfFileConstants.COLOR_RESIDENTIAL_ROOM;
                            else if(F.equalsIgnoreCase(mostRestrictiveOccupancy.getType().getCode()))
                                color = DxfFileConstants.COLOR_COMMERCIAL_ROOM;
                            else if(G.equalsIgnoreCase(mostRestrictiveOccupancy.getType().getCode()))
                                color = DxfFileConstants.COLOR_INDUSTRIAL_ROOM;

                            if (floor.getAcRooms() != null && floor.getAcRooms().size()>0) {
                                List<BigDecimal> residentialAcRoomHeights = new ArrayList<>();
                                
                                List<RoomHeight> acHeights  = new ArrayList<>();
                                List<Measurement> acRooms = new ArrayList<>();

                            	 for( Room  room: floor.getAcRooms())
								{
									if (room.getHeights() != null)
										acHeights.addAll(room.getHeights());
									if (room.getRooms() != null)
										acRooms.addAll(room.getRooms());
								}
                            	
                                for (RoomHeight roomHeight : acHeights) {
                                    if (heightOfRoomFeaturesColor.get(color) == roomHeight.getColorCode()) {
                                        residentialAcRoomHeights.add(roomHeight.getHeight());
                                    }
                                }

                                for (Measurement acRoom : acRooms) {
                                    if (heightOfRoomFeaturesColor.get(color) == acRoom.getColorCode()) {
                                        roomAreas.add(acRoom.getArea());
                                        roomWidths.add(acRoom.getWidth());
                                    }
                                }

                                if (!residentialAcRoomHeights.isEmpty()) {
                                    BigDecimal minHeight = residentialAcRoomHeights.stream().reduce(BigDecimal::min).get();
                                    // Added by Bimal to check minimum height for residential rooms only
                                    if (!A.equalsIgnoreCase(mostRestrictiveOccupancy.getType().getCode())) {
                                    	minimumHeight = MINIMUM_HEIGHT_2_75;
                                        Log.info("Minimum Residential AC Room Height required is set to : "+MINIMUM_HEIGHT_2_75);
                                    }
                                    else
                                        minimumHeight = MINIMUM_HEIGHT_3;
                                    // Comented by Bimal to check minimum height for residential rooms only 
//                                    if (!G.equalsIgnoreCase(mostRestrictiveOccupancy.getType().getCode()))
//                                        minimumHeight = MINIMUM_HEIGHT_2_4;
//                                    else
//                                        minimumHeight = MINIMUM_HEIGHT_3;

                                    subRule = RULE;
                                    subRuleDesc = RULE_AC_DESC;
                                    
                                    
                                    boolean valid = false;
                                    boolean isTypicalRepititiveFloor = false;
                                    Map<String, Object> typicalFloorValues = ProcessHelper.getTypicalFloorValues(block, floor,
                                            isTypicalRepititiveFloor);
                                    buildResult(pl, floor, minimumHeight, subRule, subRuleDesc, minHeight, valid,
                                            typicalFloorValues);
                                } else {
                                    String layerName = String.format(LAYER_ROOM_HEIGHT, block.getNumber(), floor.getNumber(),
                                            "AC_ROOM");
                                    errors.put(layerName, ROOM_HEIGHT_NOTDEFINED + layerName);
                                    pl.addErrors(errors);
                                }

                            }

                            if (floor.getRegularRooms() != null  && floor.getRegularRooms().size()>0) {
                            	
								List<BigDecimal> residentialRoomHeights = new ArrayList<>();

								List<RoomHeight> heights = new ArrayList<>();
								List<Measurement> rooms = new ArrayList<>();

								 //Commented by Bimal on 27 March, 2024
								for (Room room : floor.getRegularRooms()) {
									if (room.getHeights() != null)
										heights.addAll(room.getHeights());
									if (room.getRooms() != null)
										rooms.addAll(room.getRooms());
								}
								 
								//Added by Bimal on 27 March, 2024
								/*
								 * for (Room room : floor.getRegularRooms()) { if (room.getRooms() != null) {
								 * //List<Measurement> measurement = room.getRooms();
								 * //measurement.get(0).getHeight();
								 * 
								 * List<Measurement> measurements = room.getRooms(); for (Measurement
								 * measurement : measurements) { System.out.println("Room Heights_"
								 * +floor.getNumber()+" " +measurement.getHeight()); // Log room heights
								 * residentialRoomHeights.add(measurement.getHeight()); } }
								 * 
								 * if (room.getRooms() != null) rooms.addAll(room.getRooms()); }
								 */

								for (RoomHeight roomHeight : heights) {
									if (heightOfRoomFeaturesColor.get(color) == roomHeight.getColorCode()) {
										residentialRoomHeights.add(roomHeight.getHeight());
									}
								}

								for (Measurement room : rooms) {
									if (heightOfRoomFeaturesColor.get(color) == room.getColorCode()) {
										roomAreas.add(room.getArea());
										roomWidths.add(room.getWidth());
									}
								}

                                if (!residentialRoomHeights.isEmpty()) {
                                    BigDecimal minHeight = residentialRoomHeights.stream().reduce(BigDecimal::min).get();
                                 // Added by Bimal to check minimum height for residential rooms only
                                    if (A.equalsIgnoreCase(mostRestrictiveOccupancy.getType().getCode())) {
                                    	 minimumHeight = MINIMUM_HEIGHT_2_75;
                                     	Log.info("Minimum Residential Regular Room Height required is set to : "+MINIMUM_HEIGHT_2_75);
                                    }
                                       
                                    else
                                        minimumHeight = MINIMUM_HEIGHT_3; 
                                 // Comented by Bimal to check minimum height for residential rooms only
//                                    if (!G.equalsIgnoreCase(mostRestrictiveOccupancy.getType().getCode()))
//                                        minimumHeight = MINIMUM_HEIGHT_2_75;
//                                    else
//                                        minimumHeight = MINIMUM_HEIGHT_3_6;

                                    subRule = RULE;
                                    subRuleDesc = RULE_REGULAR_DESC;


                                    boolean valid = false;
                                    boolean isTypicalRepititiveFloor = false;
                                    Map<String, Object> typicalFloorValues = ProcessHelper.getTypicalFloorValues(block, floor,
                                            isTypicalRepititiveFloor);
                                    buildResult(pl, floor, minimumHeight, subRule, subRuleDesc, minHeight, valid,
                                            typicalFloorValues);
                                } else {
                                    String layerName = String.format(LAYER_ROOM_HEIGHT, block.getNumber(), floor.getNumber(),
                                            "REGULAR_ROOM");
                                    errors.put(layerName, ROOM_HEIGHT_NOTDEFINED + layerName);
                                    pl.addErrors(errors);
                                }

                            }

                            if (!roomAreas.isEmpty()) {
                                totalArea = roomAreas.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                                BigDecimal minRoomWidth = roomWidths.stream().reduce(BigDecimal::min).get();
//                                if (roomAreas.size() == 1) {
//                                    minimumHeight = MINIMUM_AREA_9_5;
//                                    minWidth = MINIMUM_WIDTH_2_4;
//                                }
//
//                                else if (roomAreas.size() == 2) {
//                                    minimumHeight = MINIMUM_AREA_9_5;
//                                    minWidth = MINIMUM_WIDTH_2_1;
//                                }
//                                subRule = SUBRULE_41_II_B;
//                                subRuleDesc = SUBRULE_41_II_B_AREA_DESC;
//
//                                boolean valid = false;
//                                boolean isTypicalRepititiveFloor = false;
//                                Map<String, Object> typicalFloorValues = ProcessHelper.getTypicalFloorValues(block, floor,
//                                        isTypicalRepititiveFloor);
//                                buildResult(pl, floor, minimumHeight, subRule, subRuleDesc, totalArea, valid, typicalFloorValues);
//
//                                subRuleDesc = SUBRULE_41_II_B_TOTAL_WIDTH;
//                                buildResult(pl, floor, minWidth, subRule, subRuleDesc, minRoomWidth, valid, typicalFloorValues);
                                if (roomAreas.size() == 1) {
                                    minimumHeight = MINIMUM_AREA_9_5;
                                    minWidth = MINIMUM_WIDTH_2_4;
                                    maxArea = MAXIMUM_AREA_46_45;
                                    
                                    
                                }

                                else if (roomAreas.size() > 1) {
                                	
                                    minimumHeight = MINIMUM_AREA_9_5;
                                    minWidth = MINIMUM_WIDTH_2_4;
                                    maxArea = MAXIMUM_AREA_46_45;
                                  
                                    if (roomAreas.get(0).compareTo(maxArea) <= 0 && roomAreas.get(0).compareTo(minimumHeight) >= 0 && roomWidths.get(0).compareTo(minWidth) >= 0) {
                                    setReportOutputDetails(pl, subRule, subRuleDesc,
											floor.getNumber().toString(),
											"Area >= " + minimumHeight + ", Width >=" + minWidth,
											"Area = " + roomAreas.get(0) + ", Width = " + roomWidths.get(0)
													+ DcrConstants.IN_METER,
											Result.Accepted.getResultVal(), scrutinyDetail1);
                                    }else {
                                    	setReportOutputDetails(pl, subRule, subRuleDesc,
    											floor.getNumber().toString(),
    											"Area >= " + minimumHeight + ", Width >=" + minWidth,
    											"Area = " + roomAreas.get(0) + ", Width = " + roomWidths.get(0)
    													+ DcrConstants.IN_METER,
    											Result.Not_Accepted.getResultVal(), scrutinyDetail1);
                                    }
                                    
                                    }
                                    for (int i = 1; i < roomAreas.size(); i++) {
                                    	
                                        minimumHeight = MINIMUM_AREA_7_5; //minimumArea
                                        minWidth = MINIMUM_WIDTH_2_1;
                                        maxArea = MAXIMUM_AREA_46_45;
                                        
                                        if (roomAreas.get(i).compareTo(maxArea) <= 0 && roomAreas.get(i).compareTo(minimumHeight) >= 0 && roomWidths.get(i).compareTo(minWidth) >= 0) {
                                        setReportOutputDetails(pl, subRule, subRuleDesc,
    											floor.getNumber().toString(),
    											"Area >= " + minimumHeight + ", Width >=" + minWidth,
    											"Area = " + roomAreas.get(i) + ", Width = " + roomWidths.get(i)
    													+ DcrConstants.IN_METER,
    											Result.Accepted.getResultVal(), scrutinyDetail1);
                                        }else {
                                        	setReportOutputDetails(pl, subRule, subRuleDesc,
        											floor.getNumber().toString(),
        											"Area >= " + minimumHeight + ", Width >=" + minWidth,
        											"Area = " + roomAreas.get(i) + ", Width = " + roomWidths.get(i)
        													+ DcrConstants.IN_METER,
        											Result.Not_Accepted.getResultVal(), scrutinyDetail1);
                                        }
                                        
                                    
                                    
                                    
                                    }
                            }
                        }
                    }
                }
            }
        }
        return pl;

    }

    private void buildResult(Plan pl, Floor floor, BigDecimal expected, String subRule, String subRuleDesc,
            BigDecimal actual, boolean valid, Map<String, Object> typicalFloorValues) {
        if (!(Boolean) typicalFloorValues.get("isTypicalRepititiveFloor")
                && expected.compareTo(BigDecimal.valueOf(0)) > 0 &&
                subRule != null && subRuleDesc != null) {
            if (actual.compareTo(expected) >= 0) {
                valid = true;
            }
            String value = typicalFloorValues.get("typicalFloors") != null
                    ? (String) typicalFloorValues.get("typicalFloors")
                    : " floor " + floor.getNumber();
            if (valid) {
                setReportOutputDetails(pl, subRule, subRuleDesc, value,
                        expected + DcrConstants.IN_METER,
                        actual + DcrConstants.IN_METER, Result.Accepted.getResultVal(), scrutinyDetail);
                LOG.info("Room Height Validation True: (Expected/Actual) " +expected+"/"+actual);
            } else {
                setReportOutputDetails(pl, subRule, subRuleDesc, value,
                        expected + DcrConstants.IN_METER,
                        actual + DcrConstants.IN_METER, Result.Not_Accepted.getResultVal(), scrutinyDetail);
                LOG.info("Room Height Validation False: (Expected/Actual) " +expected+"/"+actual);
            }
        }
    }

    private void setReportOutputDetails(Plan pl, String ruleNo, String ruleDesc, String floor, String expected, String actual,
            String status, ScrutinyDetail scrutinyDetail) {
        Map<String, String> details = new HashMap<>();
        details.put(RULE_NO, ruleNo);
        details.put(DESCRIPTION, ruleDesc);
        details.put(FLOOR, floor);
        details.put(REQUIRED, expected);
        details.put(PROVIDED, actual);
        details.put(STATUS, status);
        scrutinyDetail.getDetail().add(details);
        pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
    }

    @Override
    public Map<String, Date> getAmendments() {
        return new LinkedHashMap<>();
    }
}