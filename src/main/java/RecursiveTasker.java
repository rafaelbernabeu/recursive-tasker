import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;

public class RecursiveTasker<T> extends RecursiveTask<Collection<T>> {

    private final ForkJoinPool pool;
    private final List<T> tasks;
    private final int start;
    private final int end;
    private final Consumer<T> itemAction;
    private final BinaryOperator<Collection<T>> groupAction;
    private int splitValue;

    public RecursiveTasker(Collection<T> tasks, Consumer<T> itemAction) {
        this(tasks, 0, tasks.size(), itemAction, (x, y) -> { x.addAll(y); return x; }, getSplitValue(tasks), null);
    }

    public RecursiveTasker(Collection<T> tasks, Consumer<T> itemAction, BinaryOperator<Collection<T>> groupAction) {
        this(tasks, 0, tasks.size(), itemAction, groupAction, getSplitValue(tasks), null);
    }

    private RecursiveTasker(Collection<T> tasks, int start, int end, Consumer<T> itemAction, BinaryOperator<Collection<T>> groupAction, int splitValue, ForkJoinPool pool) {
        this.pool = pool == null ? new ForkJoinPool() : pool;
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

    @Override
    protected Collection<T> compute() {
        if (end - start <= splitValue) {
            for (int i = start; i < end; i++) {
                try {
                    itemAction.accept(tasks.get(i));
                } catch (Exception e) {
                    pool.shutdownNow();
                    throw e;
                }
            }
            return tasks.subList(start, end);
        } else {
            int middle = start + ((end - start) / 2);
            RecursiveTask<Collection<T>> otherTask = new RecursiveTasker<T>(tasks, start, middle, itemAction, groupAction, splitValue, pool);
            otherTask.fork();
            Collection<T> resultList = new RecursiveTasker<T>(tasks, middle, end, itemAction, groupAction, splitValue, pool).compute();
            return groupAction.apply(resultList, otherTask.join());
        }
    }

    public Collection<T> start() {
        return pool.invoke(this);
    }

    public Collection<T> start(int splitValue) {
        this.splitValue = splitValue;
        return pool.invoke(this);
    }
}
