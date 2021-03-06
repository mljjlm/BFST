package dk.itu.groupe.parsing.osm;

/**
 *
 * @author Mikael
 */
public enum OSMRoadType
{

    /**
     * OSM: Motorway. KRAK: Highway(1) and Proj_Highway(21).
     */
    MOTORWAY(1, 130, "motorway"),
    /**
     * OSM: Trunk. KRAK: Expressway(2) and Proj_Expressway(22).
     */
    TRUNK(2, 80, "trunk"),
    /**
     * OSM: Primary. KRAK: PrimaryRoute(3) and Proj_PrimaryRoute(23).
     */
    PRIMARY(3, 80, "primary"),
    /**
     * OSM: Secondary. KRAK: SecondaryRoute(4) and Proj_SecondaryRoute(24).
     */
    SECONDARY(4, 80, "secondary"),
    /**
     * OSM: Tertiary. KRAK: Road(5) and Proj_Road(25).
     */
    TERTIARY(5, 50, "tertiary"),
    /**
     * OSM: Unclassified and Byway. KRAK: OtherRoad(6) and Proj_OtherRoad(26).
     */
    UNCLASSIFIED(6, 50, "unclassified", "byway"),
    /**
     * OSM: Residential, Living_Street, Living_Street;Footway, Mini_Roundabout
     * and service. KRAK:.
     */
    RESIDENTIAL(7, 50, "residential", "living_street", "living_street;footway", "mini_roundabout", "service"),
    /**
     * OSM: Motorway_Link. KRAK: HighwayExit(31).
     */
    MOTORWAY_LINK(21, 130, "motorway_link"),
    /**
     * OSM: Trunk_Link. KRAK: ExpresswayExit(32).
     */
    TRUNK_LINK(22, 80, "trunk_link"),
    /**
     * OSM: Primary_Link. KRAK: PrimaryRouteExit(33).
     */
    PRIMARY_LINK(23, 80, "primary_link"),
    /**
     * OSM: Secondary_Link. KRAK: SecondaryRouteExit(34).
     */
    SECONDARY_LINK(24, 80, "secondary_link"),
    /**
     * OSM: Tertiary_Link. KRAK:.
     */
    TERTIARY_LINK(25, 80, "tertiary_link"),
    /**
     * OSM: Pedestrian. KRAK: PedestrianZone(11).
     */
    PEDESTRIAN(8, 30, "pedestrian"),
    /**
     * OSM: Track. KRAK: DirtRoad(10).
     */
    TRACK(9, 80, "track"),
    /**
     * OSM: Road, Yes, Tr, Rfe and Turning_Loop. KRAK: Unknown(0) and
     * AlsoUnknown(95).
     */
    ROAD(10, 80, "road", "yes", "tr", "rfe", "turning_loop"),
    /**
     * OSM: Path and Path;Track. KRAK: Path(8) and Proj_Path(28).
     */
    PATH(11, 30, "path", "path;track"),
    /**
     * OSM: Tunnel. KRAK: HighwayTunnel(41) and ExpresswayTunnel(42).
     */
    TUNNEL(12, 130, "tunnel"),
    /**
     * OSM:. KRAK: ExactLocationUnknown(99).
     */
    PLACES(13, 10, new String[0]),
    /**
     * OSM:. KRAK: Ferry(80).
     */
    FERRY(14, 30, new String[0]);

    private final int type;
    private final String[] types;
    private final int speed;

    private OSMRoadType(int type, int speed, String... types)
    {
        this.type = type;
        this.types = types;
        this.speed = speed;
    }
    
    public int getSpeed()
    {
        return speed;
    }

    public int getTypeNo()
    {
        return type;
    }

    public String[] getOSMTypes()
    {
        return types;
    }
}
