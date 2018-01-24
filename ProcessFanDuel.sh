java -cp target/NBAStats-1.0/WEB-INF/lib/NBAStats-1.0.jar com.digitald4.nbastats.compute.FanDuelIO output
sh runHadoop.sh
java -cp target/NBAStats-1.0/WEB-INF/lib/NBAStats-1.0.jar com.digitald4.nbastats.compute.FanDuelIO insert
