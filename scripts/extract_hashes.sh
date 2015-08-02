#!/bin/bash
#datasets=(absorber catalyst_activation debutanizer IndustrialDrier oxeno-hourly sulfur ThermalOxidizerConv)
datasets=(abalone amazon car cifar10 cifar10small convex dexter dorothea germancredit gisette kddcup09appetency krvskp madelon mnist mnistrotationbackimagenew secom semeion shuttle waveform winequalitywhite yeast)
for d in "${datasets[@]}"
do
	mkdir -p $AUTOWEKA_PATH/experiments/$d.RAND.CV-$d/out/hashes/
	for s in {0..24}
	do
		cd $AUTOWEKA_PATH/experiments/$d.RAND.CV-$d/out/logs/ && grep "hash" $s.log > ../hashes/$s.log
	done
done
