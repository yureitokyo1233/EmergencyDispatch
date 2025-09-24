package dispatch;

public class Unit {
    public int id;
    public int section;
    public boolean available;

    public Unit(int id, int section) {
        this.id = id;
        this.section = section;
        this.available = true;
    }
}
