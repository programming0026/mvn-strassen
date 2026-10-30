# Multiplicación de matrices con Strassen

Implementación paralela del algoritmo de Strassen para multiplicar matrices, con soporte para matrices rectangulares.

## Características

- 7 multiplicaciones recursivas por nivel en lugar de 8.
- Paralelismo con `ForkJoinPool` / `RecursiveTask`.
- Relleno automático con ceros hasta una potencia de 2.
- Multiplicación clásica por debajo de un umbral (64) para evitar la sobrecarga.
- Valida que las dimensiones sean compatibles.

## Requisitos

- JDK 21 y Maven 3.9 o superior.

## Compilar y ejecutar

```bash
mvn compile exec:java
```

## Notas

- Con matrices pequeñas el algoritmo clásico es más rápido; Strassen compensa a partir de cientos de filas.

## JAR ejecutable (fat jar)

```bash
mvn clean package
java -jar target/mvn-strassen-1.0-SNAPSHOT-launcher.jar
```

El archivo `-launcher.jar` incluye todas las dependencias.

## Licencia

Código de ejemplo de uso libre con fines educativos.
