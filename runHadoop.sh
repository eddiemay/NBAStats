mvn -f hadoop.xml package
rm -r target/output/fanduel/2018-01-26
~/Downloads/hadoop-2.9.0/bin/hadoop jar target/NBAStats-hadoop-1.0.jar com.digitald4.nbastats.hadoop.ProcessFanDuel
