package dispatch;

public class Incident implements Comparable<Incident> {
    public int id;
    public int section;
    public int severity; // 1 = S1, 4 = S4
    public int type; // 1=ALS, 2=BLS, 3=CCT

    public Incident(int id, int section, int severity, int type) {
        this.id = id;
        this.section = section;
        this.severity = severity;
        this.type = type;
    }

    @Override
    public int compareTo(Incident o) {
        return Integer.compare(this.severity, o.severity); // Lower severity = higher priority
    }
}
