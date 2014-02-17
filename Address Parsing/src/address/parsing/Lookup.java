
package address.parsing;

/**
 *
 * @author Peter Bindslev <plil@itu.dk>, Rune Henriksen <ruju@itu.dk> & Mikael
 * Jepsen <mlin@itu.dk>
 */
public enum Lookup
{
    ROAD("roadnames", "roadname"), POSTCODE("cities", "postcode"), CITY("cities", "city");
    
    private final String tableName, columnName;
    
    private Lookup(String tableName, String columnName) {
        this.tableName = tableName;
        this.columnName = columnName;
    }
    
    public String getTable()
    {
        return tableName;
    }
    
    public String getColumn()
    {
        return columnName;
    }
}
