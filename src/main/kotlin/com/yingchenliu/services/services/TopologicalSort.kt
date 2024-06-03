package com.yingchenliu.services.services

class TopologicalSort(private val graph: Map<String, List<String>>) {

    // A set to keep track of visited nodes during the topological sort.
    private val visited = mutableSetOf<String>()

    // A list to store the topological order of nodes in reverse order.
    private val topologicalOrder = mutableListOf<String>()

    // Function to perform the topological sort.
    fun topologicalSort(): List<String> {

        // Loop through the all nodes if the graph
        for (node in graph.keys) {
            // If the node has not been visited, perform depth-first search (DFS) from this node.
            if (node !in visited) {
                dfs(node)
            }
        }

        // Return the topological order in reverse as a list.
        return topologicalOrder.asReversed()

    }

    private fun dfs(node: String) {
        //Mark the current node as visited
        visited.add(node)

        //Explore all the neighbours of the current node
        for (neighbour in graph[node] ?: emptyList()) {
            // If the neighbor has not been visited, recursively visit it.
            if (neighbour !in visited) {
                dfs(neighbour)
            }
        }

        // Add the current node to the topological order.
        topologicalOrder.add(node)
    }

}