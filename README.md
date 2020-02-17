# Recursive Tasker

Suppose that you have a list with X tasks.
The code will recursively split the list and start new threads.

                    [list]   
                   /       \
                  /         \
          [1/2 list]      [1/2 list]  
            /      \          /   \
           /        \        ..   ..
     [1/4 list]    [1/4 list]      
       /       \        / \
      /         \      ..  ..
    [1/8 list] [1/8 list] 


You can specify the min number of itens in the sublists to stop the splitting via parameter or use the default value.
The default value is calculated by dividing the amount of tasks by the available cores times a dynamic value, plus one.

## Signatures
``` java
public RecursiveTasker(Collection<T> tasks, Consumer<T> itemAction)
public RecursiveTasker(Collection<T> tasks, Consumer<T> itemAction, ForkJoinPool pool)
public RecursiveTasker(Collection<T> tasks, Consumer<T> itemAction, BinaryOperator<Collection<T>> groupAction)
public RecursiveTasker(Collection<T> tasks, Consumer<T> itemAction, BinaryOperator<Collection<T>> groupAction, ForkJoinPool pool)
```
A collection of T elements that will be applied the consumer object.

The itemAction will be executed for each item in the list.
The groupAction is to customize the join action of the results lists.

The default groupAction is collect all results.

You can use this groupAction operator to sum a list of integer results:
``` java
private static final BinaryOperator<Collection<Integer>> INTEGER_COUNTER_REDUCER = (x, y) ->
            Collections.singletonList(
                    x.stream().reduce((a, b) -> a + b).orElseGet(() -> 0) +
                    y.stream().reduce((a, b) -> a + b).orElseGet(() -> 0)
            );
```

## Usage example:

``` java
new RecursiveTasker<>(list, t -> { someObject.someAction(t) }).start(2);

new RecursiveTasker<>(list, System.out::println).start(2);
```

Given an List<T>  with size of 10
  
When run the recursive tasker with a start value = 2
Then will result in:

    | 0 , 1 , 2 , 3 , 4 , 5 , 6 , 7 , 8 , 9 |    
    |0,1,2,3,4|         --        |5,6,7,8,9|     
    |0,1|   |2, 3 , 4|  --  |5,6|  |7, 8 , 9| 
    |0,1|   |2|  |3,4|  --  |5,6|  |7|  |8,9|
   
   
Obs.:

1) Multithread execution is unpredictable, so the results order are random.
2) Use single thread execution for small tasks. Keep in mind the computional cost of creation and management of threads.
3) Item actions won't changes the reference of the objects in the list. Only mutates the data inside them.


See:
[Fork/Join Framework](https://docs.oracle.com/javase/tutorial/essential/concurrency/forkjoin.html)

