import java.util.ArrayList;
import java.util.List;

/** 一条地铁线路：线路名 + 按顺序排列的站点名 */
public class Line {
    private final String name;
    private final List<String> stationNames = new ArrayList<>();

    public Line(String name) { this.name = name; }

    public String getName() { return name; }
    public void addStation(String s) { stationNames.add(s); }
    public List<String> getStationNames() { return stationNames; }
    public int size() { return stationNames.size(); }

    public boolean contains(String s) { return stationNames.contains(s); }

    @Override public String toString() { return name; }
}