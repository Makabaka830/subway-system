import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 武汉地铁模拟系统：把 subway.txt 读入并构建为一张带权无向图，
 * 站点为节点，两站之间的连接为带权边（权重为里程 KM）。
 * 提供线路查询（中转站、范围站点、所有路径、最短路径、路径打印）
 * 与计价（单程票、武汉通、日票）等功能。
 */
public class Subway {

    /** 站点注册表：站名 -> Station 对象（保证全局唯一） */
    private final Map<String, Station> stations = new LinkedHashMap<>();
    /** 邻接表：站名 -> 与之直接相连的边集合 */
    private final Map<String, List<Edge>> adjacency = new HashMap<>();
    /** 所有线路，按出现顺序 */
    private final List<Line> lines = new ArrayList<>();

    /** 站名之间的分隔符：可能是 "---" 或全角破折号 "—" */
    private static final Pattern EDGE =
            Pattern.compile("^\\s*(.+?)(?:---|\u2014)(.+?)\\s+([0-9]+(?:\\.[0-9]+)?)\\s*$");
    /** 线路区块标题，如 "2号线站点间距" */
    private static final Pattern SECTION = Pattern.compile("^(\\d+号线)站点间距");

    /** 从 subway.txt 载入整个地铁网 */
    public void loadFromFile(String path) throws IOException {
        String currentLine = null;          // 当前所属线路名
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String raw;
            while ((raw = br.readLine()) != null) {
                String s = raw.trim();
                if (s.isEmpty()) continue;
                Matcher sec = SECTION.matcher(s);
                if (sec.find()) {
                    currentLine = sec.group(1);
                    boolean exists = false;
                    for (Line l : lines) if (l.getName().equals(currentLine)) exists = true;
                    if (!exists) lines.add(new Line(currentLine));
                    continue;
                }
                if (s.startsWith("\u7ad9\u70b9\u540d\u79f0")) continue;   // 跳过表头"站点名称"
                if (currentLine == null) continue;
                Matcher m = EDGE.matcher(s);
                if (!m.matches()) continue;
                String a = m.group(1).trim();
                String b = m.group(2).trim();
                double km = Double.parseDouble(m.group(3));
                addEdge(a, b, km, currentLine);
            }
        }
    }

    /** 加入一条无向边，并把该线路登记到两端站点上 */
    private void addEdge(String a, String b, double km, String lineName) {
        Station sa = getOrCreate(a);
        Station sb = getOrCreate(b);
        sa.addLine(lineName);
        sb.addLine(lineName);
        adjacency.computeIfAbsent(a, k -> new ArrayList<>()).add(new Edge(sb, km, lineName));
        adjacency.computeIfAbsent(b, k -> new ArrayList<>()).add(new Edge(sa, km, lineName));
    }

    private Station getOrCreate(String name) {
        return stations.computeIfAbsent(name, Station::new);
    }

    public Station getStation(String name) { return stations.get(name); }
    public int getStationCount() { return stations.size(); }
    public List<Line> getLines() { return lines; }

    /** 某条线路拥有的站点数 */
    public int lineSize(String lineName) {
        for (Line l : lines) if (l.getName().equals(lineName)) return l.size();
        throw new IllegalArgumentException("\u4e0d\u5b58\u5728\u7684\u7ebf\u8def: " + lineName);
    }

    /** 某站点拥有的线路数 */
    public int stationLineCount(String name) {
        Station s = stations.get(name);
        if (s == null) throw new IllegalArgumentException("\u4e0d\u5b58\u5728\u7684\u7ad9\u70b9: " + name);
        return s.getLineCount();
    }

    // ---------------------------------------------------------------
    // 功能1：识别所有中转站（>=2 条线经过）
    // ---------------------------------------------------------------
    /** 返回所有中转站，每条记录形如 "中南路 : [2号线, 4号线]" */
    public List<String> getAllTransferStations() {
        List<String> result = new ArrayList<>();
        for (Station s : stations.values()) {
            if (s.isTransferStation()) {
                result.add(s.getName() + " : " + s.getLineNames());
            }
        }
        return result;
    }

    // ---------------------------------------------------------------
    // 功能2：距离某站点线路距离小于(等于) n 的所有站点
    // ---------------------------------------------------------------
    /** 一条结果记录：站点名 + 所在线路 + 到给定站点的距离 */
    public static class DistanceRecord {
        public final String station;
        public final java.util.Set<String> lines;
        public final double distance;
        public DistanceRecord(String station, java.util.Set<String> lines, double distance) {
            this.station = station; this.lines = lines; this.distance = distance;
        }
        @Override public String toString() {
            return "<" + station + ", " + lines + ", " + round(distance) + ">";
        }
    }

    /**
     * 输入站点名与距离阈值 n，输出与给定站点乘车距离 <= n 的所有站点。
     * @throws IllegalArgumentException 站点不存在或 n 非法时
     */
    public List<DistanceRecord> getStationsWithin(String from, double n) {
        Station start = stations.get(from);
        if (start == null) throw new IllegalArgumentException("\u4e0d\u5b58\u5728\u7684\u7ad9\u70b9: " + from);
        if (n <= 0) throw new IllegalArgumentException("\u8ddd\u79bb\u9608\u503c\u5fc5\u987b\u4e3a\u6b63\u6570: " + n);
        // 以 from 为源做 Dijkstra，得到到各站点的最短乘车距离
        Map<String, Double> dist = dijkstraDistance(start);
        List<DistanceRecord> result = new ArrayList<>();
        for (Station s : stations.values()) {
            if (s.equals(start)) continue;
            Double d = dist.get(s.getName());
            if (d != null && d <= n) {
                result.add(new DistanceRecord(s.getName(), s.getLineNames(), d));
            }
        }
        result.sort(Comparator.comparingDouble(r -> r.distance));
        return result;
    }

    // ---------------------------------------------------------------
    // 功能3：返回连接起点与终点的所有路径（不含环路）
    // ---------------------------------------------------------------
    public List<List<Station>> getAllPaths(String from, String to) {
        Station s = stations.get(from);
        Station t = stations.get(to);
        if (s == null) throw new IllegalArgumentException("\u4e0d\u5b58\u5728\u7684\u8d77\u59cb\u7ad9: " + from);
        if (t == null) throw new IllegalArgumentException("\u4e0d\u5b58\u5728\u7684\u7ec8\u6b62\u7ad9: " + to);
        List<List<Station>> result = new ArrayList<>();
        List<Station> path = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        dfsAllPaths(s, t, path, visited, result);
        return result;
    }

    private void dfsAllPaths(Station cur, Station target, List<Station> path,
                             Set<String> visited, List<List<Station>> result) {
        path.add(cur);
        visited.add(cur.getName());
        if (cur.equals(target)) {
            result.add(new ArrayList<>(path));
        } else {
            for (Edge e : adjacency.getOrDefault(cur.getName(), Collections.emptyList())) {
                if (!visited.contains(e.to.getName())) {
                    dfsAllPaths(e.to, target, path, visited, result);
                }
            }
        }
        path.remove(path.size() - 1);
        visited.remove(cur.getName());
    }

    // ---------------------------------------------------------------
    // 功能4：最短路径（按里程，Dijkstra）
    // ---------------------------------------------------------------
    /** 返回从 start 到 stop 的按里程最短路径（含起终点）；不可达返回空列表 */
    public List<Station> getShortestPath(Station start, Station stop) {
        if (start == null || stop == null)
            throw new IllegalArgumentException("\u8d77\u70b9\u6216\u7ec8\u70b9\u4e3a\u7a7a");
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<Station> pq = new PriorityQueue<>(
                Comparator.comparingDouble(a -> dist.getOrDefault(a.getName(), Double.MAX_VALUE)));
        for (Station s : stations.values()) dist.put(s.getName(), Double.MAX_VALUE);
        dist.put(start.getName(), 0.0);
        pq.add(start);
        Set<String> settled = new HashSet<>();
        while (!pq.isEmpty()) {
            Station u = pq.poll();
            if (settled.contains(u.getName())) continue;
            settled.add(u.getName());
            for (Edge e : adjacency.getOrDefault(u.getName(), Collections.emptyList())) {
                double nd = dist.get(u.getName()) + e.distance;
                if (nd < dist.getOrDefault(e.to.getName(), Double.MAX_VALUE) - 1e-9) {
                    dist.put(e.to.getName(), nd);
                    prev.put(e.to.getName(), u.getName());
                    pq.add(e.to);
                }
            }
        }
        if (dist.getOrDefault(stop.getName(), Double.MAX_VALUE) == Double.MAX_VALUE)
            return new ArrayList<>();           // 不可达
        // 回溯路径（先逆序压栈，再反转）
        List<String> reversed = new ArrayList<>();
        String cur = stop.getName();
        while (cur != null) {
            reversed.add(cur);
            if (cur.equals(start.getName())) break;
            cur = prev.get(cur);
        }
        Collections.reverse(reversed);
        List<Station> path = new ArrayList<>();
        for (String name : reversed) path.add(stations.get(name));
        return path;
    }

    // ---------------------------------------------------------------
    // 功能5：以乘车方式直观打印路径
    // ---------------------------------------------------------------
    public void printPath(List<Station> path) {
        System.out.println(formatPath(path));
    }

    /**
     * 把路径切成若干乘车段：首段以"乘"开头，后续段以"换乘"衔接。
     * 例：乘2号线[宝通寺，中南路]，换乘4号线[中南路，武昌火车站]
     */
    public String formatPath(List<Station> path) {
        if (path == null || path.size() < 2) return "\u65e0\u6709\u6548\u8def\u5f84";
        List<String> segs = new ArrayList<>();
        int i = 0;
        while (i < path.size() - 1) {
            String line = commonLine(path.get(i).getName(), path.get(i + 1).getName());
            int j = i + 1;
            while (j < path.size() - 1) {
                String nl = commonLine(path.get(j).getName(), path.get(j + 1).getName());
                if (line != null && line.equals(nl)) j++;
                else break;
            }
            // 段内不含"乘"，由分隔符"换乘"或开头"乘"提供衔接词
            segs.add(line + "[" + path.get(i).getName() + "\uff0c"
                    + path.get(j).getName() + "]");
            i = j;
        }
        return "\u4e58" + String.join("\uff0c\u6362\u4e58", segs);
    }

    /** 找同时包含相邻两站的线路名（用于判定乘车段所在线路） */
    private String commonLine(String a, String b) {
        for (Edge e : adjacency.getOrDefault(a, Collections.emptyList())) {
            if (e.to.getName().equals(b)) return e.lineName;
        }
        return null;
    }

    // ---------------------------------------------------------------
    // 功能6/7：计价
    // ---------------------------------------------------------------
    /** 一条路径的总里程（KM） */
    public double pathDistance(List<Station> path) {
        double total = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            total += edgeDistance(path.get(i).getName(), path.get(i + 1).getName());
        }
        return total;
    }

    private double edgeDistance(String a, String b) {
        for (Edge e : adjacency.getOrDefault(a, Collections.emptyList())) {
            if (e.to.getName().equals(b)) return e.distance;
        }
        throw new IllegalArgumentException("\u4e24\u7ad9\u4e0d\u76f8\u90bb: " + a + " - " + b);
    }

    /** 单程票票价：按里程限时分段计价（武汉地铁现行标准） */
    public static double ticketPrice(double km) {
        if (km <= 2)  return 2;
        if (km <= 4)  return 3;
        if (km <= 6)  return 4;
        if (km <= 8)  return 5;
        if (km <= 12) return 6;
        if (km <= 18) return 7;
        if (km <= 24) return 8;
        if (km <= 32) return 9;
        if (km <= 40) return 10;
        if (km <= 50) return 11;
        if (km <= 60) return 12;
        return 12 + Math.ceil((km - 60) / 20.0);   // 超过60km，每增加20km加1元
    }

    /** 普通单程票价格（按给定路径里程） */
    public double priceOfPath(List<Station> path) {
        return ticketPrice(pathDistance(path));
    }

    /** 武汉通刷卡价格：单程票 9 折 */
    public static double wuhanTongPrice(double km) {
        return round(ticketPrice(km) * 0.9);
    }

    /** 日票持有者乘车费用：有效期内不限次，单次记 0 元 */
    public static double dayPassPrice(double km) { return 0; }

    // ---------------------------------------------------------------
    // Dijkstra 距离表（不存路径，只存距离）
    // ---------------------------------------------------------------
    private Map<String, Double> dijkstraDistance(Station start) {
        Map<String, Double> dist = new HashMap<>();
        for (Station s : stations.values()) dist.put(s.getName(), Double.MAX_VALUE);
        dist.put(start.getName(), 0.0);
        PriorityQueue<Station> pq = new PriorityQueue<>(
                Comparator.comparingDouble(a -> dist.getOrDefault(a.getName(), Double.MAX_VALUE)));
        pq.add(start);
        Set<String> settled = new HashSet<>();
        while (!pq.isEmpty()) {
            Station u = pq.poll();
            if (settled.contains(u.getName())) continue;
            settled.add(u.getName());
            for (Edge e : adjacency.getOrDefault(u.getName(), Collections.emptyList())) {
                double nd = dist.get(u.getName()) + e.distance;
                if (nd < dist.getOrDefault(e.to.getName(), Double.MAX_VALUE) - 1e-9) {
                    dist.put(e.to.getName(), nd);
                    pq.add(e.to);
                }
            }
        }
        return dist;
    }

    public static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}