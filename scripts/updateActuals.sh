DATE=$1
java -cp target/NBAStats-1.0/WEB-INF/lib/NBAStats-1.0.jar com.digitald4.nbastats.compute.FanDuelIO updateActuals --date $DATE
sh runSpark.sh $DATE
java -cp target/NBAStats-1.0/WEB-INF/lib/NBAStats-1.0.jar com.digitald4.nbastats.compute.FanDuelIO insertActuals --date $DATE
