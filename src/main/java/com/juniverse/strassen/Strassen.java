package com.juniverse.strassen;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * Multiplicación de matrices con el algoritmo de Strassen (7 multiplicaciones en lugar de 8 por nivel,
 * O(n^2.807)), ejecutada en paralelo con ForkJoin. Las matrices se rellenan con ceros hasta una potencia de 2
 * y por debajo de {@code THRESHOLD} se usa la multiplicación clásica.
 *
 * @author Java Universe
 */
public final class Strassen {

    private static final int THRESHOLD = 64; // Umbral para multiplicación clásica
    private static final ForkJoinPool POOL = new ForkJoinPool(); // Pool global

    /** Clase de utilidades: no se instancia. */
    private Strassen() {
        
    }
    
    /**
     * Multiplica dos matrices rectangulares.
     * @param A matriz de m x n
     * @param B matriz de n x p
     * @return matriz producto de m x p
     * @throws IllegalArgumentException si alguna está vacía o las dimensiones no son compatibles
     */
    public static double[][] multiply(double[][] A, double[][] B) {
        if (A.length == 0 || B.length == 0 || A[0].length == 0 || B[0].length == 0) {
            throw new IllegalArgumentException("Las matrices no pueden estar vacías");
        }
        if (A[0].length != B.length) {
            throw new IllegalArgumentException("Dimensiones incompatibles: A tiene " + A[0].length
                    + " columnas y B tiene " + B.length + " filas");
        }
        int maxSize = Math.max(Math.max(A.length, A[0].length), Math.max(B.length, B[0].length));
        int size = nextPowerOfTwo(maxSize);

        double[][] APrep = new double[size][size];
        double[][] BPrep = new double[size][size];

        // Copiar A
        for (int i = 0; i < A.length; i++) {
            System.arraycopy(A[i], 0, APrep[i], 0, A[0].length);
        }

        // Copiar B
        for (int i = 0; i < B.length; i++) {
            System.arraycopy(B[i], 0, BPrep[i], 0, B[0].length);
        }

        // Ejecutar tarea raíz
        double[][] CPrep = POOL.invoke(new StrassenTask(APrep, BPrep));

        // Recortar resultado
        double[][] C = new double[A.length][B[0].length];
        for (int i = 0; i < C.length; i++) {
            System.arraycopy(CPrep[i], 0, C[i], 0, C[0].length);
        }

        return C;
    }

    private static class StrassenTask extends RecursiveTask<double[][]> {

        private final double[][] A;
        private final double[][] B;

        /** Crea la tarea para multiplicar dos matrices cuadradas del mismo tamaño (potencia de 2). */
        StrassenTask(double[][] A, double[][] B) {
            this.A = A;
            this.B = B;
        }

        /**
         * Divide las matrices en cuatro bloques, calcula en paralelo los siete productos M1..M7 de Strassen
         * y los combina en el resultado. Con matrices pequeñas usa la multiplicación clásica.
         */
        @Override
        protected double[][] compute() {
            int n = A.length;
            double[][] result = new double[n][n];

            if (n <= THRESHOLD) {
                result = classicalMultiply(A, B);
            } else {
                int newSize = n / 2;

                double[][] A11 = new double[newSize][newSize];
                double[][] A12 = new double[newSize][newSize];
                double[][] A21 = new double[newSize][newSize];
                double[][] A22 = new double[newSize][newSize];

                double[][] B11 = new double[newSize][newSize];
                double[][] B12 = new double[newSize][newSize];
                double[][] B21 = new double[newSize][newSize];
                double[][] B22 = new double[newSize][newSize];

                split(A, A11, 0, 0);
                split(A, A12, 0, newSize);
                split(A, A21, newSize, 0);
                split(A, A22, newSize, newSize);

                split(B, B11, 0, 0);
                split(B, B12, 0, newSize);
                split(B, B21, newSize, 0);
                split(B, B22, newSize, newSize);

                // Crear tareas paralelas
                StrassenTask M1 = new StrassenTask(add(A11, A22), add(B11, B22));
                StrassenTask M2 = new StrassenTask(add(A21, A22), B11);
                StrassenTask M3 = new StrassenTask(A11, subtract(B12, B22));
                StrassenTask M4 = new StrassenTask(A22, subtract(B21, B11));
                StrassenTask M5 = new StrassenTask(add(A11, A12), B22);
                StrassenTask M6 = new StrassenTask(subtract(A21, A11), add(B11, B12));
                StrassenTask M7 = new StrassenTask(subtract(A12, A22), add(B21, B22));

                // Lanzar tareas en paralelo
                M1.fork();
                M2.fork();
                M3.fork();
                M4.fork();
                M5.fork();
                M6.fork();
                M7.fork();

                // Combinar resultados
                double[][] C11 = add(subtract(add(M1.join(), M4.join()), M5.join()), M7.join());
                double[][] C12 = add(M3.join(), M5.join());
                double[][] C21 = add(M2.join(), M4.join());
                double[][] C22 = add(subtract(add(M1.join(), M3.join()), M2.join()), M6.join());

                // Unir en resultado
                Strassen.join(C11, result, 0, 0);
                Strassen.join(C12, result, 0, newSize);
                Strassen.join(C21, result, newSize, 0);
                Strassen.join(C22, result, newSize, newSize);
            }
            return result;
        }
    }

    /** Multiplicación clásica O(n³) de dos matrices cuadradas. */
    private static double[][] classicalMultiply(double[][] A, double[][] B) {
        int n = A.length;
        double[][] result = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double sum = 0.0;
                for (int k = 0; k < n; k++) {
                    sum += A[i][k] * B[k][j];
                }
                result[i][j] = sum;
            }
        }

        return result;
    }

    /** Menor potencia de 2 mayor o igual que {@code n}. */
    private static int nextPowerOfTwo(int n) {
        if (n <= 0) {
            return 1;
        }
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        n++;
        return n;
    }

    /** Suma dos matrices cuadradas. */
    private static double[][] add(double[][] A, double[][] B) {
        int n = A.length;
        double[][] result = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                result[i][j] = A[i][j] + B[i][j];
            }
        }
        return result;
    }

    /** Resta dos matrices cuadradas (A − B). */
    private static double[][] subtract(double[][] A, double[][] B) {
        int n = A.length;
        double[][] result = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                result[i][j] = A[i][j] - B[i][j];
            }
        }
        return result;
    }

    /** Copia a {@code child} el bloque de {@code parent} que empieza en el desplazamiento indicado. */
    private static void split(double[][] parent, double[][] child, int rowOffset, int colOffset) {
        int n = child.length;
        for (int i = 0; i < n; i++) {
            System.arraycopy(parent[i + rowOffset], colOffset, child[i], 0, n);
        }
    }

    /** Copia {@code child} dentro de {@code parent} a partir del desplazamiento indicado. */
    private static void join(double[][] child, double[][] parent, int rowOffset, int colOffset) {
        int n = child.length;
        for (int i = 0; i < n; i++) {
            System.arraycopy(child[i], 0, parent[i + rowOffset], colOffset, n);
        }
    }

}
