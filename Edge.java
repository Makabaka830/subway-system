/** 地铁图中的一条边：连接 to 站点，两站间距离（KM），所在线路名 */
public class Edge {
    public final Station to;
    public final double distance;
    public final String lineName;

    public Edge(Station to, double distance, String lineName) {
        this.to = to;
        this.distance = distance;
        this.lineName = lineName;
    }
}