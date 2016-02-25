library("matrixStats")
library("ggplot2")
library("reshape")

###########################################
# FROM DATABASE
###########################################
con = dbConnect(RSQLite::SQLite(), dbname="C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\scripts\\results-adaptive.db")
dataset = "absorber"
sql_command = paste("select avg(test_error) from results where dataset LIKE '",dataset,"%' GROUP BY batch", sep="")

results_avg = dbGetQuery(con, sql_command)

sql_command = paste("select min(test_error) from results where dataset LIKE '",dataset,"%' GROUP BY batch", sep="")
results_min = dbGetQuery(con, sql_command)


###########################################
# FROM FILES
###########################################

datasets = c("absorber", "debutanizer", "catalyst", "IndustrialDrier", "oxeno", "sulfur", "ThermalOxidizerConv")

for (d in 1:length(datasets)) {
  
  print(datasets[d])
  dataset = datasets[d]

  test_error_adaptive = matrix(0, 25, 10)
  for (i in 0:24) {
  	for (j in 0:9) {
  		filename = paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\experiments\\adaptive\\", dataset,"\\batch",j,"\\predictions.", i, ".csv", sep="")
  		#print(paste("Reading", filename))
  		if (file.exists(filename)) {
  			predictions = read.csv(filename)
  			batch = as.data.frame(predictions)
  			test_error_adaptive[i+1, j+1] = 100*sum(batch$error == "+", na.rm=TRUE)/nrow(batch)
  		}
  		else {
  			print(paste("File", filename, "does not exists"))
  			test_error_adaptive[i+1,j+1] = NaN
  		}
  	}
  }
  
  test_error_retrain = matrix(0, 25, 10)
  for (i in 0:24) {
  	for (j in 0:9) {
  		filename = paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\experiments\\static\\", dataset,"\\batch",j,"\\predictions.", i, ".csv", sep="")
  		#print(paste("Reading", filename))
  		if (file.exists(filename)) {
  			predictions = read.csv(filename)
  			batch = as.data.frame(predictions)
  			test_error_retrain[i+1, j+1] = 100*sum(batch$error == "+", na.rm=TRUE)/nrow(batch)
  		}
  		else {
  			print(paste("File", filename, "does not exists"))
  			test_error_retrain[i+1,j+1] = NaN
  		}
  	}
  }
  
  test_error_static = matrix(0, 25, 10)
  for (i in 0:24) {
  	filename = paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\experiments\\non-adaptive\\", dataset,"\\predictions.", i, ".csv", sep="")
  	if (file.exists(filename)) {
  		predictions = read.csv(filename)
  		batches  = split(predictions, ceiling(seq_along(predictions$inst.)/(nrow(predictions)/10)))
  		for (j in 1:10) {
  			batch = as.data.frame(batches[j])
  			test_error_static[i+1, j] = 100*sum(batch[4] == "+", na.rm=TRUE)/nrow(batch[4])
  		}
  	}
  	else {
  		print(paste("File", filename, "does not exists"))
  		test_error_static[i+1,] = NaN
  	}
  }
  
  m1 = as.data.frame(test_error_adaptive)
  m1$id = "Adapt"
  m2 = as.data.frame(test_error_retrain)
  m2$id = "Retrain"
  m3 = as.data.frame(test_error_static)
  m3$id = "Static"
  df = rbind(m1, m2, m3)
  
  
  g0 <- ggplot(data=melt(df, "id"), aes(as.factor(variable), value, colour=id, group=id))
  
  my.fun<-function(x){data.frame(ymin=min(x),ymax=max(x),y=mean(x))}
  g_box = g0 + 
    stat_summary(fun.data = mean_cl_boot, geom = "errorbar", size=0.5, alpha=0.5) + 
    stat_summary(fun.y = mean, geom = "line", aes(group=id, linetype=id), size=1.1) + 
    scale_linetype_manual(values=c("solid", "dashed", "dotted")) +
    theme_bw() + theme(panel.grid.major.x = element_blank(), panel.grid.major.y = element_blank(),
                       panel.grid.minor.x = element_blank(), panel.grid.major.y = element_blank()) +
    ylim(0,100) +
    xlab("Batch") + ylab("% class. error") + ggtitle(dataset)
  
  g_box
  ggsave(file=paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\plots-adaptive\\", dataset, ".png", sep=""))

}

# OLD PLOTS
#   png(paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\plots-adaptive\\", dataset, ".png", sep=""))
#   xrange = c(0,9)
#   yrange = c(0,100)
#   plot(xrange, yrange, type="n", xlab="Batch", ylab="Test error")
#   axis(1, at=seq(0,9))
#   title(dataset)
#   #colors = rainbow(3)
#   # colorblind-friendly palette
#   colors = c("#000000", "#E69F00", "#56B4E9", "#009E73", "#F0E442", "#0072B2", "#D55E00", "#CC79A7")
#   ltype = c(1,2,3)
#   symbols = c(0,1,2)
#   
#   lines(0:9, colMeans(test_error_adaptive, na.rm=TRUE), type="b", col=colors[1], lty=ltype[1], pch=symbols[1], lwd=2)
#   lines(0:9, colMins(test_error_adaptive, na.rm=TRUE), type="b", col=adjustcolor(colors[1],alpha.f=0.8), lty=ltype[1], pch=symbols[1], lwd=1)
#   
#   lines(0:9, colMeans(test_error_retrain, na.rm=TRUE), type="b", col=colors[2], lty=ltype[2], pch=symbols[2], lwd=2)
#   lines(0:9, colMins(test_error_retrain, na.rm=TRUE), type="b", col=adjustcolor(colors[2],alpha.f=0.8), lty=ltype[2], pch=symbols[2], lwd=1)
#   
#   lines(0:9, colMeans(test_error_static, na.rm=TRUE), type="b", col=colors[3], lty=ltype[3], pch=symbols[3], lwd=2)
#   lines(0:9, colMins(test_error_static, na.rm=TRUE), type="b", col=adjustcolor(colors[3],alpha.f=0.8), lty=ltype[3], pch=symbols[3], lwd=1)
#   
#   legend('topleft','groups', c("Adapt","Retrain", "Static"), lty=ltype, col=colors, pch=symbols, ncol=3, bty ="n", lwd=2)
#   dev.off()

