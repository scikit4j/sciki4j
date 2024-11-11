package eigendecomposed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.commons.math4.legacy.linear.BlockRealMatrix;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.numbers.core.Precision;

import definition.EdgeList2;
import definition.NodeList2;

public class MatrixCalculation {
	
    public static class MatrixResult {
        private static RealMatrix adjacency_matrix;
        private static RealMatrix degree_matrix;
        private static RealMatrix laplacian_matrix;

        public MatrixResult(RealMatrix adjacency_matrix, RealMatrix degree_matrix, RealMatrix laplacian_matrix) {
            MatrixResult.adjacency_matrix = adjacency_matrix;
            MatrixResult.degree_matrix = degree_matrix;
            MatrixResult.laplacian_matrix = laplacian_matrix;
        }

        public RealMatrix getAdjacencyMatrix() {
            return adjacency_matrix;
        }

        public static RealMatrix getDegreeMatrix() {
            return degree_matrix;
        }

        public static RealMatrix getLaplacianMatrix() {
            return laplacian_matrix;
        }
    }
    
    /**
     * Converts an edge list into an adjacency matrix.
     *
     * @param edge_list The edge list to convert.
     * @return 2D array representing the adjacency matrix.
     */
    public static RealMatrix convertToAdjacencyMatrix(ArrayList<EdgeList2> edge_list) {
        Set<String> unique_indices = new HashSet<>();
        for (EdgeList2 edge : edge_list) {
            unique_indices.add(edge.getSource());
            unique_indices.add(edge.getTarget());
        }

        int dimension = unique_indices.size();

        double[][] adjacency_matrix_data = new double[dimension][dimension];

        for (EdgeList2 edge : edge_list) {
            int i = Integer.parseInt(edge.getSource());
            int j = Integer.parseInt(edge.getTarget());
            
            double weight = edge.getWeight();
            
            // double weight = Math.round(edge.getWeight() * 10000.0) / 10000.0;
            
            adjacency_matrix_data[i][j] = weight;
            adjacency_matrix_data[j][i] = weight;
        }
        
        RealMatrix adjacency_matrix = new BlockRealMatrix(adjacency_matrix_data);
        return adjacency_matrix;
    }
    
    public static RealMatrix convertToXMatrix(ArrayList<NodeList2> nodeList) {
        int dimension = nodeList.size();
        
        // Determine the actual number of eigenvector properties
        int vectorDimension = (int) nodeList.get(0).getProperties().keySet().stream()
                                .filter(key -> key.startsWith("eigenvector_"))
                                .count();

        double[][] x_matrix_data = new double[dimension][vectorDimension];
        
        for (int i = 0; i < dimension; i++) {
            Map<String, Object> properties = nodeList.get(i).getProperties();
            for (int j = 0; j < vectorDimension; j++) {
                String key = "eigenvector_" + j;
                if (properties.containsKey(key)) {
                    x_matrix_data[i][j] = (double) properties.get(key);
                }
            }
        }
        
        return new BlockRealMatrix(x_matrix_data);
    }
   
    /**
     * Calculates the Laplacian matrix based on the provided degree matrix, adjacency matrix and desired type of Laplacian matrix.
     *
     * @param degree_matrix    The degree matrix of the graph.
     * @param adjacency_matrix The adjacency matrix of the graph.
     * @param algorithm       The algorithm to use for Laplacian matrix calculation ("sym" or "rw").
     * @return The Laplacian matrix.
     */
    public static RealMatrix calculateLaplacianMatrix(RealMatrix degree_matrix, RealMatrix adjacency_matrix, String algorithm) {
        try {
            RealMatrix laplacian_matrix;

            switch (algorithm) {
                case "sym":
                    laplacian_matrix = calculateSymmetricLaplacianMatrix(degree_matrix, adjacency_matrix);
                    break;
                case "rw":
                    laplacian_matrix = calculateRandomWalkLaplacianMatrix(degree_matrix, adjacency_matrix);
                    break;
                case "ad":
                    laplacian_matrix = calculateAdaptiveLaplacianMatrix(degree_matrix, adjacency_matrix);
                    break;
                default:
                	laplacian_matrix = calculateSymmetricLaplacianMatrix(degree_matrix, adjacency_matrix);
            }

            return laplacian_matrix;
        } catch (Exception e) {
            throw new RuntimeException("Error calculating Laplacian matrix: " + e.getMessage());
        }
    }

