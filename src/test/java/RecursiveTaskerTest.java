import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(JUnit4.class)
public class RecursiveTaskerTest {

    private static final BinaryOperator<Collection<Integer>> INTEGER_REDUCER = (x, y) -> Arrays.asList(x.stream().reduce((a, b) -> a + b).get() +
            y.stream().reduce((a, b) -> a + b).get());

    @Test
    public void testarConstrutor() throws Exception {
        new RecursiveTasker<>(new HashSet<>(), t -> {}).start();
        new RecursiveTasker<>(new ArrayList<>(), t -> {}).start();
        new RecursiveTasker<>(new TreeSet<>(), t -> {}).start();
    }

    @Test
    public void testarPopulandoUmaLista() throws Exception {
        String value = "0";
        List<String> values = Stream.iterate(value, x -> x).limit(10000).collect(Collectors.toList());
        new RecursiveTasker<>(values, x -> Assert.assertEquals(value, x)).start();
    }

    @Test
    public void testarSomandoOsValoresDeUmaLista() throws Exception {
        //0+1+2+3+4+5+6+7+8+9
        List<Integer> values = Stream.iterate(0, x -> x + 1).limit(10).collect(Collectors.toList());
        Collection<Integer> result = new RecursiveTasker<>(values, x -> {}, INTEGER_REDUCER).start();
        Assert.assertEquals(45, result.stream().findFirst().get().intValue());
    }
}
