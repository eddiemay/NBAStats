#mvn -f distributed.xml package
$SPARK_HOME/bin/spark-submit \
  --class "com.digitald4.nbastats.distributed.FantasyProcessor" \
  --master spark://poweredge410:7077 \
  --driver-memory 8192m \
  target/NBAStats-distributed-1.0.jar

  # --master local[*]
  # --master spark://poweredge410:7077
  # --conf spark.default.parallelism=100
