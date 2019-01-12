Recursive Tasker

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

The for 100 elements and a splitValue of 10
100 < splitValue ?  ->  100/2... 
50 < splitValue ?  ->  50/2...
25 < splitValue ?  ->  25/2...
Until reach the splitValue, each division creates one new thread, forming a tree of tasks.


Signatures

public RecursiveTasker(Collection<T> tasks, Consumer<T> itemAction)
public RecursiveTasker(Collection<T> tasks, Consumer<T> itemAction, BiFunction<Collection<T>, Collection<T>, Collection<T>> groupAction) {

The itemAction will be executed for each item in the list.
The groupAction is to customize the join action of the results lists.

The default groupAction is collect all results.

You can use this groupAction operator to sum a list of integer results:
private static final BinaryOperator<Collection<Integer>> INTEGER_REDUCER = (x, y) -> Arrays.asList(x.stream().reduce((a, b) -> a + b).get() +
            y.stream().reduce((a, b) -> a + b).get());


Usage example:

new RecursiveTasker<>(list, t -> {}).start(2);

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
