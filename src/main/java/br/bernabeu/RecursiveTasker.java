package br.bernabeu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

public class RecursiveTasker<T> extends RecursiveTask<Collection<T>> {

    private final List<T> tasks;
    private final int start;
    private final int end;
    private final Consumer<T> itemAction;
    private final BinaryOperator<Collection<T>> groupAction;
    private ForkJoinPool pool;
    private int splitValue;

    public RecursiveTasker(Collection<T> tasks, Consumer<T> itemAction) {
        this(tasks, itemAction, (ForkJoinPool) null);
    }

    public RecursiveTasker(Collection<T> tasks, Consumer<T> itemAction, ForkJoinPool pool) {
        this(tasks, itemAction, (x, y) -> { x.addAll(y); return x; }, pool);
    }

    public RecursiveTasker(Collection<T> tasks, Consumer<T> itemAction, BinaryOperator<Collection<T>> groupAction) {
        this(tasks, itemAction, groupAction, null);
    }

    public RecursiveTasker(Collection<T> tasks, Consumer<T> itemAction, BinaryOperator<Collection<T>> groupAction, ForkJoinPool pool) {
        this(tasks, 0, tasks.size(), itemAction, groupAction, getSplitValue(tasks), pool);
    }

    private RecursiveTasker(Collection<T> tasks, int start, int end, Consumer<T> itemAction, BinaryOperator<Collection<T>> groupAction, int splitValue, ForkJoinPool pool) {
        this.pool = pool;
        this.tasks = new ArrayList<>(tasks);
        this.start = start;
        this.end = end;
        this.itemAction = itemAction;
        this.groupAction = groupAction;
        this.splitValue = splitValue;
    }

    private static <T> int getSplitValue(Collection<T> tasks) {
        int size = tasks.size();
        int r = size / (Runtime.getRuntime().availableProcessors() * 2);
        return r > 0 ? r : 1;
    }

    @Override
    protected Collection<T> compute() {
        if (end - start <= splitValue) {
            for (int i = start; i < end; i++) {
                itemAction.accept(tasks.get(i));
            }
            return groupAction.apply(tasks.subList(start, end), Collections.emptyList());
        } else {
            int middle = start + ((end - start) / 2);
            RecursiveTask<Collection<T>> otherTask = new RecursiveTasker<>(tasks, start, middle, itemAction, groupAction, splitValue, pool);
            otherTask.fork();
            Collection<T> resultList = new RecursiveTasker<>(tasks, middle, end, itemAction, groupAction, splitValue, pool).compute();
            return groupAction.apply(resultList, otherTask.join());
        }
    }

    public Collection<T> start(Function<Collection<T>, Integer> splitValueFuncion) {
        return this.start(splitValueFuncion.apply(this.tasks));
    }

    public Collection<T> start(int splitValue) {
        if (splitValue <= 0) {
            throw new IllegalArgumentException("Value must be greater than zero.");
        }
        this.splitValue = splitValue;
        return this.start();
    }

    public Collection<T> start() {
        if (pool == null) {
            pool = ForkJoinPool.commonPool();
        }
        return pool.invoke(this);
    }
}
