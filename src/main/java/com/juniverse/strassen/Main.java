package com.juniverse.strassen;

import java.util.Arrays;

/**
 * Ejemplo: multiplica dos matrices de 3x3.
 *
 * @author ajuar
 */
public class Main {

    /**
     * Punto de entrada.
     * @param args no se usan
     */
    public static void main(String[] args) {
        double[][] A = {
            {1.0, 2.0, 3.0},
            {4.0, 5.0, 6.0},
            {7.0, 8.0, 9.0}
        };

        double[][] B = {
            {9.0, 8.0, 7.0},
            {6.0, 5.0, 4.0},
            {3.0, 2.0, 1.0}
        };

        double[][] C = Strassen.multiply(A, B);

        System.out.println("Resultado:");
        for (double[] row : C) {
            System.out.println(Arrays.toString(row));
        }
    }
}
