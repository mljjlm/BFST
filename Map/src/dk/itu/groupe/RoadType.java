package dk.itu.groupe;

/**
 *
 * @author Peter Bindslev <plil@itu.dk>, Rune Henriksen <ruju@itu.dk> & Mikael
 * Jepsen <mlin@itu.dk>
 */
public enum RoadType
{

    UNKNOWN(0),
    HIGHWAY(1),
    EXPRESSWAY(2),
    PRIMARY_ROUTE(3),
    SECONDARY_ROUTE(4),
    ROAD(5),
    OTHER_ROAD(6),
    PATH(8),
    DIRT_ROAD(10),
    PEDESTRIAN_ZONE(11),
    PROJ_HIGHWAY(21),
    PROJ_EXPRESSWAY(22),
    PROJ_PRIMARY_ROUTE(23),
    PROJ_SECONDARY_ROUTE(24),
    PROJ_ROAD(25),
    PROJ_OTHER_ROAD(26),
    PROJ_PATH(28),
    HIGHWAY_EXIT(31),
    EXPRESSWAY_EXIT(32),
    PRIMARY_ROUTE_EXIT(33),
    SECOUNDARY_ROUTE_EXIT(34),
    OTHER_EXIT(35),
    HIGHWAY_TUNNEL(41),
    EXPRESSWAY_TUNNEL(42),
    PRIMARY_ROUTE_TUNNEL(43),
    SECONDARY_ROUTE_TUNNEL(44),
    OTHER_ROAD_TUNNEL(45),
    SMALL_ROAD_TUNNEL(46),
    PATH_TUNNEL(48),
    FERRY(80),
    ALSO_UNKNOWN(95),
    EXACT_LOCATION_UNKNOWN(99);

    private final int typeNumber;

    private RoadType(int typeNumber)
    {
        this.typeNumber = typeNumber;
    }

    public int getTypeNumber()
    {
        return typeNumber;
    }
}