    /**
     * Calculates the degree matrix from an adjacency matrix.
     *
     * @param adj_mat The adjacency matrix.
     * @return The degree matrix.
     */
    public static RealMatrix calculateDegreeMatrix(RealMatrix adjacency_matrix) {
        int dimension = adjacency_matrix.getColumnDimension();

        double[] columnSum = new double[dimension];
        for (int col = 0; col < dimension; col++) {
            columnSum[col] = adjacency_matrix.getColumnVector(col).getL1Norm();
        }

        return MatrixUtils.createRealDiagonalMatrix(columnSum);
        // return round4Digits(MatrixUtils.createRealDiagonalMatrix(columnSum));
    }

    /**
     * Calculates the symmetric Laplacian matrix based on the degree matrix and adjacency matrix.
     *
     * @param degree_matrix    The degree matrix of the graph.
     * @param adjacency_matrix The adjacency matrix of the graph.
     * @return The symmetric Laplacian matrix.
     */
    public static RealMatrix calculateSymmetricLaplacianMatrix(RealMatrix degree_matrix, RealMatrix adjacency_matrix) {
        int dimension = degree_matrix.getColumnDimension();
        RealMatrix dHalf = MatrixUtils.createRealMatrix(dimension, dimension);

        for (int i = 0; i < dimension; i++) {
            double dHalfValue = 1.0 / Math.sqrt(degree_matrix.getEntry(i, i));
            dHalf.setEntry(i, i, dHalfValue);
        }

        RealMatrix laplacian_matrix_normalized = dHalf.multiply(adjacency_matrix).multiply(dHalf);
        
//        // Round the elements to the 4th digit
//        for (int i = 0; i < dimension; i++) {
//            for (int j = 0; j < dimension; j++) {
//            	double value = laplacian_matrix_normalized.getEntry(i, j);
//                //double value = Precision.round(laplacian_matrix_normalized.getEntry(i, j), 4);
//                laplacian_matrix_normalized.setEntry(i, j, value);
//            }
//        }
        
        return laplacian_matrix_normalized;
        // return round4Digits(laplacian_matrix_normalized);
    }

    /**
     * Calculates the random walk Laplacian matrix based on the degree matrix and adjacency matrix.
     *
     * @param degree_matrix    The degree matrix of the graph.
     * @param adjacency_matrix The adjacency matrix of the graph.
     * @return The random walk Laplacian matrix.
     */
    public static RealMatrix calculateRandomWalkLaplacianMatrix(RealMatrix degree_matrix, RealMatrix adjacency_matrix) {
        RealMatrix inverse_degree_matrix = MatrixUtils.inverse(degree_matrix);
        RealMatrix random_walk_laplacian_matrix = inverse_degree_matrix.multiply(adjacency_matrix);
        
        
        return random_walk_laplacian_matrix;
//        return round4Digits(random_walk_laplacian_matrix);
    }
    
    public static RealMatrix calculateAdaptiveLaplacianMatrix(RealMatrix degreeMatrix, RealMatrix adjacencyMatrix) {
        int dimension = degreeMatrix.getRowDimension();
        
        // Extract degree vector from the diagonal of degree matrix
        double[] degreeVector = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            degreeVector[i] = degreeMatrix.getEntry(i, i);
        }
        
        double[] D_local = new double[dimension];

        // Calculate D_local based on neighbors
        for (int i = 0; i < dimension; i++) {
            // Collect neighbor indices of node i
            double sumNeighbors = 0.0;
            int neighborCount = 0;
            for (int j = 0; j < adjacencyMatrix.getColumnDimension(); j++) {
                if (adjacencyMatrix.getEntry(i, j) > 0) {
                    sumNeighbors += degreeVector[j];
                    neighborCount++;
                }
            }

            // Only calculate if there are neighbors
            if (neighborCount > 0) {
                D_local[i] = sumNeighbors / degreeVector[i];
            } else {
                D_local[i] = 0;
            }
        }

        // Construct D_local_matrix with inverse square roots on the diagonal
        double[] D_local_inv_sqrt = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            D_local_inv_sqrt[i] = D_local[i] > 0 ? 1.0 / Math.sqrt(D_local[i]) : 0;
        }
        RealMatrix D_local_inv_sqrt_matrix = MatrixUtils.createRealDiagonalMatrix(D_local_inv_sqrt);

        // Calculate the adaptive Laplacian matrix
        return D_local_inv_sqrt_matrix.multiply(adjacencyMatrix).multiply(D_local_inv_sqrt_matrix);
    }
    
    public static RealMatrix round4Digits(RealMatrix matrix) {
    	int dimension = matrix.getColumnDimension();
    	
        // Round the elements to the 4th digit
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                double value = Precision.round(matrix.getEntry(i, j), 4);
                matrix.setEntry(i, j, value);
            }
        }

        return matrix;
    }
    
    
