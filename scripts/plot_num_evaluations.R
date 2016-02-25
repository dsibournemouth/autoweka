library("matrixStats")
library("RSQLite")

con = dbConnect(RSQLite::SQLite(), dbname="C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\scripts\\results-adaptive.db")

datasets = c("absorber", "debutanizer", "catalyst", "IndustrialDrier", "oxeno", "sulfur", "ThermalOxidizerConv")

# colorblind-friendly palette
colors = c("#000000", "#E69F00", "#56B4E9", "#009E73", "#F0E442", "#0072B2", "#D55E00", "#CC79A7")
ltype = c(1,2,3)
symbols = c(0,1,2)

for (d in 1:length(datasets)) {
  
  print(datasets[d])
  dataset = datasets[d]
  
  sql_command = paste("select batch, avg(num_evaluations), max(num_evaluations), min(num_evaluations) from results where dataset LIKE '",dataset,"%' GROUP BY batch", sep="")
  
  results_avg = dbGetQuery(con, sql_command)
  
  
  png(paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\plots-adaptive\\evaluations.", dataset, ".png", sep=""))
  xrange = c(0,9)
  yrange = c(0, max(results_avg[,3]))
  plot(xrange, yrange, xlab="Batch", ylab="# Evaluations", type="n")
  lines(0:9, results_avg[,2], type="b", col=colors[1], lty=ltype[1], pch=symbols[1], lwd=2)
  arrows(0:9, results_avg[,3], 0:9, results_avg[,4], length=0.05, angle=90, code=3)
  axis(1, at=seq(0,9))
  title(dataset)
  
  dev.off()
}
dbDisconnect(con)
