package dispatch;

public class Unit {
    public int id;
    public int section;
    public boolean available;

    public int x, y;
    public int targetSection = -1;

    public Unit(int id, int section) {
        this.id = id;
        this.section = section;
        this.available = true;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
