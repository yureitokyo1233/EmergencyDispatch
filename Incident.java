package dispatch;
import java.time.LocalTime;



public class Incident implements Comparable<Incident> {
    public int id;
    public int section;
    public int severity; // 1 = S1, 4 = S4
    public int type; // 1=ALS, 2=BLS, 3=CCT
    public LocalTime arrivalTime;

    public Incident(int id, int section, int severity, int type) {
        this.id = id;
        this.section = section;
        this.severity = severity;
        this.type = type;
        this.arrivalTime = calculateArrivalTime(section);
    }

    private LocalTime calculateArrivalTime(int section) {
        LocalTime baseTime = LocalTime.of(12, 0); // 12:00pm
        int interval;
        switch (section) {
            case 1: interval = 15; break;
            case 2: interval = 17; break;
            case 3: interval = 20; break;
            default: interval = 15; // default interval
        }
        return baseTime.plusMinutes(interval * (section - 1));
    }

    @Override
    public int compareTo(Incident o) {
        return Integer.compare(this.severity, o.severity); // Lower severity = higher priority
    }
}
