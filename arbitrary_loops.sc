    import scala.util.matching.Regex
import scala.io.Source.fromURL

import scala.collection.immutable._
import scala.math._
import scala.Int
import scala.Double

case class Edge(src: String, dest: String, weight:  Double)

def findArbitrageLoops(): Unit = {
    val (edges, currencies, rates) = getGraphCurrencies()
    val loops = getLoops(currencies, currencies, edges, Nil)
    if(loops == Nil)
        println("No opportunity here")         
    printLoops(loops, rates)
}

def getLoops(remaining: List[String], currencies: List[String], edges: List[Edge], loops: List[List[String]]): List[List[String]] = {

    // We run Bellman-Ford with each currency as source of the graph and return all the negative cycles
    remaining match {
        case h :: t => {
            val (dist, parents) = init(h, currencies)
            val (relax_distances, relax_parents) = relaxEdges(edges, currencies, dist, parents)
            val loop = getCycle(h, edges, relax_distances, relax_parents, Nil)
            if(loops.contains(loop))
                getLoops(t, currencies, edges, loops)
            else 
                getLoops(t, currencies, edges, loop :: loops)
        }

        case Nil => 
            loops
    }
}

def getGraphCurrencies(): (List[Edge], List[String], Map[(String, String), Double]) = {

    // Regular expression for data
    val jsonPattern: Regex = raw"""(\p{Upper}{3})_(\p{Upper}{3})["]: ["](\d+[.]\d+)""".r
    val str = fromURL("https://fx.priceonomics.com/v1/rates/").mkString
    val matches = jsonPattern.findAllMatchIn(str)

    // List of all currencies
    val currencies = (
        for(m <- jsonPattern.findAllMatchIn(str))
        yield (m.group(1))
    ).toList.distinct

    val data = (
        for (m <- jsonPattern.findAllMatchIn(str) if(m.group(1) != m.group(2)))
            yield (
                Edge(m.group(1), m.group(2), -log(m.group(3).toDouble)),
                ((m.group(1), m.group(2)) -> -log(m.group(3).toDouble))
            )

    ).toList

    // List of edge representing the graph and Map of rates
    val (edges, rates) = data.unzip
    (edges.toList, currencies, rates.toMap)
}


def init(startingCurrency: String, currencies: List[String]): (Map[String, Double], Map[String, String]) = {

    // 2.0/0.0 to get the Infinity value
    val distances = currencies.map(x => (x, 2.0/0.0)).toMap

    //Set the source distance to 0.0
    val srcDistances = distances + (startingCurrency -> 0.0)

    //At the beginning, no node has a parent
    val parents = currencies.map(x => (x, "None")).toMap
    (srcDistances, parents)
}

def relaxUntilNoMoreIteration(iterationIdx: Int, edges: List[Edge], distances: Map[String, Double], parents: Map[String, String]): (Map[String, Double], Map[String, String]) = {
    def relaxUntilNoMoreNode(list: List[Edge], distances: Map[String, Double], parents: Map[String, String]): (Map[String, Double], Map[String, String]) = {
        list match {
            case currentEdge :: tail =>
                val src = currentEdge.src
                val dest = currentEdge.dest
                val weight = currentEdge.weight
                val distSrc = distances.get(src).get
                val distDest = distances.get(dest).get

                // Update if shorter path found
                if(distSrc + weight < distDest)
                    relaxUntilNoMoreNode(tail, distances + (dest -> (distSrc + weight)), parents + (dest -> src))
                else
                    relaxUntilNoMoreNode(tail, distances, parents)
            case _ =>
                relaxUntilNoMoreIteration(iterationIdx - 1, edges, distances, parents)
        }
    }
    if (iterationIdx > 0)
        relaxUntilNoMoreNode(edges, distances, parents)
    else 
        (distances, parents)
}

def relaxEdges(edges: List[Edge], currencies: List[String], distances: Map[String, Double], parents: Map[String, String]): (Map[String, Double], Map[String, String]) = {
    relaxUntilNoMoreIteration(currencies.size - 1, edges, distances, parents)
}


def getCycle(source: String, edges: List[Edge], distances: Map[String, Double], parents: Map[String, String], cycle: List[String]): List[String] = {
    edges match {
        case currentEdge :: tail =>
            val src = currentEdge.src
            val dest = currentEdge.dest
            val weight = currentEdge.weight
            val distSrc = distances.get(src).get
            val distDest = distances.get(dest).get

            // If can be relaxed again then there is a negative cycle
            if (distDest < distSrc + weight)
                iterateLoop(source, parents.get(source).get, source :: Nil, parents)
            else
                getCycle(source, tail, distances, parents, cycle)

        case Nil =>
            Nil
    }
}

def iterateLoop(initial: String, current: String, list: List[String], parents: Map[String, String]): List[String] = {

    // Loop until the initial is reached again
    if (list.contains(current)){
        val end = (current :: list).reverse
        end.drop(end.indexOf(current))
    }else
        iterateLoop(initial, parents.get(current).get, current :: list, parents)
}

def printLoops(loops: List[List[String]], rates: Map[(String, String), Double]): Unit = {
    loops match {
        case Nil =>
            println("Done !")
        case h :: t =>
            printLoop(h, 100, rates)
            printLoops(t, rates)
    }
}

def printLoop(loop: List[String], money: Double, rates: Map[(String, String), Double]): Unit = {

    // Print each exchange made during the loop
    loop match {
        case h1 :: (h2 :: t)  => {
            val rate = rates((h1, h2))
            val newMoney = money * exp(-rate)
            println(s"From ${h1} to ${h2} at at the rate of ${exp(-rate)} : we got ${newMoney} ${h2}")
            printLoop(h2::t, newMoney, rates)
        }
        case _ => 
            println()
    }
}

// Call main function
findArbitrageLoops()





    




