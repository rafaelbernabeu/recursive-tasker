package br.bernabeu;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(JUnit4.class)
public class RecursiveTaskerTest {

    private static final BinaryOperator<Collection<Integer>> INTEGER_COUNTER_REDUCER = (x, y) ->
            Collections.singletonList(
                    x.stream().reduce((a, b) -> a + b).orElseGet(() -> 0) +
                    y.stream().reduce((a, b) -> a + b).orElseGet(() -> 0)
            );

    private static final BinaryOperator<Collection<String>> STRING_COUNTER_REDUCER = (x, y) ->
            Collections.singletonList(
                    String.valueOf(
                            x.stream().reduce((a, b) -> a + b).orElseGet(() -> "").length() +
                            y.stream().reduce((a, b) -> a + b).orElseGet(() -> "").length()
                    )
            );

    private static final Consumer CONSUMER = c -> { System.out.println(Thread.currentThread().getName()); };

    @Test
    public void testarConstrutor() {
        ForkJoinPool commomPool = ForkJoinPool.commonPool();
        Collection<String> collectionS = Collections.singleton("");
        Collection<Integer> collectionI = Collections.singleton(0);

        Collection<String> hashSetResult = new RecursiveTasker<>(new HashSet<>(collectionS), CONSUMER, STRING_COUNTER_REDUCER, commomPool).start();
        Collection<String> arrayListResult = new RecursiveTasker<>(new ArrayList<>(collectionS), CONSUMER, STRING_COUNTER_REDUCER, commomPool).start();
        Collection<String> treeSetResult = new RecursiveTasker<>(new TreeSet<>(collectionS), CONSUMER, STRING_COUNTER_REDUCER, commomPool).start();

        Collection<Integer> hashSetResult2 = new RecursiveTasker<>(new HashSet<>(collectionI), CONSUMER, INTEGER_COUNTER_REDUCER, commomPool).start();
        Collection<Integer> arrayListResult2 = new RecursiveTasker<>(new ArrayList<>(collectionI), CONSUMER, INTEGER_COUNTER_REDUCER, commomPool).start();
        Collection<Integer> treeSetResult2 = new RecursiveTasker<>(new TreeSet<>(collectionI), CONSUMER, INTEGER_COUNTER_REDUCER, commomPool).start();

        Assert.assertEquals(collectionS.stream().findFirst().orElseThrow(() -> new UnknownError()).length(), Integer.parseInt(hashSetResult.stream().findAny().orElseThrow(() -> new UnknownError())));
        Assert.assertEquals(collectionS.stream().findFirst().orElseThrow(() -> new UnknownError()).length(), Integer.parseInt(arrayListResult.stream().findAny().orElseThrow(() -> new UnknownError())));
        Assert.assertEquals(collectionS.stream().findFirst().orElseThrow(() -> new UnknownError()).length(), Integer.parseInt(treeSetResult.stream().findAny().orElseThrow(() -> new UnknownError())));

        Assert.assertEquals(collectionI.stream().findAny().orElseThrow(() -> new UnknownError()), hashSetResult2.stream().findAny().orElseThrow(() -> new UnknownError()));
        Assert.assertEquals(collectionI.stream().findAny().orElseThrow(() -> new UnknownError()), arrayListResult2.stream().findAny().orElseThrow(() -> new UnknownError()));
        Assert.assertEquals(collectionI.stream().findAny().orElseThrow(() -> new UnknownError()), treeSetResult2.stream().findAny().orElseThrow(() -> new UnknownError()));
    }

    @Test
    public void testarSomandoOsValoresDeUmaString() {
        String rafael = "Rafael";
        Collection<String> collection = Collections.singleton(rafael);

        for (int i = 100; i > 0; i--) {

            Collection<String> hashSetResult = new RecursiveTasker<>(new HashSet<>(collection), CONSUMER, STRING_COUNTER_REDUCER).start(i);
            Collection<String> arrayListResult = new RecursiveTasker<>(new ArrayList<>(collection), CONSUMER, STRING_COUNTER_REDUCER).start(i);
            Collection<String> treeSetResult = new RecursiveTasker<>(new TreeSet<>(collection), CONSUMER, STRING_COUNTER_REDUCER).start(i);

            Assert.assertEquals(rafael.length(), Integer.parseInt(hashSetResult.stream().findAny().orElseThrow(() -> new UnknownError())));
            Assert.assertEquals(rafael.length(), Integer.parseInt(arrayListResult.stream().findAny().orElseThrow(() -> new UnknownError())));
            Assert.assertEquals(rafael.length(), Integer.parseInt(treeSetResult.stream().findAny().orElseThrow(() -> new UnknownError())));

        }
    }

    @Test
    public void testarPopulandoUmaLista() {
        String value = "0";
        List<String> values = Stream.iterate(value, x -> x).limit(10000).collect(Collectors.toList());
        for (int i = 100; i > 0; i--) {
            new RecursiveTasker<>(values, x -> { Assert.assertEquals(value, x); CONSUMER.accept(x);}).start(i);
        }
    }

    @Test
    public void testarSomandoOsValoresDeUmaLista() {
        //0+1+2+3+4+5+6+7+8+9
        List<Integer> values = Stream.iterate(0, x -> x + 1).limit(10).collect(Collectors.toList());

        for (int i = 100; i > 0; i--) {
            Collection<Integer> result = new RecursiveTasker<>(values, CONSUMER, INTEGER_COUNTER_REDUCER).start(i);

            Assert.assertEquals(0 + 1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9, result.stream().findFirst().get().intValue());
        }
    }

    @Test
    public void testarQueEhMaisRapidoMesmo() {
        List<Integer> values = Stream.iterate(0, x -> x + 1).limit(100).collect(Collectors.toList());
        Consumer<Integer> consumer = t -> {
            try {
                Thread.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        long ti1, ti2, tf1, tf2 = 0;
        ti1 = System.currentTimeMillis();
        values.forEach(consumer);
        tf1 = System.currentTimeMillis();

        ti2 = System.currentTimeMillis();
        Collection<Integer> result = new RecursiveTasker<>(values, consumer).start();
        tf2 = System.currentTimeMillis();

        System.out.println("SingleThread: " + (tf1 - ti1));
        System.out.println("MultiThread : " + (tf2 - ti2));
        Assert.assertTrue((tf1 - ti1) > (tf2 - ti2));
    }
}
