package dispatch;
import java.util.*;

public class CityMap {
    private Map<Integer, List<Integer>> adjList = new HashMap<>();

    public CityMap() {
        for (int i = 1; i <= 6; i++) adjList.put(i, new ArrayList<>());
        // Example connections (customize as needed)
        adjList.get(1).add(2); adjList.get(2).add(1);
        adjList.get(2).add(3); adjList.get(3).add(2);
        adjList.get(3).add(4); adjList.get(4).add(3);
        adjList.get(4).add(5); adjList.get(5).add(4);
        adjList.get(5).add(6); adjList.get(6).add(5);
    }

    public int getNearestSection(int from, Set<Integer> targets) {
        Queue<Integer> q = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        q.add(from); visited.add(from);
        while (!q.isEmpty()) {
            int curr = q.poll();
            if (targets.contains(curr)) return curr;
            for (int next : adjList.get(curr)) {
                if (!visited.contains(next)) {
                    q.add(next); visited.add(next);
                }
            }
        }
        return -1;
    }
}
