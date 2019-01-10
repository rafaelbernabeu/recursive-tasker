import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class RecursiveTasker<T> extends RecursiveTask<Collection<T>> {

    private final ForkJoinPool pool;
    private final List<T> tasks;
    private final int start;
    private final int end;
    private final Consumer<T> itemAction;
    private final BiFunction<Collection<T>, Collection<T>, Collection<T>> groupAction;
    private int splitValue;

    public RecursiveTasker(Collection<T> tasks, Consumer<T> itemAction) {
        this(tasks, 0, tasks.size(), itemAction, (x, y) -> { x.addAll(y); return x; }, getSplitValue(tasks));
    }

    public RecursiveTasker(Collection<T> tasks, Consumer<T> itemAction, BiFunction<Collection<T>, Collection<T>, Collection<T>> groupAction) {
        this(tasks, 0, tasks.size(), itemAction, groupAction, getSplitValue(tasks));
    }

    private RecursiveTasker(Collection<T> tasks, int start, int end, Consumer<T> itemAction, BiFunction<Collection<T>, Collection<T>, Collection<T>> groupAction, int splitValue) {
        pool = createPool();
        this.tasks = new ArrayList<>(tasks);
        this.start = start;
        this.end = end;
        this.itemAction = itemAction;
        this.groupAction = groupAction;
        this.splitValue = splitValue;
    }

    private static <T> int getSplitValue(Collection<T> tasks) {
        int listSize = tasks.size();
        int cores = Runtime.getRuntime().availableProcessors();
        if (listSize >= 1000) {
            return (listSize / (cores * 8)) + 1;
        } else if (listSize >= 100) {
            return (listSize / (cores * 4)) + 1;
        } else {
            return (listSize / (cores * 2)) + 1;
        }
    }

    private ForkJoinPool createPool() {
        if (pool == null) {
            return new ForkJoinPool();
        }
        return pool;
    }

    @Override
    protected Collection<T> compute() {
        if (end - start <= splitValue) {
            for (int i = start; i < end; i++) {
                itemAction.accept(tasks.get(i));
            }
            return tasks.subList(start, end);
        } else {
            int middle = start + ((end - start) / 2);
            System.out.println("splitValue: " + splitValue + ", start: " + start + ", middle: " + middle + ", end: " + end);
            RecursiveTask<Collection<T>> otherTask = new RecursiveTasker<T>(tasks, start, middle, itemAction, groupAction, splitValue);
            otherTask.fork();
            List<T> resultList = new CopyOnWriteArrayList<>(new RecursiveTasker<T>(tasks, middle, end, itemAction, groupAction, splitValue).compute());
            return groupAction.apply(resultList, otherTask.join());
        }
    }

    public void cancel() {
        pool.shutdownNow();
    }

    public Collection<T> start() throws Exception {
        return pool.invoke(this);
    }

    public Collection<T> start(int splitValue) {
        this.splitValue = splitValue;
        return pool.invoke(this);
    }
}
