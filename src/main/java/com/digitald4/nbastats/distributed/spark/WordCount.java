package com.digitald4.nbastats.distributed.spark;

import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Dataset;

public class WordCount {
	public static void main(String[] args) {
		// Should be some file on your system
		String logFile = "src/main/java/com/digitald4/nbastats/compute/spark/WordCount.java";
		SparkSession spark = SparkSession.builder().appName("Simple Application").getOrCreate();
		Dataset<String> logData = spark.read().textFile(logFile).cache();

		long numAs = logData.filter(s -> s.contains("a")).count();
		long numBs = logData.filter(s -> s.contains("b")).count();

		System.out.println("Lines with a: " + numAs + ", lines with b: " + numBs);

		spark.stop();
	}
}
