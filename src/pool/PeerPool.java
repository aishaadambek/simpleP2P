package pool;

import java.util.LinkedList;

public class PeerPool<T> {

    private LinkedList<T> queue;

    public T peek(){
        return queue.peek();
    }

    public T poll(){
        return queue.poll();
    }

    public void add(T t){
        queue.add(t);

    }

    public PeerPool(){
        queue = new LinkedList<T>();
    }

}