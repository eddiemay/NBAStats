DATE=$1
java -cp target/NBAStats-1.0/WEB-INF/lib/NBAStats-1.0.jar com.digitald4.nbastats.compute.FanDuelIO output --date $DATE
sh runSpark.sh $DATE
java -cp target/NBAStats-1.0/WEB-INF/lib/NBAStats-1.0.jar com.digitald4.nbastats.compute.FanDuelIO insert --date $DATE
