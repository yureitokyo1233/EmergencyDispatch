package dispatch;
import java.util.*;

public class DispatchSystem {
    public CityMap map = new CityMap();
    public Map<Integer, Unit> units = new HashMap<>();
    public PriorityQueue<Incident> incidents = new PriorityQueue<>();
    private int incidentId = 1;

    public DispatchSystem() {
        units.put(1, new Unit(1, 1));
        units.put(2, new Unit(2, 6));
    }

    public void reportIncident(int section, int severity, int type) {
        incidents.add(new Incident(incidentId++, section, severity, type));
    }

    public Unit dispatchNearest(int section) {
        Set<Integer> availableSections = new HashSet<>();
        for (Unit u : units.values()) if (u.available) availableSections.add(u.section);
        int nearest = map.getNearestSection(section, availableSections);
        for (Unit u : units.values()) {
            if (u.section == nearest && u.available) {
                u.available = false;
                return u;
            }
        }
        return null;
    }

    public List<Incident> getSortedIncidents() {
        List<Incident> list = new ArrayList<>(incidents);
        mergeSort(list, 0, list.size() - 1);
        return list;
    }

    private void mergeSort(List<Incident> arr, int l, int r) {
        if (l < r) {
            int m = (l + r) / 2;
            mergeSort(arr, l, m);
            mergeSort(arr, m + 1, r);
            merge(arr, l, m, r);
        }
    }

    private void merge(List<Incident> arr, int l, int m, int r) {
        List<Incident> left = new ArrayList<>(arr.subList(l, m + 1));
        List<Incident> right = new ArrayList<>(arr.subList(m + 1, r + 1));
        int i = 0, j = 0, k = l;
        while (i < left.size() && j < right.size()) {
            if (left.get(i).compareTo(right.get(j)) <= 0) arr.set(k++, left.get(i++));
            else arr.set(k++, right.get(j++));
        }
        while (i < left.size()) arr.set(k++, left.get(i++));
        while (j < right.size()) arr.set(k++, right.get(j++));
    }
}
