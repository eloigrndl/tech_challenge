# Tech Challenge : Arbitrage Loops

During one of the interviews I had, the compagny asked to work on the [following puzzle](https://priceonomics.com/jobs/puzzle/) in the field of graph theory,  using Scala. It consists of finding arbitrage loops given exchange rates. This is my attempt to this challenge : the solution is a Scala script, executable by [Ammonite](http://ammonite.io/#ScalaScripts) 

## Solution :

Here N represents the number of currencies. 

This problem is related to graph theory as it can be reduce to a negative cycle detection problem that can solved using Bellman-Ford algorithm. 
They are 3 big parts in this algorithm :
- loaded and setting up the data
- find the negative cycles
- print the potential cycles / opportunities

### Time complexity :

To load and parse data (construct the graph), I've used regular expressions as to me, it was the simplest way to do it. This first step takes O(N^2) time : for each exchange rates X from currency i to currency i+1, we construct an edge between the 2 currency nodes with a weight of -log(X). We have to set up N(N-1) edges that represents those possible conversion from one currency to another (we do not add an edge to represent an exchange between a currency and itself).

The second part is to find the cycles. To do so, we run the Bellman-Ford algorithm N times, each time setting the n-th currency as source on the graph.
Bellman-Ford algorithm has a worst case performance of O(N^3). Since we run it N times, this second part takes at most O(N^4).

The last part is printing the loops we found. This takes at most O(N) time as the number of cycles depends linearly on the number of nodes and every node can only be part of one cycle at the time (otherwise we could merge the 2 cycles this node is part of).

The overall algorithm is upper-bounded by the middle part and thus take at most O(N^4).

Regarding space, I decided to go with Immutable objects (Map, List among others) as I've learned to always use immutable data strutures in Scala when possible. In this case, we might waste space as we update a lot the "distances" and "parents" map. To solve this issue, we could simply choose to use mutable objects : it would simplify the program and save space. (for a comprehension purpose, I also added an additional map to get access to the exchange rates while printing the cycles : this is purely optional and can be removed if the prints are not needed)
