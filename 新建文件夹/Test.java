import java.util.List;

/**
 * 测试类：演示地铁模拟系统的各项功能。
 * 请在 subway.txt 所在目录运行本类（VSCode 直接运行 main 即可）。
 */
public class Test {
    public static void main(String[] args) {
        Subway subway = new Subway();
        try {
            // 依次尝试常见路径，便于在 VSCode 不同工作目录下运行
            String[] candidates = {"subway.txt", "data/subway.txt", "../subway.txt"};
            java.io.File file = null;
            for (String c : candidates) {
                java.io.File f = new java.io.File(c);
                if (f.exists()) { file = f; break; }
            }
            if (file == null) {
                System.err.println("\u672a\u627e\u5230 subway.txt\uff0c\u8bf7\u5c06\u5176\u653e\u5728\u9879\u76ee\u6839\u76ee\u5f55");
                return;
            }
            subway.loadFromFile(file.getPath());
        } catch (Exception e) {
            System.err.println("\u8bfb\u53d6 subway.txt \u51fa\u9519: " + e.getMessage());
            return;
        }

        System.out.println("======== \u6b66\u6c49\u5730\u94c1\u6a21\u62df\u7cfb\u7edf ========");
        System.out.println("\u5171\u8bfb\u5165\u7ebf\u8def " + subway.getLines().size() + " \u6761\uff0c\u8f66\u7ad9 "
                + subway.getStationCount() + " \u4e2a\u3002");

        // ---- 功能1：所有中转站 ----
        System.out.println("\n[1] \u4e2d\u8f6c\u7ad9\uff08>=2\u6761\u7ebf\u7ecf\u8fc7\uff09\uff1a");
        for (String t : subway.getAllTransferStations()) System.out.println("    " + t);

        // ---- 功能2：范围内站点 ----
        System.out.println("\n[2] \u4e0e\u300c\u534e\u4e2d\u79d1\u6280\u5927\u5b66\u300d\u8ddd\u79bb<=1km \u7684\u7ad9\u70b9\uff1a");
        for (Subway.DistanceRecord r : subway.getStationsWithin("\u534e\u4e2d\u79d1\u6280\u5927\u5b66", 1))
            System.out.println("    " + r);

        // ---- 功能3：所有路径 ----
        System.out.println("\n[3] \u5929\u6cb3\u673a\u573a -> \u6c38\u660c\u706b\u8f66\u7ad9 \u7684\u6240\u6709\u8def\u5f84\u6570\uff1a");
        List<List<Station>> all = subway.getAllPaths("\u5929\u6cb3\u673a\u573a", "\u6b66\u660c\u706b\u8f66\u7ad9");
        System.out.println("    \u5171 " + all.size() + " \u6761\u4e0d\u91cd\u590d\u8def\u5f84");
        if (!all.isEmpty()) {
            List<Station> p0 = all.get(0);
            System.out.println("    \u7b2c1\u6761\u8def\u5f84\u7ad9\u6570=" + p0.size());
        }

        // ---- 功能4：最短路径 ----
        System.out.println("\n[4] \u5929\u6cb3\u673a\u573a -> \u6b66\u660c\u706b\u8f66\u7ad9 \u7684\u6700\u77ed\u8def\u5f84\uff1a");
        Station s = subway.getStation("\u5929\u6cb3\u673a\u573a");
        Station e = subway.getStation("\u6b66\u660c\u706b\u8f66\u7ad9");
        List<Station> sp = subway.getShortestPath(s, e);
        StringBuilder sb = new StringBuilder("    ");
        for (Station st : sp) sb.append(st.getName()).append(" \u2192 ");
        System.out.println(sb + "  (\u91cc\u7a0b " + Subway.round(subway.pathDistance(sp)) + " km)");

        // ---- 功能5：打印乘车方式 ----
        System.out.println("[5] \u4e58\u8f66\u65b9\u5f0f\uff1a");
        System.out.print("    "); subway.printPath(sp);

        // 题目示例：宝通寺 -> 武昌火车站
        System.out.println("    \u793a\u4f8b\uff1a\u5b9d\u901a\u5bfa->\u6b66\u660c\u706b\u8f66\u7ad9\uff1a");
        System.out.print("    "); subway.printPath(subway.getShortestPath(
                subway.getStation("\u5b9d\u901a\u5bfa"), subway.getStation("\u6b66\u660c\u706b\u8f66\u7ad9")));

        // ---- 功能6：单程票价格 ----
        double km = subway.pathDistance(sp);
        System.out.println("[6] \u8be5\u8def\u5f84\u5355\u7a0b\u7968\u7968\u4ef7\uff1a " + subway.priceOfPath(sp) + " \u5143 (\u91cc\u7a0b "
                + Subway.round(km) + " km)");

        // ---- 功能7：武汉通 / 日票 ----
        System.out.println("[7] \u6b66\u6c49\u901a(9\u6298)\uff1a " + Subway.wuhanTongPrice(km)
                + " \u5143\uff1b\u65e5\u7968\u6301\u6709\u8005\u5355\u6b21\u8f66\u8d39\uff1a " + Subway.dayPassPrice(km) + " \u5143");

        // ---- 异常处理演示 ----
        System.out.println("\n[\u5f02\u5e38\u5904\u7406\u6f14\u793a]");
        try { subway.getStationsWithin("\u4e0d\u5b58\u5728\u7684\u7ad9", 1); }
        catch (IllegalArgumentException ex) { System.out.println("    \u6293\u83b7\u5230: " + ex.getMessage()); }
        try { subway.getShortestPath(subway.getStation("\u5929\u6cb3\u673a\u573a"), null); }
        catch (IllegalArgumentException ex) { System.out.println("    \u6293\u83b7\u5230: " + ex.getMessage()); }
    }
}