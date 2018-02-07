mvn -f distributed.xml package
~/Downloads/spark-2.2.1-bin-hadoop2.7/bin/spark-submit \
  --class "com.digitald4.nbastats.distributed.spark.ProcessFanDuel" \
  --master spark://poweredge410:7077 \
  target/NBAStats-distributed-1.0.jar

  # --master local[4] \
  # --master spark://poweredge410:7077
