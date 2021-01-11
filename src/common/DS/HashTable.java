package common.DS;

// code stolen from chow10
// todo: don't steal code

public class HashTable<T> {
    LinkedList<T>[] table;
    int capacity;
    public int size = 0;
    int tabIndex = 0;
    LinkedNode<T> curr = null;
    public HashTable(int capacity) {
        table = new LinkedList[capacity];
        this.capacity = capacity;
        for (int i = capacity; --i>= 0; ) {
            table[i] = new LinkedList<T>();
        }
    }
    public boolean add(T obj) {
        int index = (Math.abs(obj.hashCode())) % this.capacity;
        // doesn't contain, add it
        if (!table[index].contains(obj)) {
            table[index].add(obj);
            size++;
            return true;
        }
        return false;
    }
    public boolean contains(T obj) {
        int index = (Math.abs(obj.hashCode())) % this.capacity;
        return table[index].contains(obj);
    }
    public boolean remove(T obj) {
        int index = (Math.abs(obj.hashCode())) % this.capacity;
        // contains it, remove it
        if (table[index].contains(obj)) {
            this.size--;
            return table[index].remove(obj);
        }
        return false;
    }
    public void resetIterator() {
        tabIndex = 0;
        curr = null;
    }
    public LinkedNode<T> next() {
        if (size != 0) {
            if (curr == null) {
                for (tabIndex = table.length; --tabIndex >= 0; ) {
                    if (table[tabIndex].size != 0) {
                        curr = table[tabIndex].head;
                        return curr;
                    }
                }
                // no element left!
                return null;
            }
            else {
                // go to the next one
                curr = curr.next;
                return curr;
            }
        }
        else {
            return null;
        }

    }
}