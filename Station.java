public class Station {
    private final String name;
    // 经过该站点的所有线路名（如 "2号线"），用 LinkedHashSet 保证顺序且去重
    private final java.util.LinkedHashSet<String> lineNames = new java.util.LinkedHashSet<>();

    public Station(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public void addLine(String lineName) { lineNames.add(lineName); }

    public java.util.Set<String> getLineNames() { return lineNames; }

    public int getLineCount() { return lineNames.size(); }

    /** 是否为中转站：至少有两条线路经过 */
    public boolean isTransferStation() { return lineNames.size() >= 2; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Station)) return false;
        return name.equals(((Station) o).name);
    }
    @Override public int hashCode() { return name.hashCode(); }
    @Override public String toString() { return name; }
}