//	public static void main(String[] args)
//	{
//		ArrayList<EdgeList2> edgeList2= new ArrayList<>();
//        edgeList2.add(new EdgeList2("0", "20", 0.7918, 72));
//        edgeList2.add(new EdgeList2("0", "19", 0.00062, 71));
//        edgeList2.add(new EdgeList2("0", "18", 0.00103, 70));
//        edgeList2.add(new EdgeList2("0", "17", 0.01437, 69));
//        edgeList2.add(new EdgeList2("0", "16", 0.00169, 68));
//        edgeList2.add(new EdgeList2("0", "15", 0.28752, 67));
//        edgeList2.add(new EdgeList2("0", "14", 0.80878, 66));
//        edgeList2.add(new EdgeList2("0", "13", 0.0002, 65));
//        edgeList2.add(new EdgeList2("0", "12", 0.05861, 64));
//        edgeList2.add(new EdgeList2("0", "11", 0.88376, 63));
//        edgeList2.add(new EdgeList2("0", "10", 0.00084, 38));
//        edgeList2.add(new EdgeList2("0", "9", 0.00028, 37));
//        edgeList2.add(new EdgeList2("0", "8", 0.00073, 36));
//        edgeList2.add(new EdgeList2("0", "7", 0.91748, 35));
//        edgeList2.add(new EdgeList2("0", "6", 0.01991, 34));
//        edgeList2.add(new EdgeList2("0", "5", 0.71734, 19));
//        edgeList2.add(new EdgeList2("0", "4", 0.01872, 18));
//        edgeList2.add(new EdgeList2("0", "3", 0.81038, 17));
//        edgeList2.add(new EdgeList2("0", "2", 0.00014, 16));
//        edgeList2.add(new EdgeList2("0", "1", 0.00007, 15));
//        edgeList2.add(new EdgeList2("1", "20", 0.00014, 91));
//        edgeList2.add(new EdgeList2("1", "19", 0.79078, 90));
//        edgeList2.add(new EdgeList2("1", "18", 0.42139, 89));
//        edgeList2.add(new EdgeList2("1", "17", 0.12998, 88));
//        edgeList2.add(new EdgeList2("1", "16", 0.37667, 87));
//        edgeList2.add(new EdgeList2("1", "15", 0.01936, 86));
//        edgeList2.add(new EdgeList2("1", "14", 0.00021, 85));
//        edgeList2.add(new EdgeList2("1", "13", 0.56941, 84));
//        edgeList2.add(new EdgeList2("1", "12", 0.06084, 83));
//        edgeList2.add(new EdgeList2("1", "11", 0.00007, 82));
//        edgeList2.add(new EdgeList2("1", "10", 0.52795, 81));
//        edgeList2.add(new EdgeList2("1", "9", 0.53828, 80));
//        edgeList2.add(new EdgeList2("1", "8", 0.32461, 79));
//        edgeList2.add(new EdgeList2("1", "7", 0.00007, 78));
//        edgeList2.add(new EdgeList2("1", "6", 0.09581, 77));
//        edgeList2.add(new EdgeList2("1", "5", 0.00038, 76));
//
//        for (EdgeList2 edge : edgeList2) {
//            System.out.println(edge);
//        }
//        
//        Set<String> unique_indices = new HashSet<>();
//        for (EdgeList2 edge : edgeList2) {
//            unique_indices.add(edge.getSource());
//
//            unique_indices.add(edge.getTarget());
//
//        }
//
////        List<String> sorted_indices = new ArrayList<>(unique_indices);
////        Collections.sort(sorted_indices);
//
//        int dimension = unique_indices.size();
//        
////        Map<String, Integer> index_mapping = new HashMap<>();
////        for (int i = 0; i < dimension; i++) {
////            index_mapping.put(sorted_indices.get(i), i);
////        }
//
//        double[][] adjacency_matrix_data = new double[dimension][dimension];
//
//        for (EdgeList2 edge : edgeList2) {
//            int i = Integer.parseInt(edge.getSource());
//            System.out.println(i);
//            int j = Integer.parseInt(edge.getTarget());
//            System.out.println(j);
//            
//            double weight = Math.round(edge.getWeight() * 10000.0) / 10000.0;
//            System.out.println(weight);
////            double weight = edge.getWeight();
//            adjacency_matrix_data[i][j] = weight;
//            adjacency_matrix_data[j][i] = weight;
//        }
//        
//        RealMatrix adjacency_matrix = new BlockRealMatrix(adjacency_matrix_data);
//        System.out.println(adjacency_matrix.toString());
////        return adjacency_matrix;
//
//	}

    
}
